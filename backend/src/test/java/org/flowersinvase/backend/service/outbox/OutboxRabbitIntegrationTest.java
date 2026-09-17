package org.flowersinvase.backend.service.outbox;

import org.flowersinvase.backend.config.rabbit.RabbitTopologyProperties;
import org.flowersinvase.backend.dto.generation.CreateGenerationRequest;
import org.flowersinvase.backend.entity.user.User;
import org.flowersinvase.backend.repository.user.UserRepository;
import org.flowersinvase.backend.service.generation.GenerationService;
import org.junit.jupiter.api.Test;
import org.springframework.amqp.core.AmqpAdmin;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.simple.JdbcClient;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
class OutboxRabbitIntegrationTest {

    @Autowired
    private GenerationService generationService;

    @Autowired
    private OutboxBatchPublisher batchPublisher;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private JdbcClient jdbcClient;

    @Autowired
    private AmqpAdmin amqpAdmin;

    @Autowired
    private RabbitTopologyProperties rabbitProperties;

    @Test
    void pendingCommandIsConfirmedByRabbitAndMarkedPublished() {
        UUID userId = UUID.randomUUID();
        userRepository.save(new User(
                userId,
                userId + "@example.test",
                "not-used-in-this-test"
        ));
        amqpAdmin.purgeQueue(rabbitProperties.requestsQueue(), true);

        try {
            var response = generationService.create(
                    userId,
                    new CreateGenerationRequest("outbox integration test")
            );

            batchPublisher.publishBatch();

            Boolean published = jdbcClient.sql("""
                    select published_at is not null
                    from outbox_messages
                    where aggregate_id = :generationId
                    """)
                    .param("generationId", response.id())
                    .query(Boolean.class)
                    .single();
            assertThat(published).isTrue();
            assertThat(amqpAdmin.getQueueInfo(rabbitProperties.requestsQueue()))
                    .isNotNull();
        } finally {
            userRepository.deleteById(userId);
            amqpAdmin.purgeQueue(rabbitProperties.requestsQueue(), true);
        }
    }
}
