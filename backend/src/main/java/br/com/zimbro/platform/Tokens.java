package br.com.zimbro.platform;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.HexFormat;

/** Gera o token que o cliente guarda e o hash que o banco guarda. Nunca os dois juntos. */
public final class Tokens {

    private static final SecureRandom ALEATORIO = new SecureRandom();
    private static final Base64.Encoder BASE64 = Base64.getUrlEncoder().withoutPadding();

    private Tokens() {
    }

    public static String gerar() {
        byte[] bytes = new byte[32];
        ALEATORIO.nextBytes(bytes);
        return BASE64.encodeToString(bytes);
    }

    public static String hash(String token) {
        try {
            MessageDigest sha256 = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(sha256.digest(token.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 deveria existir em toda JVM", e);
        }
    }
}
