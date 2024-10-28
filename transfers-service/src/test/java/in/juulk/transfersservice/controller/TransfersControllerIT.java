package in.juulk.transfersservice.controller;

import com.google.api.gax.core.CredentialsProvider;
import com.google.api.gax.core.NoCredentialsProvider;
import com.google.api.gax.grpc.GrpcTransportChannel;
import com.google.api.gax.rpc.FixedTransportChannelProvider;
import com.google.api.gax.rpc.TransportChannelProvider;
import com.google.cloud.pubsub.v1.SubscriptionAdminClient;
import com.google.cloud.pubsub.v1.SubscriptionAdminSettings;
import com.google.cloud.pubsub.v1.TopicAdminClient;
import com.google.cloud.pubsub.v1.TopicAdminSettings;
import com.google.cloud.spring.pubsub.PubSubAdmin;
import com.google.pubsub.v1.ProjectSubscriptionName;
import com.google.pubsub.v1.PullRequest;
import com.google.pubsub.v1.PullResponse;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.context.annotation.Bean;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.containers.PubSubEmulatorContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;


@SpringBootTest
@Testcontainers
@ActiveProfiles("test")
@AutoConfigureMockMvc
class TransfersControllerIT {

    @Autowired
    private MockMvc mockMvc;
    private static SubscriptionAdminClient subscriptionAdminClient;
    private static PubSubAdmin admin;
    private static ManagedChannel channel;

    @Container
    private static final PubSubEmulatorContainer pubsubEmulator =
            new PubSubEmulatorContainer(
                    DockerImageName.parse("gcr.io/google.com/cloudsdktool/cloud-sdk:317.0.0-emulators"));

    @DynamicPropertySource
    static void emulatorProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.cloud.gcp.pubsub.emulator-host", pubsubEmulator::getEmulatorEndpoint);
    }

    @BeforeAll
    static void setup() throws Exception {
        channel = ManagedChannelBuilder.forTarget("dns:///" + pubsubEmulator.getEmulatorEndpoint())
                        .usePlaintext()
                        .build();
        TransportChannelProvider channelProvider =
                FixedTransportChannelProvider.create(GrpcTransportChannel.create(channel));

        TopicAdminClient topicAdminClient =
                TopicAdminClient.create(
                        TopicAdminSettings.newBuilder()
                                .setCredentialsProvider(NoCredentialsProvider.create())
                                .setTransportChannelProvider(channelProvider)
                                .build());

        subscriptionAdminClient = SubscriptionAdminClient.create(
                SubscriptionAdminSettings.newBuilder()
                        .setTransportChannelProvider(channelProvider)
                        .setCredentialsProvider(NoCredentialsProvider.create())
                        .build());

        admin = new PubSubAdmin(() -> "test-project", topicAdminClient, subscriptionAdminClient);
        admin.createTopic("test-topic");
        admin.createSubscription("test-subscription", "test-topic");


    }

    @AfterAll
    static void cleanupPubsubClients() {
        admin.close();
        channel.shutdown();
    }

    @TestConfiguration
    static class PubSubEmulatorConfiguration {
        @Bean
        CredentialsProvider googleCredentials() {
            return NoCredentialsProvider.create();
        }
    }



    @Test
    void transferStatus() throws Exception {


        String transferId = UUID.randomUUID().toString();
        mockMvc.perform(post("/api/v1/transfers/"+transferId+"/status/1"))
                .andDo(print());

        String projectSubscriptionName = ProjectSubscriptionName.format("test-project", "test-subscription");

        PullRequest pullRequest = PullRequest.newBuilder()
                .setReturnImmediately(true)
                .setMaxMessages(10)
                .setSubscription(projectSubscriptionName)
                .build();

        PullResponse pullResponse = subscriptionAdminClient.getStub().pullCallable().call(pullRequest);
        List<String> messages =  pullResponse.getReceivedMessagesList().stream()
                .map(message -> message.getMessage().getData().toStringUtf8())
                .collect(Collectors.toList());

        await().atMost(60, TimeUnit.SECONDS )
                .untilAsserted(
                        () -> assertThat(messages).hasSize(1)
                );
    }
}