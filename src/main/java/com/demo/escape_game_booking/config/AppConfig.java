package com.demo.escape_game_booking.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Clock;
import java.time.ZoneId;

@Configuration
@EnableConfigurationProperties({
        BookingProperties.class,
        JwtProperties.class,
        AdminProperties.class
})
public class AppConfig {

    @Bean
    public Clock clock() {
        return Clock.system(ZoneId.of("Europe/Paris"));
    }
}