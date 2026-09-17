package org.flowersinvase.backend.repository.outbox;

import org.flowersinvase.backend.entity.outbox.OutboxMessage;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

@Repository
public class JdbcOutboxRepository implements OutboxRepository {

    private static final RowMapper<OutboxMessage> ROW_MAPPER = (resultSet, rowNumber) ->
            new OutboxMessage(
                    resultSet.getObject("id", UUID.class),
                    resultSet.getObject("aggregate_id", UUID.class),
                    resultSet.getString("payload"),
                    resultSet.getObject("created_at", OffsetDateTime.class),
                    resultSet.getInt("attempts"),
                    resultSet.getObject("next_attempt_at", OffsetDateTime.class)
            );

    private final JdbcClient jdbcClient;

    public JdbcOutboxRepository(JdbcClient jdbcClient) {
        this.jdbcClient = jdbcClient;
    }

    @Override
    public void save(OutboxMessage message) {
        jdbcClient.sql("""
                insert into outbox_messages (
                    id, aggregate_id, payload, created_at, attempts, next_attempt_at
                ) values (
                    :id, :aggregateId, cast(:payload as jsonb),
                    coalesce(:createdAt, current_timestamp), :attempts,
                    coalesce(:nextAttemptAt, current_timestamp)
                )
                """)
                .param("id", message.id())
                .param("aggregateId", message.aggregateId())
                .param("payload", message.payload())
                .param("createdAt", message.createdAt())
                .param("attempts", message.attempts())
                .param("nextAttemptAt", message.nextAttemptAt())
                .update();
    }

    @Override
    public List<OutboxMessage> findPendingForUpdate(int limit) {
        return jdbcClient.sql("""
                select id, aggregate_id, payload, created_at, attempts, next_attempt_at
                from outbox_messages
                where published_at is null
                  and next_attempt_at <= current_timestamp
                order by created_at
                limit :limit
                for update skip locked
                """)
                .param("limit", limit)
                .query(ROW_MAPPER)
                .list();
    }

    @Override
    public void markPublished(UUID id) {
        jdbcClient.sql("""
                update outbox_messages
                set published_at = current_timestamp,
                    last_error = null
                where id = :id
                """)
                .param("id", id)
                .update();
    }

    @Override
    public void scheduleRetry(UUID id, int attempts, OffsetDateTime nextAttemptAt, String error) {
        jdbcClient.sql("""
                update outbox_messages
                set attempts = :attempts,
                    next_attempt_at = :nextAttemptAt,
                    last_error = :error
                where id = :id
                """)
                .param("id", id)
                .param("attempts", attempts)
                .param("nextAttemptAt", nextAttemptAt)
                .param("error", error)
                .update();
    }

    @Override
    public int deletePublishedBefore(OffsetDateTime threshold) {
        return jdbcClient.sql("""
                delete from outbox_messages
                where published_at is not null
                  and published_at < :threshold
                """)
                .param("threshold", threshold)
                .update();
    }
}

