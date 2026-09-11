package pl.fairydeck.authorization.adapter.in.rest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

import java.time.Instant;
import java.util.Currency;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestConstructor;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.assertj.MockMvcTester;
import org.springframework.test.web.servlet.assertj.MvcTestResult;
import pl.fairydeck.authorization.application.CardNotFoundException;
import pl.fairydeck.authorization.application.CardQueries;
import pl.fairydeck.authorization.application.IssueCard;
import pl.fairydeck.authorization.application.port.out.TransactionFilter;
import pl.fairydeck.authorization.domain.authorization.Authorization;
import pl.fairydeck.authorization.domain.authorization.AuthorizationStatus;
import pl.fairydeck.authorization.domain.card.Card;
import pl.fairydeck.authorization.domain.card.CardStatus;
import pl.fairydeck.authorization.domain.ledger.Balance;
import pl.fairydeck.authorization.domain.money.Money;

@WebMvcTest(CardController.class)
@MockitoBean(types = {IssueCard.class, CardQueries.class})
@TestConstructor(autowireMode = TestConstructor.AutowireMode.ALL)
class CardControllerTest {

    private static final Currency GBP = Currency.getInstance("GBP");
    private static final UUID CARDHOLDER_ID = UUID.fromString("7c9e6679-7425-40de-944b-e07fc1f90ae7");

    private final MockMvcTester mvc;
    private final IssueCard issueCard;
    private final CardQueries cardQueries;
    private final Card card = new Card(UUID.randomUUID(), CARDHOLDER_ID, CardStatus.ACTIVE, Money.of("500.00", GBP));

    CardControllerTest(MockMvcTester mvc, IssueCard issueCard, CardQueries cardQueries) {
        this.mvc = mvc;
        this.issueCard = issueCard;
        this.cardQueries = cardQueries;
    }

    @Test
    void issuesACardAndPointsAtIt() {
        given(issueCard.issue(CARDHOLDER_ID, Money.of("500.00", GBP))).willReturn(card);

        MvcTestResult result = mvc.post().uri("/v1/cards").contentType(MediaType.APPLICATION_JSON).content("""
                { "cardholderId": "%s", "creditLimit": 500.00, "currency": "GBP" }
                """.formatted(CARDHOLDER_ID)).exchange();

        assertThat(result).hasStatus(HttpStatus.CREATED)
                .hasHeader("Location", "/v1/cards/" + card.id())
                .bodyJson().isLenientlyEqualTo("""
                        { "id": "%s", "cardholderId": "%s", "status": "ACTIVE", "creditLimit": 500.00, "currency": "GBP" }
                        """.formatted(card.id(), CARDHOLDER_ID));
    }

    @Test
    void rejectsANonPositiveCreditLimit() {
        MvcTestResult result = mvc.post().uri("/v1/cards").contentType(MediaType.APPLICATION_JSON).content("""
                { "cardholderId": "%s", "creditLimit": 0, "currency": "GBP" }
                """.formatted(CARDHOLDER_ID)).exchange();

        assertThat(result).hasStatus(HttpStatus.BAD_REQUEST)
                .hasContentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON);
    }

    @Test
    void rejectsACreditLimitWhoseMinorUnitsOverflowWithoutIssuingIt() {
        MvcTestResult result = mvc.post().uri("/v1/cards").contentType(MediaType.APPLICATION_JSON).content("""
                { "cardholderId": "%s", "creditLimit": 92233720368547758.08, "currency": "GBP" }
                """.formatted(CARDHOLDER_ID)).exchange();

        assertThat(result).hasStatus(HttpStatus.BAD_REQUEST)
                .hasContentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON)
                .bodyJson().extractingPath("$.detail").asString().contains("outside the supported range");
        verifyNoInteractions(issueCard);
    }

    @Test
    void reportsTheBalanceDerivedFromTheLedger() {
        given(cardQueries.balance(card.id()))
                .willReturn(new Balance(Money.of("500.00", GBP), Money.of("30.00", GBP), Money.of("20.00", GBP)));

        MvcTestResult result = mvc.get().uri("/v1/cards/{id}/balance", card.id()).exchange();

        assertThat(result).hasStatus(HttpStatus.OK).bodyJson().isLenientlyEqualTo("""
                { "cardId": "%s", "currency": "GBP", "creditLimit": 500.00, "pending": 30.00, "settled": 20.00, "available": 450.00 }
                """.formatted(card.id()));
    }

    @Test
    void listsTransactionsMatchingTheFilter() {
        Authorization approved = new Authorization(UUID.randomUUID(), card.id(), Money.of("12.34", GBP), "Coffee Corner",
                "key", AuthorizationStatus.APPROVED, null, Instant.parse("2026-09-07T10:00:00Z"), null);
        given(cardQueries.transactions(eq(card.id()), any())).willReturn(List.of(approved));

        MvcTestResult result = mvc.get().uri("/v1/cards/{id}/transactions", card.id())
                .param("from", "2026-09-01T00:00:00Z")
                .param("to", "2026-09-08T00:00:00Z")
                .param("status", "APPROVED")
                .exchange();

        assertThat(result).hasStatus(HttpStatus.OK).bodyJson().isLenientlyEqualTo("""
                [ { "id": "%s", "status": "APPROVED", "amount": 12.34 } ]
                """.formatted(approved.id()));
        ArgumentCaptor<TransactionFilter> filter = ArgumentCaptor.forClass(TransactionFilter.class);
        verify(cardQueries).transactions(eq(card.id()), filter.capture());
        assertThat(filter.getValue()).isEqualTo(new TransactionFilter(
                Instant.parse("2026-09-01T00:00:00Z"), Instant.parse("2026-09-08T00:00:00Z"), AuthorizationStatus.APPROVED));
    }

    @Test
    void listsEverythingWhenNoFilterIsGiven() {
        given(cardQueries.transactions(eq(card.id()), any())).willReturn(List.of());

        mvc.get().uri("/v1/cards/{id}/transactions", card.id()).exchange();

        verify(cardQueries).transactions(card.id(), TransactionFilter.none());
    }

    @Test
    void answersNotFoundForAnUnknownCard() {
        given(cardQueries.balance(card.id())).willThrow(new CardNotFoundException(card.id()));

        MvcTestResult result = mvc.get().uri("/v1/cards/{id}/balance", card.id()).exchange();

        assertThat(result).hasStatus(HttpStatus.NOT_FOUND)
                .hasContentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON);
    }
}
