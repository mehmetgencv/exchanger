package com.exchanger.mapper;

import com.exchanger.dto.requests.CurrencyConversionRequest;
import com.exchanger.entity.CurrencyConversion;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.factory.Mappers;

import java.math.BigDecimal;

@Mapper(componentModel = "spring")
public interface CurrencyConversionMapper {

    CurrencyConversionMapper INSTANCE = Mappers.getMapper(CurrencyConversionMapper.class);

    @Mapping(target = "convertedAmount", expression = "java(request.amount().multiply(rate))")
    @Mapping(target = "exchangeRate", source = "rate")
    @Mapping(target = "sourceAmount", source = "request.amount")
    CurrencyConversion toEntity(CurrencyConversionRequest request, BigDecimal rate);
}
