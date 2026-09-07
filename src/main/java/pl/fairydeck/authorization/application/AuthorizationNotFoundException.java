package pl.fairydeck.authorization.application;

import java.util.UUID;

public class AuthorizationNotFoundException extends RuntimeException {

    public AuthorizationNotFoundException(UUID authorizationId) {
        super("Authorization %s does not exist".formatted(authorizationId));
    }
}
