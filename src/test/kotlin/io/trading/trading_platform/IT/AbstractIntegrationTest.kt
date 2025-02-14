package io.trading.trading_platform.IT

import org.apache.kafka.clients.admin.AdminClient
import org.apache.kafka.clients.admin.NewTopic
import org.junit.jupiter.api.BeforeAll
import org.springframework.test.context.DynamicPropertyRegistry
import org.springframework.test.context.DynamicPropertySource
import org.testcontainers.containers.GenericContainer
import org.testcontainers.containers.MongoDBContainer
import org.testcontainers.containers.Network
import org.testcontainers.containers.wait.strategy.Wait
import org.testcontainers.junit.jupiter.Container
import org.testcontainers.kafka.KafkaContainer
import org.testcontainers.utility.DockerImageName
import java.time.Duration

abstract class AbstractIntegrationTest {
    companion object {
        private val network = Network.newNetwork()

        @BeforeAll
        @JvmStatic
        fun setupKafkaTopics() {
            val adminProps = mapOf(
                "bootstrap.servers" to kafkaContainer.bootstrapServers,
                "security.protocol" to "PLAINTEXT"
            )

            AdminClient.create(adminProps).use { admin ->
                val topics = listOf(
                    NewTopic("trade-event", 1, 1),
                    NewTopic("volatility-alerts", 1, 1)
                )
                admin.createTopics(topics).all().get()
            }
        }

        @Container
        @JvmField
        val mongoDBContainer = MongoDBContainer(DockerImageName.parse("mongo:6.0.5"))
            .waitingFor(Wait.forLogMessage(".*Waiting for connections.*", 1))
            .withReuse(true)

        @Container
        @JvmField
        val zookeeper = GenericContainer("confluentinc/cp-zookeeper:7.6.0")
            .withExposedPorts(2181)
            .withNetwork(network)
            .withNetworkAliases("zookeeper")

        @Container
        @JvmField
        val kafkaContainer = KafkaContainer(DockerImageName.parse("apache/kafka:3.7.0"))
            .withReuse(true)
            .dependsOn(zookeeper)
            .withNetwork(network)
            .withEnv("KAFKA_AUTO_CREATE_TOPICS_ENABLE", "true")
            .withStartupTimeout(Duration.ofSeconds(120))

        @Container
        @JvmField
        val redisContainer = GenericContainer(DockerImageName.parse("redis:7.2-alpine"))
            .withExposedPorts(6379)
            .withReuse(true)
            .withNetwork(network) // Используем общую сеть
            .withStartupTimeout(Duration.ofSeconds(60))

        @JvmStatic
        @DynamicPropertySource
        fun properties(registry: DynamicPropertyRegistry) {
            registry.add("spring.kafka.bootstrap-servers", kafkaContainer::getBootstrapServers)
            registry.add("spring.kafka.zookeeper.connect") { "zookeeper:2181" }
           registry.add("spring.data.mongodb.uri", mongoDBContainer::getReplicaSetUrl)
            //registry.add("spring.data.mongodb.uri") { "mongodb://${mongoDBContainer.host}:${mongoDBContainer.firstMappedPort}" }

            registry.add("spring.data.redis.host") { redisContainer.host }
            registry.add("spring.data.redis.port") { redisContainer.getMappedPort(6379).toString() }
        }
    }

}