package com.exchanger.repository;

import com.exchanger.entity.CurrencyConversion;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.UUID;

public interface CurrencyConversionRepository extends JpaRepository<CurrencyConversion, UUID> {
    Page<CurrencyConversion> findAllByTransactionDateBetween(LocalDateTime start, LocalDateTime end, Pageable pageable);
}