package pl.fairydeck.authorization.adapter.out.persistence;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Optional;
import java.util.UUID;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;
import pl.fairydeck.authorization.application.port.out.CardRepository;
import pl.fairydeck.authorization.domain.card.Card;
import pl.fairydeck.authorization.domain.card.CardStatus;

@Repository
class JdbcCardRepository implements CardRepository {

    private static final String COLUMNS = "id, cardholder_id, status, credit_limit_minor, currency";

    private final JdbcClient jdbc;

    JdbcCardRepository(JdbcClient jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    public void save(Card card) {
        jdbc.sql("INSERT INTO cards (" + COLUMNS + ") VALUES (:id, :cardholderId, :status, :creditLimitMinor, :currency)")
                .param("id", card.id())
                .param("cardholderId", card.cardholderId())
                .param("status", card.status().name())
                .param("creditLimitMinor", card.creditLimit().minorUnits())
                .param("currency", card.creditLimit().currency().getCurrencyCode())
                .update();
    }

    @Override
    public Optional<Card> findById(UUID id) {
        return jdbc.sql("SELECT " + COLUMNS + " FROM cards WHERE id = :id")
                .param("id", id)
                .query(JdbcCardRepository::toCard)
                .optional();
    }

    private static Card toCard(ResultSet row, int rowNumber) throws SQLException {
        return new Card(
                Columns.uuid(row, "id"),
                Columns.uuid(row, "cardholder_id"),
                CardStatus.valueOf(row.getString("status")),
                Columns.money(row, "credit_limit_minor", "currency"));
    }
}
