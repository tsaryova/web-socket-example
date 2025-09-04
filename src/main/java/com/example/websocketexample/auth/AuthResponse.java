package com.example.websocketexample.auth;

import lombok.AllArgsConstructor;
import lombok.Getter;

@AllArgsConstructor
public class AuthResponse {

    @Getter
    private String token;
}
