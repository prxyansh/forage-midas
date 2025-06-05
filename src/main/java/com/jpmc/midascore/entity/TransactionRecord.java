package com.jpmc.midascore.entity;

import jakarta.persistence.*;

@Entity
public class TransactionRecord {

    @Id
    @GeneratedValue
    private Long id;

    @ManyToOne
    @JoinColumn(name = "sender_id", nullable = false)
    private UserRecord sender;

    @ManyToOne
    @JoinColumn(name = "recipient_id", nullable = false)
    private UserRecord recipient;

    @Column(nullable = false)
    private float amount;

    @Column(nullable = false)
    private float incentiveAmount = 0.0f;

    @Column(nullable = false)
    private boolean successful;

    protected TransactionRecord() {
    }

    public TransactionRecord(UserRecord sender, UserRecord recipient, float amount, boolean successful) {
        this.sender = sender;
        this.recipient = recipient;
        this.amount = amount;
        this.successful = successful;
    }

    public TransactionRecord(UserRecord sender, UserRecord recipient, float amount, float incentiveAmount, boolean successful) {
        this.sender = sender;
        this.recipient = recipient;
        this.amount = amount;
        this.incentiveAmount = incentiveAmount;
        this.successful = successful;
    }

    public Long getId() {
        return id;
    }

    public UserRecord getSender() {
        return sender;
    }

    public UserRecord getRecipient() {
        return recipient;
    }

    public float getAmount() {
        return amount;
    }

    public float getIncentiveAmount() {
        return incentiveAmount;
    }

    public void setIncentiveAmount(float incentiveAmount) {
        this.incentiveAmount = incentiveAmount;
    }

    public boolean isSuccessful() {
        return successful;
    }

    @Override
    public String toString() {
        return String.format("Transaction[id=%d, sender=%s, recipient=%s, amount=%.2f, incentiveAmount=%.2f, successful=%b]",
                id, sender.getName(), recipient.getName(), amount, incentiveAmount, successful);
    }
}
