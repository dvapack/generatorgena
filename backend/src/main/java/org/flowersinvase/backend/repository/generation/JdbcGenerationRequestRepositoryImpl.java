package org.flowersinvase.backend.repository.generation;

import org.flowersinvase.backend.entity.generation.GenerationEntity;
import org.flowersinvase.backend.enums.GenerationStatus;
import org.flowersinvase.backend.enums.GenerationType;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

import java.sql.Types;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public class JdbcGenerationRequestRepositoryImpl implements GenerationRequestRepository {

    private final static RowMapper<GenerationEntity> ROW_MAPPER =
            (resultSet, rowNum) -> new GenerationEntity(
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

    public JdbcGenerationRequestRepositoryImpl(JdbcClient jdbcClient) {
        this.jdbcClient = jdbcClient;
    }

    @Override
    public GenerationEntity save(GenerationEntity generationEntity) {
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
                .param("id", generationEntity.id())
                .param("userId", generationEntity.userId())
                .param("prompt", generationEntity.prompt())
                .param("type", generationEntity.type().name())
                .param("status", generationEntity.status().name())
                .param("rating", generationEntity.rating(), Types.INTEGER)
                .param("createdAt", generationEntity.createdAt(), Types.TIMESTAMP_WITH_TIMEZONE)
                .param("completedAt", generationEntity.completedAt(), Types.TIMESTAMP_WITH_TIMEZONE)
                .query(ROW_MAPPER)
                .single();
    }

    @Override
    public List<GenerationEntity> findAllByUserId(UUID userId, int offset, int limit) {
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
    public Optional<GenerationEntity> findById(UUID id) {
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
    public Optional<GenerationEntity> findByIdAndUserId(UUID id, UUID userId) {
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
    public Optional<GenerationEntity> findByIdForUpdate(UUID id) {
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
            for update
            """)
                .param("id", id)
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
