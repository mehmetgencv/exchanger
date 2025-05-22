package com.exchanger.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Objects;
import java.util.UUID;

@Entity
public class CurrencyConversion {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private UUID id;

    private String sourceCurrency;
    private String targetCurrency;

    private BigDecimal sourceAmount;
    private BigDecimal convertedAmount;
    private BigDecimal exchangeRate;

    private LocalDateTime transactionDate;

    public CurrencyConversion() {
        this.transactionDate = LocalDateTime.now();
    }

    public CurrencyConversion(String sourceCurrency, String targetCurrency,
                              BigDecimal sourceAmount, BigDecimal convertedAmount,
                              BigDecimal exchangeRate) {
        this.sourceCurrency = sourceCurrency;
        this.targetCurrency = targetCurrency;
        this.sourceAmount = sourceAmount;
        this.convertedAmount = convertedAmount;
        this.exchangeRate = exchangeRate;
        this.transactionDate = LocalDateTime.now();
    }

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public String getSourceCurrency() {
        return sourceCurrency;
    }

    public void setSourceCurrency(String sourceCurrency) {
        this.sourceCurrency = sourceCurrency;
    }

    public String getTargetCurrency() {
        return targetCurrency;
    }

    public void setTargetCurrency(String targetCurrency) {
        this.targetCurrency = targetCurrency;
    }

    public BigDecimal getSourceAmount() {
        return sourceAmount;
    }

    public void setSourceAmount(BigDecimal sourceAmount) {
        this.sourceAmount = sourceAmount;
    }

    public BigDecimal getConvertedAmount() {
        return convertedAmount;
    }

    public void setConvertedAmount(BigDecimal convertedAmount) {
        this.convertedAmount = convertedAmount;
    }

    public BigDecimal getExchangeRate() {
        return exchangeRate;
    }

    public void setExchangeRate(BigDecimal exchangeRate) {
        this.exchangeRate = exchangeRate;
    }

    public LocalDateTime getTransactionDate() {
        return transactionDate;
    }

    public void setTransactionDate(LocalDateTime transactionDate) {
        this.transactionDate = transactionDate;
    }

    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass()) return false;
        CurrencyConversion that = (CurrencyConversion) o;
        return Objects.equals(id, that.id) && Objects.equals(sourceCurrency, that.sourceCurrency) && Objects.equals(targetCurrency, that.targetCurrency) && Objects.equals(sourceAmount, that.sourceAmount) && Objects.equals(convertedAmount, that.convertedAmount) && Objects.equals(exchangeRate, that.exchangeRate) && Objects.equals(transactionDate, that.transactionDate);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, sourceCurrency, targetCurrency, sourceAmount, convertedAmount, exchangeRate, transactionDate);
    }

    @Override
    public String toString() {
        return "CurrencyConversion{" +
                "id=" + id +
                ", sourceCurrency='" + sourceCurrency + '\'' +
                ", targetCurrency='" + targetCurrency + '\'' +
                ", sourceAmount=" + sourceAmount +
                ", convertedAmount=" + convertedAmount +
                ", exchangeRate=" + exchangeRate +
                ", transactionDate=" + transactionDate +
                '}';
    }
}
