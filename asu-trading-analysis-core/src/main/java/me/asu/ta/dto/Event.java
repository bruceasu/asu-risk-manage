package me.asu.ta.dto;

public sealed interface Event permits QuoteEvent, OrderEvent {
    long ts();
}