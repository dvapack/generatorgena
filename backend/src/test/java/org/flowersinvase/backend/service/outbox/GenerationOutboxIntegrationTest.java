package org.flowersinvase.backend.service.outbox;

import org.flowersinvase.backend.dto.generation.CreateGenerationRequest;
import org.flowersinvase.backend.service.generation.GenerationService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@Transactional
class GenerationOutboxIntegrationTest {

    @Autowired
    private GenerationService generationService;

    @Autowired
    private JdbcClient jdbcClient;

    @Test
    void generationAndCommandAreStoredTogether() {
        UUID userId = UUID.randomUUID();
        jdbcClient.sql("""
                insert into users (id, email, password_hash)
                values (:id, :email, :passwordHash)
                """)
                .param("id", userId)
                .param("email", userId + "@example.test")
                .param("passwordHash", "not-used-in-this-test")
                .update();

        var response = generationService.create(
                userId,
                new CreateGenerationRequest("flowers in a vase")
        );

        Integer generationCount = jdbcClient.sql("""
                select count(*)
                from generation_requests
                where id = :id
                """)
                .param("id", response.id())
                .query(Integer.class)
                .single();
        String payload = jdbcClient.sql("""
                select payload::text
                from outbox_messages
                where aggregate_id = :id
                """)
                .param("id", response.id())
                .query(String.class)
                .single();

        assertThat(generationCount).isOne();
        assertThat(payload).contains(response.id().toString());
        assertThat(payload).contains("flowers in a vase");
    }
}
