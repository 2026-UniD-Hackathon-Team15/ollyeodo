package com.team.ollyeodo.ai;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.converter.BeanOutputConverter;
import org.springframework.ai.openai.OpenAiChatOptions;
import org.springframework.ai.openai.api.ResponseFormat;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.util.MimeTypeUtils;

import java.time.Duration;
import java.util.Objects;

/** OpenAI Chat Completions의 JSON Schema 구조화 출력 어댑터. */
public class OpenAiExtractionClient implements ExtractionClient {
    private final ChatClient chatClient;
    /** 이미지 분석 추론 강도 (low|medium|high). 비어 있으면 모델 기본값 */
    private final String imageReasoningEffort;

    public OpenAiExtractionClient(ChatClient chatClient) {
        this(chatClient, null);
    }

    public OpenAiExtractionClient(ChatClient chatClient, String imageReasoningEffort) {
        this.chatClient = Objects.requireNonNull(chatClient);
        this.imageReasoningEffort = imageReasoningEffort == null || imageReasoningEffort.isBlank()
                ? null : imageReasoningEffort.strip();
    }

    @Override
    public ExtractedAttrs extractImage(String prompt, ImageInput image, Duration timeout) {
        return extract(prompt, "첨부 이미지의 관찰값을 추출하세요.", image, timeout, ExtractedAttrs.class);
    }

    @Override
    public CaptionClaims extractCaption(String prompt, String caption, Duration timeout) {
        return extract(prompt, caption, null, timeout, CaptionClaims.class);
    }

    @Override
    public CaptionReview reviewCaption(String prompt, String input, Duration timeout) {
        return extract(prompt, input, null, timeout, CaptionReview.class);
    }

    private <T> T extract(String prompt, String text, ImageInput image, Duration timeout, Class<T> type) {
        var converter = new BeanOutputConverter<>(type);
        var format = ResponseFormat.builder().type(ResponseFormat.Type.JSON_SCHEMA)
                .jsonSchema(ResponseFormat.JsonSchema.builder().name(type.getSimpleName())
                        .strict(true).schema(nullableSchema(converter.getJsonSchema())).build()).build();
        // 사용자 입력은 별도 데이터 메시지로 전달한다.
        var options = OpenAiChatOptions.builder().responseFormat(format);
        if (image != null && imageReasoningEffort != null) options.reasoningEffort(imageReasoningEffort);
        var response = chatClient.prompt().options(options.build()).system(prompt).user(u -> {
            u.text(text);
            if (image != null) u.media(MimeTypeUtils.parseMimeType(image.mediaType()),
                    new ByteArrayResource(image.bytes()));
        }).call().content();
        if (response == null || response.isBlank()) throw new IllegalStateException("AI 추출 응답이 비어 있습니다.");
        return Objects.requireNonNull(converter.convert(response), "AI 추출 응답이 없습니다.");
    }

    /** 확인 불가(null)와 실제 값 없음([])을 구분하면서 strict schema를 사용한다. */
    static String nullableSchema(String schema) {
        try {
            var mapper = new ObjectMapper();
            JsonNode root = mapper.readTree(schema);
            makeNullable(root, false);
            return mapper.writeValueAsString(root);
        } catch (java.io.IOException e) {
            throw new IllegalStateException("추출 스키마 생성에 실패했습니다.", e);
        }
    }

    private static void makeNullable(JsonNode node, boolean nullable) {
        if (!(node instanceof ObjectNode object)) return;
        var properties = object.get("properties");
        if (properties != null) {
            object.put("additionalProperties", false);
            var required = object.putArray("required");
            properties.properties().forEach(entry -> {
                required.add(entry.getKey());
                makeNullable(entry.getValue(), true);
            });
        }
        if (object.has("items")) makeNullable(object.get("items"), false);
        if (object.has("$defs")) object.get("$defs").elements().forEachRemaining(n -> makeNullable(n, false));
        if (nullable && object.path("type").isTextual()) {
            String type = object.get("type").asText();
            object.putArray("type").add(type).add("null");
        }
    }
}
