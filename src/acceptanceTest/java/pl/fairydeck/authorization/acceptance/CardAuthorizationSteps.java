package pl.fairydeck.authorization.acceptance;

import static com.github.tomakehurst.wiremock.client.WireMock.okJson;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static org.assertj.core.api.Assertions.assertThat;
import static pl.fairydeck.authorization.acceptance.AcceptanceContext.RISK_ENGINE;

import io.cucumber.java.Before;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import java.math.BigDecimal;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Currency;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.MediaType;
import org.springframework.web.client.RestClient;
import pl.fairydeck.authorization.application.port.out.CardRepository;
import pl.fairydeck.authorization.domain.card.Card;
import pl.fairydeck.authorization.domain.card.CardStatus;
import pl.fairydeck.authorization.domain.money.Money;

public class CardAuthorizationSteps {

    private static final String SCORES = "/v1/scores";
    private static final String LOW_RISK = "{\"score\": 5}";
    private static final int FAR_BEYOND_THE_TIMEOUT_MS = 2_000;
    private static final String DEFAULT_MERCHANT = "Coffee Corner";

    private final RestClient api;
    private final CardRepository cards;
    private final List<AuthorizationView> answers = new ArrayList<>();
    private UUID cardId;
    private PurchaseRequest lastPurchase;
    private Duration lastDecisionTime = Duration.ZERO;

    CardAuthorizationSteps(@Value("${local.server.port}") int port, CardRepository cards) {
        this.api = RestClient.builder().baseUrl("http://localhost:" + port).build();
        this.cards = cards;
    }

    @Before
    public void forgetPreviousRiskEngineBehaviour() {
        RISK_ENGINE.resetAll();
    }

    @Given("the risk engine scores every purchase as low risk")
    public void theRiskEngineScoresEveryPurchaseAsLowRisk() {
        RISK_ENGINE.stubFor(post(SCORES).willReturn(okJson(LOW_RISK)));
    }

    @Given("the risk engine does not answer in time")
    public void theRiskEngineDoesNotAnswerInTime() {
        RISK_ENGINE.stubFor(post(SCORES).willReturn(okJson(LOW_RISK).withFixedDelay(FAR_BEYOND_THE_TIMEOUT_MS)));
    }

    @Given("a card with a credit limit of {bigdecimal} {word}")
    public void aCardWithACreditLimitOf(BigDecimal limit, String currency) {
        CardView card = api.post().uri("/v1/cards")
                .contentType(MediaType.APPLICATION_JSON)
                .body(new IssueCardRequest(UUID.randomUUID(), limit, currency))
                .retrieve()
                .body(CardView.class);
        cardId = card.id();
    }

    @Given("a blocked card with a credit limit of {bigdecimal} {word}")
    public void aBlockedCardWithACreditLimitOf(BigDecimal limit, String currency) {
        Card blocked = new Card(UUID.randomUUID(), UUID.randomUUID(), CardStatus.BLOCKED,
                Money.of(limit.toPlainString(), Currency.getInstance(currency)));
        cards.save(blocked);
        cardId = blocked.id();
    }

    @Given("an approved purchase of {bigdecimal} {word}")
    public void anApprovedPurchaseOf(BigDecimal amount, String currency) {
        aPurchaseIsMadeAt(amount, currency, DEFAULT_MERCHANT);
        thePurchaseIsApproved();
    }

    @When("a purchase of {bigdecimal} {word} is made at {string}")
    public void aPurchaseIsMadeAt(BigDecimal amount, String currency, String merchant) {
        purchase(new PurchaseRequest(cardId, amount, currency, merchant), UUID.randomUUID().toString());
    }

    @When("a purchase of {bigdecimal} {word} is made with idempotency key {string}")
    public void aPurchaseIsMadeWithIdempotencyKey(BigDecimal amount, String currency, String idempotencyKey) {
        purchase(new PurchaseRequest(cardId, amount, currency, DEFAULT_MERCHANT), idempotencyKey);
    }

    @When("the same purchase is retried with idempotency key {string}")
    public void theSamePurchaseIsRetriedWithIdempotencyKey(String idempotencyKey) {
        purchase(lastPurchase, idempotencyKey);
    }

    @When("{int} purchases of {bigdecimal} {word} are made at the same time")
    public void purchasesAreMadeAtTheSameTime(int count, BigDecimal amount, String currency) throws Exception {
        CountDownLatch go = new CountDownLatch(1);
        try (var threads = Executors.newVirtualThreadPerTaskExecutor()) {
            List<Future<AuthorizationView>> inFlight = IntStream.range(0, count)
                    .mapToObj(attempt -> threads.submit(() -> {
                        go.await();
                        return send(new PurchaseRequest(cardId, amount, currency, "Merchant " + attempt),
                                "race-" + cardId + "-" + attempt);
                    }))
                    .toList();
            go.countDown();
            for (Future<AuthorizationView> answer : inFlight) {
                answers.add(answer.get());
            }
        }
    }

