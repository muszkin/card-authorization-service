package pl.fairydeck.authorization.adapter.in.rest;

import jakarta.validation.Valid;
import java.net.URI;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.jspecify.annotations.Nullable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import pl.fairydeck.authorization.application.CardQueries;
import pl.fairydeck.authorization.application.IssueCard;
import pl.fairydeck.authorization.application.port.out.TransactionFilter;
import pl.fairydeck.authorization.domain.authorization.AuthorizationStatus;
import pl.fairydeck.authorization.domain.card.Card;

@RestController
@RequestMapping(CardController.PATH)
class CardController {

    static final String PATH = "/v1/cards";

    private final IssueCard issueCard;
    private final CardQueries queries;

    CardController(IssueCard issueCard, CardQueries queries) {
        this.issueCard = issueCard;
        this.queries = queries;
    }

    @PostMapping
    ResponseEntity<CardResponse> issue(@Valid @RequestBody IssueCardRequest request) {
        Card card = issueCard.issue(request.cardholderId(), request.limit());
        return ResponseEntity.created(URI.create(PATH + "/" + card.id())).body(CardResponse.from(card));
    }

    @GetMapping("/{id}/balance")
    BalanceResponse balance(@PathVariable UUID id) {
        return BalanceResponse.of(id, queries.balance(id));
    }

    @GetMapping("/{id}/transactions")
    List<AuthorizationResponse> transactions(
            @PathVariable UUID id,
            @RequestParam(required = false) @Nullable Instant from,
            @RequestParam(required = false) @Nullable Instant to,
            @RequestParam(required = false) @Nullable AuthorizationStatus status) {
        return queries.transactions(id, new TransactionFilter(from, to, status)).stream()
                .map(AuthorizationResponse::from)
                .toList();
    }
}
