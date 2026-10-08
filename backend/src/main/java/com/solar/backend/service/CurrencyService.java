package com.solar.backend.service;

import com.solar.backend.model.Currency;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;


@Service
public class CurrencyService {

    @Value("${currency.rate.eur-per-usd:0.86}")
    private double eurPerUsd;

    @Value("${currency.rate.ron-per-usd:4.52}")
    private double ronPerUsd;

    public double fromUsd(double usdAmount, Currency currency) {
        return switch (currency) {
            case USD -> usdAmount;
            case EUR -> usdAmount * eurPerUsd;
            case RON -> usdAmount * ronPerUsd;
        };
    }
}
