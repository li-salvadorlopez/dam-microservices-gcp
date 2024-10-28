package in.juulk.transfersservice.configuration;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app")
public record PubSubProperties(String topicName, String subscription) {
}
