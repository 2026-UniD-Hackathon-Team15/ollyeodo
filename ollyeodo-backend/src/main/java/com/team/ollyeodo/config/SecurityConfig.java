package com.team.ollyeodo.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.nimbusds.jwt.JWTParser;
import java.nio.charset.StandardCharsets;
import java.util.List;
import javax.crypto.spec.SecretKeySpec;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ProblemDetail;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.oauth2.core.DelegatingOAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.OAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2TokenValidatorResult;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jose.jws.SignatureAlgorithm;
import org.springframework.security.oauth2.jwt.BadJwtException;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtValidators;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.SecurityFilterChain;

/**
 * 로그인 (#71). 회원가입·로그인·비밀번호는 Supabase Auth가 맡고, 백엔드는 Supabase가 준
 * access token(JWT)의 서명·발급자(iss)·대상(aud)·만료만 확인한다. 사용자 ID는 토큰의 sub.
 * - 공개: /api/health, /api/rules
 * - 로그인 필요: /api/checks/** (토큰이 없거나 잘못되면 401 + 한국어 안내)
 */
@Configuration
public class SecurityConfig {
    public static final String UNAUTHORIZED_DETAIL = "로그인이 필요해요. 다시 로그인해 주세요.";
    static final String AUDIENCE = "authenticated";

    @Bean
    public SecurityFilterChain apiSecurity(HttpSecurity http, JwtDecoder decoder, ObjectMapper json) throws Exception {
        AuthenticationEntryPoint unauthorized = (request, response, exception) -> {
            ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.UNAUTHORIZED, UNAUTHORIZED_DETAIL);
            problem.setInstance(java.net.URI.create(request.getRequestURI()));
            response.setStatus(HttpStatus.UNAUTHORIZED.value());
            response.setHeader("WWW-Authenticate", "Bearer");
            response.setContentType(MediaType.APPLICATION_PROBLEM_JSON_VALUE);
            response.setCharacterEncoding(StandardCharsets.UTF_8.name());
            json.writeValue(response.getOutputStream(), problem);
        };
        http.csrf(AbstractHttpConfigurer::disable)
                .cors(Customizer.withDefaults())   // WebConfig의 CORS 설정을 그대로 쓴다
                .httpBasic(AbstractHttpConfigurer::disable)
                .formLogin(AbstractHttpConfigurer::disable)
                .logout(AbstractHttpConfigurer::disable)
                .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
                        .requestMatchers("/api/checks", "/api/checks/**").authenticated()
                        .anyRequest().permitAll())
                .oauth2ResourceServer(o -> o.jwt(j -> j.decoder(decoder)).authenticationEntryPoint(unauthorized))
                .exceptionHandling(e -> e.authenticationEntryPoint(unauthorized));
        return http.build();
    }

    /**
     * Supabase 서명 방식에 맞춰 검증한다.
     * - ES256/RS256(새 서명 키): ${SUPABASE_URL}/auth/v1/.well-known/jwks.json 공개키
     * - HS256(예전 방식): SUPABASE_JWT_SECRET이 있을 때만
     * SUPABASE_URL이 없으면(로컬 H2·테스트) 모든 토큰을 거절한다.
     */
    @Bean
    public JwtDecoder jwtDecoder(@Value("${supabase.url:}") String supabaseUrl,
                                 @Value("${supabase.jwt-secret:}") String jwtSecret) {
        if (supabaseUrl == null || supabaseUrl.isBlank()) {
            return token -> {
                throw new BadJwtException("인증 서버(SUPABASE_URL)가 설정되지 않았습니다.");
            };
        }
        String issuer = supabaseUrl.strip().replaceAll("/+$", "") + "/auth/v1";
        OAuth2TokenValidator<Jwt> validator = validator(issuer);

        NimbusJwtDecoder jwks = NimbusJwtDecoder.withJwkSetUri(issuer + "/.well-known/jwks.json")
                .jwsAlgorithms(a -> {
                    a.add(SignatureAlgorithm.ES256);
                    a.add(SignatureAlgorithm.RS256);
                })
                .build();
        jwks.setJwtValidator(validator);

        NimbusJwtDecoder hmac = null;
        if (jwtSecret != null && !jwtSecret.isBlank()) {
            hmac = NimbusJwtDecoder.withSecretKey(new SecretKeySpec(
                            jwtSecret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"))
                    .macAlgorithm(MacAlgorithm.HS256).build();
            hmac.setJwtValidator(validator);
        }
        NimbusJwtDecoder secretDecoder = hmac;
        return token -> {
            String alg;
            try {
                alg = JWTParser.parse(token).getHeader().getAlgorithm().getName();
            } catch (java.text.ParseException | RuntimeException e) {
                throw new BadJwtException("토큰 형식이 올바르지 않습니다.", e);
            }
            if ("HS256".equals(alg)) {
                if (secretDecoder == null) throw new BadJwtException("HS256 토큰은 받지 않습니다.");
                return secretDecoder.decode(token);
            }
            return jwks.decode(token);
        };
    }

    /** 만료·발급자(Supabase Auth)·대상(authenticated 사용자) 확인 */
    static OAuth2TokenValidator<Jwt> validator(String issuer) {
        OAuth2TokenValidator<Jwt> audience = jwt -> {
            List<String> aud = jwt.getAudience();
            return aud != null && aud.contains(AUDIENCE)
                    ? OAuth2TokenValidatorResult.success()
                    : OAuth2TokenValidatorResult.failure(new OAuth2Error("invalid_token",
                    "로그인한 사용자 토큰이 아닙니다.", null));
        };
        OAuth2TokenValidator<Jwt> subject = jwt -> jwt.getSubject() != null && isUuid(jwt.getSubject())
                ? OAuth2TokenValidatorResult.success()
                : OAuth2TokenValidatorResult.failure(new OAuth2Error("invalid_token", "사용자 ID가 없습니다.", null));
        return new DelegatingOAuth2TokenValidator<>(JwtValidators.createDefaultWithIssuer(issuer), audience, subject);
    }

    private static boolean isUuid(String value) {
        try {
            java.util.UUID.fromString(value);
            return true;
        } catch (IllegalArgumentException e) {
            return false;
        }
    }
}
