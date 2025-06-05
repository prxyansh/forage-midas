package com.jpmc.midascore.component;

import com.jpmc.midascore.entity.TransactionRecord;
import com.jpmc.midascore.entity.UserRecord;
import com.jpmc.midascore.foundation.Balance;
import com.jpmc.midascore.foundation.Transaction;
import com.jpmc.midascore.repository.TransactionRepository;
import com.jpmc.midascore.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;

@Component
public class DatabaseConduit {
    private static final Logger logger = LoggerFactory.getLogger(DatabaseConduit.class);
    private static final String INCENTIVE_API_URL = "http://localhost:8080/incentive";

    private final UserRepository userRepository;
    private final TransactionRepository transactionRepository;

    @Autowired
    private RestTemplate restTemplate;

    public DatabaseConduit(UserRepository userRepository, TransactionRepository transactionRepository) {
        this.userRepository = userRepository;
        this.transactionRepository = transactionRepository;
    }

    public void save(UserRecord userRecord) {
        userRepository.save(userRecord);

        // Track wilbur's initial balance
        if ("wilbur".equals(userRecord.getName())) {
            logger.info("WILBUR-TRACKER: Initial balance = {}", userRecord.getBalance());
        }
    }

    /**
     * Process a transaction by validating and updating balances if valid.
     * After validation, it calls the incentive API to get any additional incentive amount.
     *
     * @param transaction The transaction to process
     * @return True if the transaction was successful, false otherwise
     */
    @Transactional
    public boolean processTransaction(Transaction transaction) {
        // 1. Validate sender and recipient
        UserRecord sender = userRepository.findById(transaction.getSenderId());
        UserRecord recipient = userRepository.findById(transaction.getRecipientId());

        if (sender == null || recipient == null) {
            logger.warn("Invalid sender ID {} or recipient ID {}. Transaction discarded.",
                    transaction.getSenderId(), transaction.getRecipientId());
            return false;
        }

        // 2. Validate sender has sufficient balance
        float amount = transaction.getAmount();
        if (sender.getBalance() < amount) {
            logger.warn("Sender {} has insufficient balance {}, required {}. Transaction discarded.",
                    sender.getName(), sender.getBalance(), amount);

            // Create a record of the failed transaction
            TransactionRecord failedTransaction = new TransactionRecord(sender, recipient, amount, false);
            transactionRepository.save(failedTransaction);
            return false;
        }

        // 3. Call incentive API to get incentive amount
        float incentiveAmount = 0.0f;
        try {
            Balance incentive = restTemplate.postForObject(INCENTIVE_API_URL, transaction, Balance.class);
            if (incentive != null) {
                incentiveAmount = incentive.getAmount();
            }
            logger.info("Received incentive amount: {} for transaction from {} to {}",
                    incentiveAmount, sender.getName(), recipient.getName());
        } catch (Exception e) {
            logger.error("Failed to call incentive API: {}", e.getMessage());
            // Continue with the transaction even if incentive API call fails
        }

        // 4. Update balances - deduct amount from sender, add amount + incentive to recipient
        sender.setBalance(sender.getBalance() - amount);
        recipient.setBalance(recipient.getBalance() + amount + incentiveAmount);

        // 5. Save updated user records
        userRepository.save(sender);
        userRepository.save(recipient);

        // 6. Create and save transaction record with incentive
        TransactionRecord transactionRecord = new TransactionRecord(sender, recipient, amount, incentiveAmount, true);
        transactionRepository.save(transactionRecord);

        // Log wilbur's balance changes for tracking purposes
        if ("wilbur".equals(sender.getName())) {
            logger.info("WILBUR-TRACKER: After sending {} to {}, new balance = {}",
                    amount, recipient.getName(), sender.getBalance());
        }

        if ("wilbur".equals(recipient.getName())) {
            logger.info("WILBUR-TRACKER: After receiving {} from {} (plus incentive: {}), new balance = {}",
                    amount, sender.getName(), incentiveAmount, recipient.getBalance());
        }

        logger.info("Transaction processed successfully: {} -> {}, amount: {}, incentive: {}",
                sender.getName(), recipient.getName(), amount, incentiveAmount);
        return true;
    }

    /**
     * Get a user's balance by name
     * @param name The user name
     * @return The user's current balance
     */
    public float getUserBalanceByName(String name) {
        Iterable<UserRecord> users = userRepository.findAll();
        for (UserRecord user : users) {
            if (name.equals(user.getName())) {
                if ("wilbur".equals(name)) {
                    logger.info("WILBUR-TRACKER: Final balance = {}", user.getBalance());
                }
                return user.getBalance();
            }
        }
        return -1;
    }

    /**
     * Get a user's balance by ID
     * @param userId The user ID
     * @return The user's current balance, or 0 if user doesn't exist
     */
    public float getUserBalanceById(long userId) {
        UserRecord user = userRepository.findById(userId);
        if (user == null) {
            logger.info("User with ID {} not found, returning balance of 0", userId);
            return 0;
        }
        logger.info("Found balance {} for user ID {}", user.getBalance(), userId);
        return user.getBalance();
    }
}
