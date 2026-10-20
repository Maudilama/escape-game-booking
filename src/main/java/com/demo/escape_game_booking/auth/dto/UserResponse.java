package com.demo.escape_game_booking.auth.dto;

import com.demo.escape_game_booking.user.Role;
import com.demo.escape_game_booking.user.User;

public record UserResponse(Long id, String email, String firstName, String lastName, Role role) {

    public static UserResponse from(User user) {
        return new UserResponse(user.getId(), user.getEmail(),
                user.getFirstName(), user.getLastName(), user.getRole());
    }
}