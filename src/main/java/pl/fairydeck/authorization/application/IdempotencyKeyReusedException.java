package pl.fairydeck.authorization.application;

public class IdempotencyKeyReusedException extends RuntimeException {

    public IdempotencyKeyReusedException(String idempotencyKey) {
        super("Idempotency key %s was already used for a different purchase".formatted(idempotencyKey));
    }
}
