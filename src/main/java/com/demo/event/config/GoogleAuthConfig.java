package com.demo.event.config;

import com.google.api.client.googleapis.auth.oauth2.GoogleIdTokenVerifier;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.gson.GsonFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.Collections;

/**
 * Cung cap bean GoogleIdTokenVerifier de verify idToken tu Google Sign-In.
 * GOOGLE_CLIENT_ID lay tu console.cloud.google.com, dien vao .env.
 */
@Configuration
public class GoogleAuthConfig {

    @Value("${GOOGLE_CLIENT_ID:}")
    private String googleClientId;

    @Bean
    public GoogleIdTokenVerifier googleIdTokenVerifier() {
        return new GoogleIdTokenVerifier.Builder(new NetHttpTransport(), GsonFactory.getDefaultInstance())
            .setAudience(Collections.singletonList(googleClientId))
            .build();
    }
}
