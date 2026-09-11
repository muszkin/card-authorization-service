Feature: Card authorization

  Purchases are authorized against the balance available on a card. The balance is derived from an
  append-only ledger of holds, releases, captures and refunds; it is never stored.

  Background:
    Given the risk engine scores every purchase as low risk

  Scenario: A purchase within the available balance is approved
    Given a card with a credit limit of 100.00 GBP
    When a purchase of 30.00 GBP is made at "Coffee Corner"
    Then the purchase is approved
    And the available balance is 70.00 GBP

  Scenario: A purchase exceeding the available balance is declined
    Given a card with a credit limit of 100.00 GBP
    When a purchase of 100.01 GBP is made at "Coffee Corner"
    Then the purchase is declined because of "INSUFFICIENT_FUNDS"
    And the available balance is 100.00 GBP

  Scenario: A purchase on a blocked card is declined
    Given a blocked card with a credit limit of 100.00 GBP
    When a purchase of 1.00 GBP is made at "Coffee Corner"
    Then the purchase is declined because of "CARD_NOT_ACTIVE"

  Scenario: A retried purchase with the same idempotency key is not authorized twice
    Given a card with a credit limit of 100.00 GBP
    When a purchase of 30.00 GBP is made with idempotency key "retry-once"
    And the same purchase is retried with idempotency key "retry-once"
    Then both answers refer to the same authorization
    And the available balance is 70.00 GBP

  Scenario: Concurrent purchases cannot overdraw the available balance
    Given a card with a credit limit of 100.00 GBP
    When 16 purchases of 10.00 GBP are made at the same time
    Then exactly 10 of them are approved
    And the available balance is 0.00 GBP

  Scenario: When the risk service times out, the purchase is declined
    Given a card with a credit limit of 100.00 GBP
    But the risk engine does not answer in time
    When a purchase of 30.00 GBP is made at "Coffee Corner"
    Then the purchase is declined because of "RISK_UNAVAILABLE"
    And the decision took less than 1000 ms
    And the available balance is 100.00 GBP

  Scenario: A fractional risk score declines the purchase without a hold
    Given a card with a credit limit of 100.00 GBP
    But the risk engine returns a fractional score
    When a purchase of 30.00 GBP is made at "Coffee Corner"
    Then the purchase is declined because of "RISK_UNAVAILABLE"
    And the available balance is 100.00 GBP

  Scenario: Capturing an authorization converts the hold into a settled charge
    Given a card with a credit limit of 100.00 GBP
    And an approved purchase of 30.00 GBP
    When the purchase is captured
    Then the authorization is "CAPTURED"
    And the settled amount is 30.00 GBP
    And the available balance is 70.00 GBP

  Scenario: Reversing an authorization releases the hold and restores the balance
    Given a card with a credit limit of 100.00 GBP
    And an approved purchase of 30.00 GBP
    When the purchase is reversed
    Then the authorization is "REVERSED"
    And the available balance is 100.00 GBP

  Scenario: Retrying a capture after it succeeded changes nothing
    Given a card with a credit limit of 100.00 GBP
    And an approved purchase of 30.00 GBP
    When the purchase is captured
    And the capture is retried
    Then the retry returns the same settled authorization
    And the settled amount is 30.00 GBP
    And the available balance is 70.00 GBP

  Scenario: The card's transactions show every decision, newest first
    Given a card with a credit limit of 100.00 GBP
    When a purchase of 30.00 GBP is made at "Coffee Corner"
    And a purchase of 80.00 GBP is made at "Bike Shop"
    Then the card's transactions are "DECLINED, APPROVED"
    And the card's approved transactions are "APPROVED"
