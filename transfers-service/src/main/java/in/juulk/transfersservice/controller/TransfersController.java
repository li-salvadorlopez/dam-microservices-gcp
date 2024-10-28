package in.juulk.transfersservice.controller;

import in.juulk.transfersservice.model.Transfer;
import in.juulk.transfersservice.service.TransfersEventsPublisher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/transfers")
public class TransfersController {

    private static final Logger LOGGER = LoggerFactory.getLogger(TransfersController.class);

    private final TransfersEventsPublisher publisher;

    public TransfersController(TransfersEventsPublisher publisher) {
        this.publisher = publisher;
    }

    @PostMapping("/{transferId}/status/{status}")
    public ResponseEntity<String> transferStatus(@PathVariable String transferId, @PathVariable int status){
        LOGGER.info("Received request with status: {}", status);
        publisher.publishTransferEvent(new Transfer(transferId, status));
        return new ResponseEntity<>(HttpStatusCode.valueOf(200));
    }

}
