package com.nhnacademy.account.config;

import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.KeyUse;
import com.nimbusds.jose.jwk.RSAKey;
import com.nimbusds.jose.jwk.source.ImmutableJWKSet;
import com.nimbusds.jose.jwk.source.JWKSource;
import com.nimbusds.jose.proc.SecurityContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.nio.file.attribute.PosixFilePermission;
import java.security.GeneralSecurityException;
import java.security.KeyFactory;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.PrivateKey;
import java.security.interfaces.RSAPrivateCrtKey;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.RSAPublicKeySpec;
import java.util.Base64;
import java.util.EnumSet;
import java.util.Set;

import static java.nio.file.attribute.PosixFilePermission.OWNER_READ;
import static java.nio.file.attribute.PosixFilePermission.OWNER_WRITE;

@Configuration
public class JwtKeyConfig {
    private static final int RSA_KEY_SIZE = 2048;
    private static final String PRIVATE_KEY_BEGIN = "-----BEGIN PRIVATE KEY-----";
    private static final String PRIVATE_KEY_END = "-----END PRIVATE KEY-----";

    @Bean
    public KeyPair keyPair(JwtProperties properties) {
        if (properties.getPrivateKeyPath() == null) {
            throw new IllegalStateException(
                    "security.jwt.private-key-path must be configured"
            );
        }

        Path privateKeyPath = properties.getPrivateKeyPath()
                .toPath()
                .toAbsolutePath()
                .normalize();

        try {
            if (Files.exists(privateKeyPath)) {
                return loadKeyPair(privateKeyPath);
            }

            KeyPair keyPair = generateKeyPair();
            try {
                savePrivateKey(privateKeyPath, keyPair.getPrivate());
                return keyPair;
            } catch (FileAlreadyExistsException ignored) {
                return loadKeyPair(privateKeyPath);
            }
        } catch (IOException | GeneralSecurityException e) {
            throw new IllegalStateException(
                    "Failed to initialize JWT key pair from " + privateKeyPath,
                    e
            );
        }
    }

    private KeyPair generateKeyPair() throws GeneralSecurityException {
        KeyPairGenerator keyPairGenerator = KeyPairGenerator.getInstance("RSA");
        keyPairGenerator.initialize(RSA_KEY_SIZE);

        return keyPairGenerator.generateKeyPair();
    }

    private void savePrivateKey(Path path, PrivateKey privateKey) throws IOException {
        Path parent = path.getParent();
        if (parent != null) {
            Files.createDirectories(parent);
        }

        String encodedKey = Base64.getMimeEncoder(64, "\n".getBytes(StandardCharsets.US_ASCII))
                .encodeToString(privateKey.getEncoded());
        String pem = PRIVATE_KEY_BEGIN + "\n"
                + encodedKey + "\n"
                + PRIVATE_KEY_END + "\n";

        Files.writeString(
                path,
                pem,
                StandardCharsets.US_ASCII,
                StandardOpenOption.CREATE_NEW,
                StandardOpenOption.WRITE
        );
        restrictToOwner(path);
    }

    private void restrictToOwner(Path path) throws IOException {
        try {
            Set<PosixFilePermission> permissions = EnumSet.of(OWNER_READ, OWNER_WRITE);
            Files.setPosixFilePermissions(path, permissions);
        } catch (UnsupportedOperationException ignored) {
            // POSIX permissions are unavailable on this file system.
        }
    }

    private KeyPair loadKeyPair(Path path) throws IOException, GeneralSecurityException {
        String pem = Files.readString(path, StandardCharsets.US_ASCII);
        if (!pem.contains(PRIVATE_KEY_BEGIN) || !pem.contains(PRIVATE_KEY_END)) {
            throw new GeneralSecurityException("Invalid PKCS#8 PEM private key");
        }

        String encodedKey = pem
                .replace(PRIVATE_KEY_BEGIN, "")
                .replace(PRIVATE_KEY_END, "")
                .replaceAll("\\s", "");

        byte[] keyBytes;
        try {
            keyBytes = Base64.getDecoder().decode(encodedKey);
        } catch (IllegalArgumentException e) {
            throw new GeneralSecurityException("Invalid PKCS#8 PEM private key", e);
        }

        KeyFactory keyFactory = KeyFactory.getInstance("RSA");
        PrivateKey privateKey = keyFactory.generatePrivate(
                new PKCS8EncodedKeySpec(keyBytes)
        );
        if (!(privateKey instanceof RSAPrivateCrtKey rsaPrivateKey)) {
            throw new GeneralSecurityException(
                    "JWT private key must contain RSA CRT parameters"
            );
        }

        RSAPublicKey publicKey = (RSAPublicKey) keyFactory.generatePublic(
                new RSAPublicKeySpec(
                        rsaPrivateKey.getModulus(),
                        rsaPrivateKey.getPublicExponent()
                )
        );
        return new KeyPair(publicKey, privateKey);
    }

    @Bean
    public RSAKey signingJwk(KeyPair keyPair, JwtProperties properties) {
        RSAPublicKey publicKey = (RSAPublicKey) keyPair.getPublic();
        RSAPrivateKey privateKey = (RSAPrivateKey) keyPair.getPrivate();

        return new RSAKey.Builder(publicKey)
                .privateKey(privateKey)
                .keyUse(KeyUse.SIGNATURE)
                .algorithm(JWSAlgorithm.RS256)
                .keyID(properties.getKeyId())
                .build();
    }

    @Bean
    public JWKSet jwkSet(RSAKey signingJwk) {
        return new JWKSet(signingJwk);
    }

    @Bean
    public JWKSource<SecurityContext> jwkSource(JWKSet jwkSet) {
        return new ImmutableJWKSet<>(jwkSet);
    }

    @Bean
    public JwtEncoder jwtEncoder(JWKSource<SecurityContext> jwkSource) {
        return new NimbusJwtEncoder(jwkSource);
    }
}
