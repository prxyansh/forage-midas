package com.jpmc.midascore.component;

import com.jpmc.midascore.foundation.Transaction;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
public class KafkaConsumer {

    private static final Logger logger = LoggerFactory.getLogger(KafkaConsumer.class);
    private final DatabaseConduit databaseConduit;

    @Value("${general.kafka-topic}")
    private String topic;

    public KafkaConsumer(DatabaseConduit databaseConduit) {
        this.databaseConduit = databaseConduit;
    }

    /**
     * Kafka listener for incoming transactions.
     *
     * @param transaction The transaction object deserialized from Kafka message
     */
    @KafkaListener(topics = "${general.kafka-topic}", groupId = "${spring.kafka.consumer.group-id}")
    public void consumeTransaction(Transaction transaction) {
        logger.info("Received transaction: {}", transaction);

        // Process the transaction using DatabaseConduit
        boolean success = databaseConduit.processTransaction(transaction);

        if (success) {
            logger.info("Transaction processed successfully: amount = {}", transaction.getAmount());
        } else {
            logger.info("Transaction processing failed: amount = {}", transaction.getAmount());
        }
    }
}
