package br.com.zimbro.platform;

import java.security.SecureRandom;
import java.util.UUID;

/**
 * UUID v7: aleatorio como o v4, mas com os 48 bits mais altos sendo o relogio em
 * milissegundos. Isso o deixa ordenado por tempo, o que evita a fragmentacao de
 * indice do v4 — e o celular pode gerar o id offline antes de sincronizar.
 *
 * O Postgres 18 tem uuidv7() nativo e ele e o default das tabelas. Esta classe
 * existe para o caso comum: o id nasce em Java, antes do insert.
 */
public final class Ids {

    private static final SecureRandom ALEATORIO = new SecureRandom();

    private Ids() {
    }

    public static UUID v7() {
        long ms = System.currentTimeMillis();
        byte[] bytes = new byte[2];
        ALEATORIO.nextBytes(bytes);

        long alto = (ms & 0xFFFF_FFFF_FFFFL) << 16          // 48 bits de relogio
                | 0x7000L                                    // versao 7
                | ((bytes[0] & 0x0FL) << 8) | (bytes[1] & 0xFFL);

        long baixo = (ALEATORIO.nextLong() & 0x3FFF_FFFF_FFFF_FFFFL)
                | 0x8000_0000_0000_0000L;                    // variante RFC 4122

        return new UUID(alto, baixo);
    }
}
