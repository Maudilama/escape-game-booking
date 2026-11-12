package com.demo.escape_game_booking.auth;

import com.demo.escape_game_booking.user.Role;
import org.springframework.security.oauth2.jwt.Jwt;

import java.util.List;

public record CurrentUser(Long id, boolean admin) {

    public static CurrentUser from(Jwt jwt) {
        List<String> roles = jwt.getClaimAsStringList("roles");
        boolean admin = roles != null && roles.contains(Role.ADMIN.name());
        return new CurrentUser(Long.valueOf(jwt.getSubject()), admin);
    }
}