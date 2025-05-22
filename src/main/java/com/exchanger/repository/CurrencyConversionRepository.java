package com.exchanger.repository;

import com.exchanger.entity.CurrencyConversion;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface CurrencyConversionRepository extends JpaRepository<CurrencyConversion, UUID> {

}