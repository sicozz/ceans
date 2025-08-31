/*
 * Copyright (c) 2025 Example Company Inc. All rights reserved.
 */
package datageneration;

import static java.lang.Math.exp;
import static java.lang.Math.pow;
import static java.lang.Math.sqrt;

import java.time.Instant;
import java.util.Random;

import com.sicozz.ceans.common.model.TickerMessage;

public final class TickerGenerator {
    private static final double HALF = 0.5;
    private static final double TIME_DELTA = 1.0 / MarketHistory.COINBASE_TICKS_PER_DAY;
    private static final long START_SEQUENCE = 10000000000L;
    private static final long START_TRADE_ID = 100000000L;

    private final double drift;
    private final double volatility;
    private final MarketHistory history;
    private final Random random;
    private long sequence;
    private long tradeId;

    public TickerGenerator(final double initPrice, final double initSize, final double drift, final double volatility) {
        if (initPrice <= 0) {
            throw new IllegalArgumentException("Start price must be positive");
        }
        if (initSize <= 0) {
            throw new IllegalArgumentException("Start price must be positive");
        }
        if (!Double.isFinite(drift)) {
            throw new IllegalArgumentException("Drift must be finite");
        }
        if (volatility < 0 || !Double.isFinite(volatility)) {
            throw new IllegalArgumentException("Volatility must be non-negative and finite");
        }

        this.drift = drift;
        this.volatility = volatility;
        this.history = new MarketHistory();
        this.random = new Random();
        sequence = START_SEQUENCE;
        tradeId = START_TRADE_ID;
    }

    public void initializeHistory(double initPrice, double initSize) {
        double lastPrice = initPrice;
        double lastSize = initSize;
        for (int i = 0; i < MarketHistory.COINBASE_TICKS_PER_DAY; i++) {
            double price = lastPrice * generatePriceMovement();
            double size = lastSize * generateSizeMovement();
            history.record(price, size);
            lastPrice = price;
            lastSize = size;
        }
    }

    public TickerMessage next() {
        final double price = history.getlastPrice() * generatePriceMovement();
        final double size = generateSizeMovement();
        final Instant now = Instant.now();
        history.record(price, size);

        final TickerMessage message = TickerMessage.newBuilder()
                .setType("ticker")
                .setSequence(sequence)
                .setProductId("BTC-USD")
                .setPrice(String.format("%.2f", price))
                .setOpen24h(String.format("%.2f", history.open24h()))
                .setVolume24h(String.format("%.2f", history.getVolume24h()))
                .setLow24h(String.format("%.2f", history.low24h()))
                .setHigh24h(String.format("%.2f", history.high24h()))
                .setVolume30d("30000.75")
                .setBestBid("49999.99")
                .setBestBidSize("1.5")
                .setBestAsk("50000.01")
                .setBestAskSize("2.0")
                .setSide("buy")
                .setTime(now.toString())
                .setTradeId(tradeId)
                .setLastSize(String.format("%.2f", size))
                .build();

        sequence++;
        tradeId++;

        return message;
    }

    private double generatePriceMovement() {
        final double randomShock = random.nextGaussian();
        final double driftComponent = (drift - (pow(volatility, 2) * HALF)) * TIME_DELTA;
        final double diffusionComponent = volatility * sqrt(TIME_DELTA) * randomShock;
        final double movement = exp(driftComponent + diffusionComponent);
        return movement;
    }

    private double generateSizeMovement() {
        return 1.0;
    }

    // ODO: Documentation on Initialize and then always pop before insert
    private final class MarketHistory {
        private static final int COINBASE_TICKS_PER_DAY = 17280; // Every 5000ms (5s) = 24*60*60 / 5
        private final double[] priceHistory;
        private final double[] sizeHistory;
        private double lastPrice;
        private double volume24h;
        private int tickNumber;

        private MarketHistory() {
            priceHistory = new double[COINBASE_TICKS_PER_DAY];
            sizeHistory = new double[COINBASE_TICKS_PER_DAY];
            lastPrice = 0;
            volume24h = 0;
            tickNumber = 0;
        }

        private void record(double price, double size) {
            double yesterdaySize = sizeHistory[tickNumber];
            volume24h = volume24h - yesterdaySize + size;
            lastPrice = price;
            priceHistory[tickNumber] = price;
            sizeHistory[tickNumber] = size;
            tickNumber = (tickNumber + 1) % COINBASE_TICKS_PER_DAY;
        }

        private double getlastPrice() {
            return lastPrice;
        }

        private double getVolume24h() {
            return volume24h;
        }

        private double open24h() {
            return priceHistory[tickNumber];
        }

        private double high24h() {
            double max = 0;
            for (int i = 0; i < COINBASE_TICKS_PER_DAY; i++) {
                if (priceHistory[i] > max) {
                    max = priceHistory[i];
                }
            }
            return max;
        }

        private double low24h() {
            double min = 0;
            for (int i = 0; i < COINBASE_TICKS_PER_DAY; i++) {
                if (priceHistory[i] > min) {
                    min = priceHistory[i];
                }
            }
            return min;
        }
    }
}
