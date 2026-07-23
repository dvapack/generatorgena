package org.flowersinvase.backend.generation.repository;

import org.flowersinvase.backend.generation.entity.GenerationRequest;
import org.flowersinvase.backend.generation.entity.GenerationStatus;
import org.flowersinvase.backend.generation.entity.GenerationType;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

import java.sql.Types;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public class JdbcGenerationRequestRepository implements GenerationRequestRepository {

    private final static RowMapper<GenerationRequest> ROW_MAPPER =
            (resultSet, rowNum) -> new GenerationRequest(
                    resultSet.getObject("id", UUID.class),
                    resultSet.getObject("user_id", UUID.class),
                    resultSet.getString("prompt"),
                    GenerationType.valueOf(
                            resultSet.getString("type")
                    ),
                    GenerationStatus.valueOf(
                            resultSet.getString("status")
                    ),
                    resultSet.getObject("rating", Integer.class),
                    resultSet.getObject("created_at", OffsetDateTime.class),
                    resultSet.getObject("completed_at", OffsetDateTime.class)
            );

    private final JdbcClient jdbcClient;

    public JdbcGenerationRequestRepository(JdbcClient jdbcClient) {
        this.jdbcClient = jdbcClient;
    }

    @Override
    public GenerationRequest save(GenerationRequest generationRequest) {
        return jdbcClient.sql("""
                insert into generation_requests (
                    id,
                    user_id,
                    prompt,
                    type,
                    status,
                    rating,
                    created_at,
                    completed_at
                )
                values (
                    :id,
                    :userId,
                    :prompt,
                    :type,
                    :status,
                    :rating,
                    coalesce(:createdAt, current_timestamp),
                    :completedAt
                )
                returning
                    id,
                    user_id,
                    prompt,
                    type,
                    status,
                    rating,
                    created_at,
                    completed_at
                """)
                .param("id", generationRequest.id())
                .param("userId", generationRequest.userId())
                .param("prompt", generationRequest.prompt())
                .param("type", generationRequest.type().name())
                .param("status", generationRequest.status().name())
                .param("rating", generationRequest.rating(), Types.INTEGER)
                .param("createdAt", generationRequest.createdAt(), Types.TIMESTAMP_WITH_TIMEZONE)
                .param("completedAt", generationRequest.completedAt(), Types.TIMESTAMP_WITH_TIMEZONE)
                .query(ROW_MAPPER)
                .single();
    }

    @Override
    public List<GenerationRequest> findAllByUserId(UUID userId, int offset, int limit) {
        return jdbcClient.sql("""
                select
                    id,
                    user_id,
                    prompt,
                    type,
                    status,
                    rating,
                    created_at,
                    completed_at
                from generation_requests
                where user_id = :userId
                order by created_at desc, id desc
                limit :limit
                offset :offset
                """)
                .param("userId", userId)
                .param("limit", limit)
                .param("offset", offset)
                .query(ROW_MAPPER)
                .list();
    }

    @Override
    public Optional<GenerationRequest> findById(UUID id) {
        return jdbcClient.sql("""
                select
                    id,
                    user_id,
                    prompt,
                    type,
                    status,
                    rating,
                    created_at,
                    completed_at
                from generation_requests
                where id = :id
                """)
                .param("id", id)
                .query(ROW_MAPPER)
                .optional();
    }

    @Override
    public Optional<GenerationRequest> findByIdAndUserId(UUID id, UUID userId) {
        return jdbcClient.sql("""
                select
                    id,
                    user_id,
                    prompt,
                    type,
                    status,
                    rating,
                    created_at,
                    completed_at
                from generation_requests
                where id = :id
                  and user_id = :userId
                """)
                .param("id", id)
                .param("userId", userId)
                .query(ROW_MAPPER)
                .optional();
    }

    @Override
    public boolean updateRating(UUID id, UUID userId, Integer rating) {
        int affectedRows = jdbcClient.sql("""
                update generation_requests
                set rating = :rating
                where id = :id
                  and user_id = :userId
                """)
                .param("id", id)
                .param("userId", userId)
                .param("rating", rating)
                .update();
        return affectedRows == 1;
    }

    @Override
    public boolean updateStatus(UUID id, GenerationStatus status, OffsetDateTime completedAt) {
        int affectedRows = jdbcClient.sql("""
                update generation_requests
                set status = :status,
                    completed_at = :completedAt
                where id = :id
                """)
                .param("id", id)
                .param("status", status.name())
                .param("completedAt", completedAt, Types.TIMESTAMP_WITH_TIMEZONE)
                .update();
        return affectedRows == 1;
    }

    @Override
    public boolean deleteByUserAndId(UUID userId, UUID id) {
        int affectedRows = jdbcClient.sql("""
                delete from generation_requests
                where id = :id
                    and user_id = :userId
                """)
                .param("id", id)
                .param("userId", userId)
                .update();
        return affectedRows == 1;
    }

    @Override
    public long countByUserId(UUID userId) {
        return jdbcClient.sql("""
            select count(*)
            from generation_requests
            where user_id = :userId
            """)
                .param("userId", userId)
                .query(Long.class)
                .single();
    }
}
