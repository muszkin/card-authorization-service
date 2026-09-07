package pl.fairydeck.authorization.application.port.out;

/**
 * Raised by the store when a second authorization claims an idempotency key that is already taken. This is the
 * database's unique constraint speaking, translated into the language of the port.
 */
public class DuplicateIdempotencyKeyException extends RuntimeException {

    public DuplicateIdempotencyKeyException(String idempotencyKey) {
        super("An authorization with idempotency key %s already exists".formatted(idempotencyKey));
    }
}
