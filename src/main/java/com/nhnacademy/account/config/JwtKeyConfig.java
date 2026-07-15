package com.nhnacademy.account.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.security.*;

@Configuration
public class JwtKeyConfig {

    @Bean
    public KeyPair keyPair() throws NoSuchAlgorithmException {
        KeyPairGenerator keyPairGenerator = KeyPairGenerator.getInstance("RSA");
        keyPairGenerator.initialize(2048);

        return keyPairGenerator.generateKeyPair();
    }

    @Bean
    public PrivateKey privateKey() throws NoSuchAlgorithmException {
        return keyPair().getPrivate();
    }

    @Bean
    public PublicKey publicKey() throws NoSuchAlgorithmException {
        return keyPair().getPublic();
    }
}
