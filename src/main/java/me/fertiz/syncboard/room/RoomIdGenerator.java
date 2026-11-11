package me.fertiz.syncboard.room;

import org.springframework.stereotype.Component;

import java.security.SecureRandom;

@Component
public class RoomIdGenerator {

    private static final String ALPHABET = "abcdefghijkmnpqrstuvwxyz23456789";
    private static final int LENGTH = 8;

    private final SecureRandom random = new SecureRandom();

    public String next() {
        StringBuilder id = new StringBuilder(LENGTH);
        for (int i = 0; i < LENGTH; i++) {
            id.append(ALPHABET.charAt(random.nextInt(ALPHABET.length())));
        }
        return id.toString();
    }
}
