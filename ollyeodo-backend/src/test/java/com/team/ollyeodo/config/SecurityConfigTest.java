package com.team.ollyeodo.config;

import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.crypto.RSASSASigner;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.interfaces.RSAPublicKey;
import java.time.Instant;
import java.util.Date;
import java.util.List;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.BadJwtException;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** #71 토큰 검사 규칙: 발급자(Supabase Auth)·대상(authenticated)·만료·사용자 ID */
class SecurityConfigTest {
    private static final String ISSUER = "https://example.supabase.co/auth/v1";
    private static final String USER = "11111111-1111-1111-1111-111111111111";
    private static KeyPair keys;
    private static JwtDecoder decoder;

    @BeforeAll
    static void setUp() throws Exception {
        KeyPairGenerator generator = KeyPairGenerator.getInstance("RSA");
        generator.initialize(2048);
        keys = generator.generateKeyPair();
        NimbusJwtDecoder nimbus = NimbusJwtDecoder.withPublicKey((RSAPublicKey) keys.getPublic()).build();
        nimbus.setJwtValidator(SecurityConfig.validator(ISSUER));
        decoder = nimbus;
    }

    private static String token(String issuer, List<String> audience, String subject, Instant expires) throws Exception {
        var claims = new JWTClaimsSet.Builder().issuer(issuer).audience(audience).subject(subject)
                .issueTime(Date.from(expires.minusSeconds(7200))).expirationTime(Date.from(expires)).build();
        SignedJWT jwt = new SignedJWT(new JWSHeader(JWSAlgorithm.RS256), claims);
        jwt.sign(new RSASSASigner(keys.getPrivate()));
        return jwt.serialize();
    }

    private static Instant later() {
        return Instant.now().plusSeconds(3600);
    }

    @Test
    void acceptsSupabaseUserToken() throws Exception {
        assertThat(decoder.decode(token(ISSUER, List.of("authenticated"), USER, later())).getSubject())
                .isEqualTo(USER);
    }

    @Test
    void rejectsOtherIssuerAudienceSubjectOrExpired() {
        assertThatThrownBy(() -> decoder.decode(token("https://evil.example/auth/v1", List.of("authenticated"), USER, later())))
                .isInstanceOf(BadJwtException.class);
        assertThatThrownBy(() -> decoder.decode(token(ISSUER, List.of("anon"), USER, later())))
                .isInstanceOf(BadJwtException.class);
        assertThatThrownBy(() -> decoder.decode(token(ISSUER, List.of("authenticated"), "not-a-uuid", later())))
                .isInstanceOf(BadJwtException.class);
        assertThatThrownBy(() -> decoder.decode(token(ISSUER, List.of("authenticated"), USER,
                Instant.now().minusSeconds(3600))))
                .isInstanceOf(BadJwtException.class);
    }

    @Test
    void withoutSupabaseUrlEveryTokenIsRejected() {
        JwtDecoder none = new SecurityConfig().jwtDecoder("", "");
        assertThatThrownBy(() -> none.decode("anything")).isInstanceOf(RuntimeException.class);
    }
}
