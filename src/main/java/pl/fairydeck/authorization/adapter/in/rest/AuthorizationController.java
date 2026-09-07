package pl.fairydeck.authorization.adapter.in.rest;

import jakarta.validation.Valid;
import java.net.URI;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import pl.fairydeck.authorization.application.AuthorizePurchase;
import pl.fairydeck.authorization.domain.authorization.Authorization;

@RestController
@RequestMapping(AuthorizationController.PATH)
class AuthorizationController {

    static final String PATH = "/v1/authorizations";
    static final String IDEMPOTENCY_KEY = "Idempotency-Key";

    private final AuthorizePurchase authorizePurchase;

    AuthorizationController(AuthorizePurchase authorizePurchase) {
        this.authorizePurchase = authorizePurchase;
    }

    @PostMapping
    ResponseEntity<AuthorizationResponse> authorize(
            @RequestHeader(IDEMPOTENCY_KEY) String idempotencyKey,
            @Valid @RequestBody AuthorizationRequest request) {
        Authorization authorization = authorizePurchase.authorize(request.toPurchase(idempotencyKey));
        return ResponseEntity.created(URI.create(PATH + "/" + authorization.id()))
                .body(AuthorizationResponse.from(authorization));
    }
}
