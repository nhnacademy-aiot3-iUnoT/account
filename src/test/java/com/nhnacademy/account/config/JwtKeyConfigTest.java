package com.nhnacademy.account.config;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.security.KeyPair;
import java.security.interfaces.RSAPublicKey;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class JwtKeyConfigTest {

    @TempDir
    private Path tempDir;

    private final JwtKeyConfig jwtKeyConfig = new JwtKeyConfig();

    @Test
    void generatesAndSavesKeyPairWhenPrivateKeyDoesNotExist() throws Exception {
        Path privateKeyPath = tempDir.resolve("keys/jwt-private.pem");
        JwtProperties properties = properties(privateKeyPath);

        KeyPair keyPair = jwtKeyConfig.keyPair(properties);

        assertThat(Files.readString(privateKeyPath))
                .startsWith("-----BEGIN PRIVATE KEY-----")
                .endsWith("-----END PRIVATE KEY-----\n");
        assertThat(((RSAPublicKey) keyPair.getPublic()).getModulus().bitLength())
                .isGreaterThanOrEqualTo(2048);
    }

    @Test
    void loadsSameKeyPairWhenPrivateKeyExists() {
        Path privateKeyPath = tempDir.resolve("jwt-private.pem");
        JwtProperties properties = properties(privateKeyPath);
        KeyPair generatedKeyPair = jwtKeyConfig.keyPair(properties);

        KeyPair loadedKeyPair = jwtKeyConfig.keyPair(properties);

        assertThat(loadedKeyPair.getPrivate().getEncoded())
                .isEqualTo(generatedKeyPair.getPrivate().getEncoded());
        assertThat(loadedKeyPair.getPublic().getEncoded())
                .isEqualTo(generatedKeyPair.getPublic().getEncoded());
    }

    @Test
    void rejectsInvalidPrivateKeyFile() throws Exception {
        Path privateKeyPath = tempDir.resolve("invalid.pem");
        Files.writeString(privateKeyPath, "not-a-private-key");
        JwtProperties properties = properties(privateKeyPath);

        assertThatThrownBy(() -> jwtKeyConfig.keyPair(properties))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Failed to initialize JWT key pair");
    }

    @Test
    void requiresPrivateKeyPath() {
        assertThatThrownBy(() -> jwtKeyConfig.keyPair(new JwtProperties()))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("security.jwt.private-key-path must be configured");
    }

    private JwtProperties properties(Path privateKeyPath) {
        JwtProperties properties = new JwtProperties();
        properties.setPrivateKeyPath(privateKeyPath.toFile());
        return properties;
    }
}