    @When("the purchase is captured")
    public void thePurchaseIsCaptured() {
        transition("capture");
    }

    @When("the purchase is reversed")
    public void thePurchaseIsReversed() {
        transition("reverse");
    }

    @When("the capture is retried")
    public void theCaptureIsRetried() {
        transition("capture");
    }

    @Then("the purchase is approved")
    public void thePurchaseIsApproved() {
        assertThat(lastAnswer().status()).isEqualTo("APPROVED");
    }

    @Then("the purchase is declined because of {string}")
    public void thePurchaseIsDeclinedBecauseOf(String reason) {
        assertThat(lastAnswer().status()).isEqualTo("DECLINED");
        assertThat(lastAnswer().declineReason()).isEqualTo(reason);
    }

    @Then("the decision took less than {int} ms")
    public void theDecisionTookLessThan(int milliseconds) {
        assertThat(lastDecisionTime).isLessThan(Duration.ofMillis(milliseconds));
    }

    @Then("the authorization is {string}")
    public void theAuthorizationIs(String status) {
        assertThat(lastAnswer().status()).isEqualTo(status);
    }

    @Then("both answers refer to the same authorization")
    public void bothAnswersReferToTheSameAuthorization() {
        assertThat(answers).hasSize(2);
        assertThat(answers.get(1).id()).isEqualTo(answers.get(0).id());
    }

    @Then("the retry returns the same settled authorization")
    public void theRetryReturnsTheSameSettledAuthorization() {
        AuthorizationView retry = answers.get(answers.size() - 1);
        AuthorizationView original = answers.get(answers.size() - 2);
        assertThat(retry.id()).isEqualTo(original.id());
        assertThat(retry.status()).isEqualTo("CAPTURED");
    }

    @Then("exactly {int} of them are approved")
    public void exactlyOfThemAreApproved(int approved) {
        assertThat(answers).filteredOn(answer -> answer.status().equals("APPROVED")).hasSize(approved);
    }

    @Then("the available balance is {bigdecimal} {word}")
    public void theAvailableBalanceIs(BigDecimal available, String currency) {
        assertThat(balance().available()).isEqualByComparingTo(available);
        assertThat(balance().currency()).isEqualTo(currency);
    }

    @Then("the settled amount is {bigdecimal} {word}")
    public void theSettledAmountIs(BigDecimal settled, String currency) {
        assertThat(balance().settled()).isEqualByComparingTo(settled);
    }

    @Then("the card's transactions are {string}")
    public void theCardsTransactionsAre(String statuses) {
        assertThat(statusesOf(api.get().uri("/v1/cards/{id}/transactions", cardId))).isEqualTo(statuses);
    }

    @Then("the card's approved transactions are {string}")
    public void theCardsApprovedTransactionsAre(String statuses) {
        assertThat(statusesOf(api.get().uri("/v1/cards/{id}/transactions?status=APPROVED", cardId)))
                .isEqualTo(statuses);
    }

    private void purchase(PurchaseRequest request, String idempotencyKey) {
        lastPurchase = request;
        long startedAt = System.nanoTime();
        answers.add(send(request, idempotencyKey));
        lastDecisionTime = Duration.ofNanos(System.nanoTime() - startedAt);
    }

    private AuthorizationView send(PurchaseRequest request, String idempotencyKey) {
        return api.post().uri("/v1/authorizations")
                .header("Idempotency-Key", idempotencyKey)
                .contentType(MediaType.APPLICATION_JSON)
                .body(request)
                .retrieve()
                .body(AuthorizationView.class);
    }

    private void transition(String action) {
        answers.add(api.post().uri("/v1/authorizations/{id}/" + action, lastAnswer().id())
                .retrieve()
                .body(AuthorizationView.class));
    }

    private AuthorizationView lastAnswer() {
        return answers.getLast();
    }

    private BalanceView balance() {
        return api.get().uri("/v1/cards/{id}/balance", cardId).retrieve().body(BalanceView.class);
    }

    private String statusesOf(RestClient.RequestHeadersSpec<?> request) {
        List<AuthorizationView> transactions = request.retrieve().body(new ParameterizedTypeReference<>() { });
        return transactions.stream().map(AuthorizationView::status).collect(Collectors.joining(", "));
    }

    record IssueCardRequest(UUID cardholderId, BigDecimal creditLimit, String currency) {
    }

    record PurchaseRequest(UUID cardId, BigDecimal amount, String currency, String merchant) {
    }

    record CardView(UUID id) {
    }

    record AuthorizationView(UUID id, String status, String declineReason) {
    }

    record BalanceView(String currency, BigDecimal available, BigDecimal settled, BigDecimal pending) {
    }
}
