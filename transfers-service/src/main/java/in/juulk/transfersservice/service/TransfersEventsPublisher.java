package in.juulk.transfersservice.service;

import com.google.api.client.util.Value;
import com.google.cloud.spring.pubsub.PubSubAdmin;
import com.google.cloud.spring.pubsub.core.PubSubTemplate;
import in.juulk.transfersservice.configuration.PubSubProperties;
import in.juulk.transfersservice.model.Transfer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

@Service
public class TransfersEventsPublisher {

    private static final Logger LOGGER = LoggerFactory.getLogger(TransfersEventsPublisher.class);

    private final PubSubTemplate pubSubTemplate;
    private final PubSubAdmin pubSubAdmin;
    private final PubSubProperties pubSubProperties;



    @Autowired
    public TransfersEventsPublisher(PubSubTemplate pubSubTemplate, PubSubAdmin pubSubAdmin, PubSubProperties pubSubProperties) {
        this.pubSubTemplate = pubSubTemplate;
        this.pubSubAdmin = pubSubAdmin;
        this.pubSubProperties = pubSubProperties;
    }



    public void publishTransferEvent(Transfer event){
        CompletableFuture<String> published = pubSubTemplate.publish(pubSubProperties.topicName(), event.toString());
        try {
            published.get();
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        } catch (ExecutionException e) {
            throw new RuntimeException(e);
        }
    }
}
