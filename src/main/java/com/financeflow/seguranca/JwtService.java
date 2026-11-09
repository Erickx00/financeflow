package com.financeflow.seguranca;

import com.financeflow.configuracao.PropriedadesJwt;
import com.financeflow.usuario.Papel;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.Instant;
import java.util.UUID;

@Service
public class JwtService {

    private final JwtEncoder encoder;
    private final PropriedadesJwt properties;
    private final Clock clock;

    public JwtService(JwtEncoder encoder, PropriedadesJwt properties, Clock clock) {
        this.encoder = encoder;
        this.properties = properties;
        this.clock = clock;
    }

    public String issueToken(UUID usuarioId, String email, Papel papel) {
        Instant issuedAt = clock.instant();
        JwtClaimsSet claims = JwtClaimsSet.builder()
                .subject(usuarioId.toString())
                .claim("email", email)
                .claim("papel", papel.name())
                .issuedAt(issuedAt)
                .expiresAt(issuedAt.plus(properties.expiration()))
                .build();
        JwsHeader header = JwsHeader.with(MacAlgorithm.HS256).build();
        return encoder.encode(JwtEncoderParameters.from(header, claims)).getTokenValue();
    }

    public long expiresInSeconds() {
        return properties.expiration().toSeconds();
    }
}
