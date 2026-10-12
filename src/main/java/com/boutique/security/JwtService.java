package com.boutique.security;

import java.time.Instant;
import java.util.List;

import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.stereotype.Service;

import com.boutique.customer.Customer;

/** Génération des jetons JWT signés (HS256). La validation est faite par le JwtDecoder de Spring Security. */
@Service
public class JwtService {

    public static final String CLAIM_CUSTOMER_ID = "customerId";
    public static final String CLAIM_ROLES = "roles";

    private final JwtEncoder encoder;
    private final JwtProperties properties;

    public JwtService(JwtEncoder encoder, JwtProperties properties) {
        this.encoder = encoder;
        this.properties = properties;
    }

    public String generateToken(Customer customer) {
        Instant now = Instant.now();
        JwtClaimsSet claims = JwtClaimsSet.builder()
                .issuer(properties.issuer())
                .issuedAt(now)
                .expiresAt(now.plus(properties.expiration()))
                .subject(customer.getEmail())
                .claim(CLAIM_CUSTOMER_ID, customer.getId())
                .claim(CLAIM_ROLES, List.of(customer.getRole().name()))
                .build();
        JwsHeader header = JwsHeader.with(MacAlgorithm.HS256).build();
        return encoder.encode(JwtEncoderParameters.from(header, claims)).getTokenValue();
    }

    public long expirationInSeconds() {
        return properties.expiration().toSeconds();
    }
}
