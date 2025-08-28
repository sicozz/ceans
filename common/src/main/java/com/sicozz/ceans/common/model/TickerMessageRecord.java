/*
 * Copyright (c) 2025 Example Company Inc. All rights reserved.
 */
package com.sicozz.ceans.common.model;

public record TickerMessageRecord(
        String type,
        long sequence,
        String product_id,
        String price,
        String open_24h,
        String volume_24h,
        String low_24h,
        String high_24h,
        String volume_30d,
        String best_bid,
        String best_bid_size,
        String best_ask,
        String best_ask_size,
        String side,
        String time,
        long trade_id,
        String last_size) {}
