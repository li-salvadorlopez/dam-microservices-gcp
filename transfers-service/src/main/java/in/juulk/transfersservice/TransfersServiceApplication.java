package in.juulk.transfersservice;

import in.juulk.transfersservice.configuration.PubSubProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

@SpringBootApplication
@EnableConfigurationProperties(PubSubProperties.class)
public class TransfersServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(TransfersServiceApplication.class, args);
    }

}
