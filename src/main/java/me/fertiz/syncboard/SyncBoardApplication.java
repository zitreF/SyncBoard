package me.fertiz.syncboard;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;
import org.springframework.context.annotation.Bean;
import org.springframework.scheduling.annotation.EnableScheduling;

import java.time.InstantSource;

@SpringBootApplication
@ConfigurationPropertiesScan
@EnableScheduling
public class SyncBoardApplication {

    public static void main(String[] args) {
        SpringApplication.run(SyncBoardApplication.class, args);
    }

    @Bean
    public InstantSource clock() {
        return InstantSource.system();
    }
}
