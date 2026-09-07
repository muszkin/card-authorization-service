package pl.fairydeck.authorization.adapter.in.rest;

import jakarta.validation.Valid;
import java.net.URI;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import pl.fairydeck.authorization.application.AuthorizationLifecycle;
import pl.fairydeck.authorization.application.AuthorizePurchase;
import pl.fairydeck.authorization.domain.authorization.Authorization;

@RestController
@RequestMapping(AuthorizationController.PATH)
class AuthorizationController {

    static final String PATH = "/v1/authorizations";
    static final String IDEMPOTENCY_KEY = "Idempotency-Key";

    private final AuthorizePurchase authorizePurchase;
    private final AuthorizationLifecycle lifecycle;

    AuthorizationController(AuthorizePurchase authorizePurchase, AuthorizationLifecycle lifecycle) {
        this.authorizePurchase = authorizePurchase;
        this.lifecycle = lifecycle;
    }

    @PostMapping
    ResponseEntity<AuthorizationResponse> authorize(
            @RequestHeader(IDEMPOTENCY_KEY) String idempotencyKey,
            @Valid @RequestBody AuthorizationRequest request) {
        Authorization authorization = authorizePurchase.authorize(request.toPurchase(idempotencyKey));
        return ResponseEntity.created(URI.create(PATH + "/" + authorization.id()))
                .body(AuthorizationResponse.from(authorization));
    }

    @PostMapping("/{id}/capture")
    AuthorizationResponse capture(@PathVariable UUID id) {
        return AuthorizationResponse.from(lifecycle.capture(id));
    }

    @PostMapping("/{id}/reverse")
    AuthorizationResponse reverse(@PathVariable UUID id) {
        return AuthorizationResponse.from(lifecycle.reverse(id));
    }
}
