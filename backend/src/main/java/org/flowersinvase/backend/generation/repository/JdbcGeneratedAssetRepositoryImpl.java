package org.flowersinvase.backend.generation.repository;

import org.flowersinvase.backend.generation.entity.GeneratedAsset;
import org.flowersinvase.backend.generation.entity.GeneratedAssetType;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

import java.sql.Types;
import java.time.OffsetDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public class JdbcGeneratedAssetRepositoryImpl implements  GeneratedAssetRepository {

    private static final RowMapper<GeneratedAsset> ROW_MAPPER =
            (resultSet, rowNumber) -> new GeneratedAsset(
                    resultSet.getObject("id", UUID.class),
                    resultSet.getObject("request_id", UUID.class),
                    resultSet.getString("object_key"),
                    GeneratedAssetType.valueOf(resultSet.getString("asset_type")),
                    resultSet.getString("content_type"),
                    resultSet.getObject("size_bytes", Integer.class),
                    resultSet.getObject("duration", Integer.class),
                    resultSet.getObject("width", Integer.class),
                    resultSet.getObject("height", Integer.class),
                    resultSet.getObject("created_at", OffsetDateTime.class)
            );

    private final JdbcClient jdbcClient;

    public JdbcGeneratedAssetRepositoryImpl(JdbcClient jdbcClient) {
        this.jdbcClient = jdbcClient;
    }


    @Override
    public GeneratedAsset save(GeneratedAsset generatedAsset) {
        return jdbcClient.sql("""
                insert into generated_assets (
                    id,
                    request_id,
                    object_key,
                    asset_type,
                    content_type,
                    size_bytes,
                    duration,
                    width,
                    height,
                    created_at
                )
                values (
                    :id,
                    :requestId,
                    :objectKey,
                    :assetType,
                    :contentType,
                    :sizeBytes,
                    :duration,
                    :width,
                    :height,
                    coalesce(:createdAt, current_timestamp)
                )
                returning
                    id,
                    request_id,
                    object_key,
                    asset_type,
                    content_type,
                    size_bytes,
                    duration,
                    width,
                    height,
                    created_at
                """)
                .param("id", generatedAsset.id())
                .param("requestId", generatedAsset.requestId())
                .param("objectKey", generatedAsset.objectKey())
                .param("assetType", generatedAsset.assetType().name())
                .param("contentType", generatedAsset.contentType())
                .param("sizeBytes", generatedAsset.sizeBytes(), Types.INTEGER)
                .param("duration", generatedAsset.duration(), Types.INTEGER)
                .param("width", generatedAsset.width(), Types.INTEGER)
                .param("height", generatedAsset.height(), Types.INTEGER)
                .param("createdAt", generatedAsset.createdAt(), Types.TIMESTAMP_WITH_TIMEZONE)
                .query(ROW_MAPPER)
                .single();
    }

    @Override
    public Optional<GeneratedAsset> findByUserIdAndRequestId(UUID userId, UUID requestId) {
        return jdbcClient.sql("""
                select
                    ga.id,
                    ga.request_id,
                    ga.object_key,
                    ga.asset_type,
                    ga.content_type,
                    ga.size_bytes,
                    ga.duration,
                    ga.width,
                    ga.height,
                    ga.created_at
                from generated_assets ga
                join generation_requests gr
                    on gr.id = ga.request_id
                where ga.request_id = :requestId
                  and gr.user_id = :userId
                """)
                .param("requestId", requestId)
                .param("userId", userId)
                .query(ROW_MAPPER)
                .optional();
    }

    @Override
    public Optional<GeneratedAsset> findByRequestId(UUID requestId) {
        return jdbcClient.sql("""
            select
                id,
                request_id,
                object_key,
                asset_type,
                content_type,
                size_bytes,
                duration,
                width,
                height,
                created_at
            from generated_assets
            where request_id = :requestId
            """)
                .param("requestId", requestId)
                .query(ROW_MAPPER)
                .optional();
    }

    @Override
    public List<GeneratedAsset> findAllByRequestIds(Collection<UUID> requestIds) {
        if (requestIds.isEmpty()) {
            return List.of();
        }
        return jdbcClient.sql("""
                select *
                from generated_assets
                where request_id in (:requestIds)
                """)
                .param("requestIds", requestIds)
                .query(ROW_MAPPER)
                .list();
    }

    @Override
    public boolean deleteByRequestId(UUID requestId) {
        int affectedRows = jdbcClient.sql("""
                delete from generated_assets
                where request_id = :requestId
                """)
                .param("requestId", requestId)
                .update();

        return affectedRows == 1;
    }

    @Override
    public boolean deleteByUserIdAndRequestId(UUID userId, UUID requestId) {
        int affectedRows = jdbcClient.sql("""
                delete
                from generated_assets ga
                using generation_requests gr
                where gr.id = ga.request_id
                  and gr.user_id = :userId 
                  and ga.request_id = :requestId
                """)
                .param("requestId", requestId)
                .param("userId", userId)
                .update();

        return affectedRows == 1;
    }
}
