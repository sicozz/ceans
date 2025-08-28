/*
 * Copyright (c) 2025 Example Company Inc. All rights reserved.
 */
package datageneration;

import com.sicozz.ceans.common.model.TickerMessage;

public class TickerGenerator {
    private static final int SECONDS_A_DAY = 86400;
    private static final float DEV_ONLY_PRICE = 50000.00F;

    private long baseTradeId;
    private int tickNumber;
    private float[] dayPriceHistory = new float[SECONDS_A_DAY];

    public TickerGenerator(long baseTradeId, int tickNumber) {
        this.baseTradeId = baseTradeId;
        this.tickNumber = tickNumber - 1;
    }

    public TickerMessage next() {
        return TickerMessage.newBuilder()
                .setType("ticker")
                .setSequence(generatSequence())
                .setProductId("BTC-USD")
                .setPrice(generatePrice())
                .setOpen24h("49500.00")
                .setVolume24h("1000.50")
                .setLow24h("49000.00")
                .setHigh24h("51000.00")
                .setVolume30d("30000.75")
                .setBestBid("49999.99")
                .setBestBidSize("1.5")
                .setBestAsk("50000.01")
                .setBestAskSize("2.0")
                .setSide("buy")
                .setTime("2025-01-01T12:00:00Z")
                .setTradeId(baseTradeId + tickNumber)
                .setLastSize("0.1")
                .build();
    }

    private String generatePrice() {
        dayPriceHistory[tickNumber] = DEV_ONLY_PRICE;
        return Float.toString(DEV_ONLY_PRICE);
    }

    private long generatSequence() {
        tickNumber = (tickNumber + 1) % (SECONDS_A_DAY - 1);
        return tickNumber;
    }
}
