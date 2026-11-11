package me.fertiz.syncboard.room;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;

@ConfigurationProperties("syncboard")
public record RoomProperties(int maxRooms, Duration roomIdleTimeout) {
}
