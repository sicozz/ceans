/*
 * Copyright (c) 2025 Example Company Inc. All rights reserved.
 */
package datageneration;

import static java.lang.Math.abs;
import static java.lang.Math.exp;
import static java.lang.Math.max;
import static java.lang.Math.pow;
import static java.lang.Math.sqrt;

import java.time.Instant;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.Random;

import com.sicozz.ceans.common.model.TickerMessage;

public final class TickerGenerator {
    private static final double DAYS_PER_MONTH = 30;
    private static final double HALF = 0.5;
    private static final double TIME_DELTA = 1.0 / MarketHistory.COINBASE_TICKS_PER_DAY;
    private static final long START_SEQUENCE = 10000000000L;
    private static final long START_TRADE_ID = 100000000L;
    private static final double BASE_DAILY_VOLUME = 250000.00;
    private static final double BASE_VOLUME_MULTIPLIER = 1.0;
    private static final double BASE_VOLUME_VOLATILITY_SENSIBILITY = 1.8;
    private static final double SPREAD_BASIS_POINTS = 2.0;
    private static final double MIN_TRADE_SIZE = 0.001;
    private static final double CRYPTO_MARKET_SHAPE = 1.5;

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
            double size = lastSize * generateLastSize();
            history.record(price, size);
            lastPrice = price;
            lastSize = size;
        }
    }

    public TickerMessage next() {
        final double priceMovement = generatePriceMovement();
        final double price = history.getLastPrice() * priceMovement;
        final double size = generateLastSize();
        final Instant now = Instant.now();
        final ZonedDateTime time = now.atZone(ZoneOffset.UTC);
        final double volume24h = generateVolume24h(time.getHour(), time.getMinute(), priceMovement);
        history.record(price, size);
        final double baseSpread = (SPREAD_BASIS_POINTS / 1000) * price;
        final double halfSpread = baseSpread * exp(random.nextGaussian(0, 0.3)) * HALF;
        final double bestBid = price - halfSpread;
        final double bestAsk = price + halfSpread;
        final double bestBidSize = generateLastSize();
        final double bestAskSize = generateLastSize();
        final String side = random.nextDouble() < HALF ? "buy" : "sell";

        final TickerMessage message = TickerMessage.newBuilder()
                .setType("ticker")
                .setSequence(sequence)
                .setProductId("BTC-USD")
                .setPrice(String.format("%.2f", price))
                .setOpen24h(String.format("%.2f", history.open24h()))
                .setVolume24h(String.format("%.2f", volume24h))
                .setLow24h(String.format("%.2f", history.low24h()))
                .setHigh24h(String.format("%.2f", history.high24h()))
                .setVolume30d(String.format("%.2f", generateVolume30d(volume24h)))
                .setBestBid(String.format("%.2f", bestBid))
                .setBestBidSize(String.format("%.8f", bestBidSize))
                .setBestAsk(String.format("%.2f", bestAsk))
                .setBestAskSize(String.format("%.8f", bestAskSize))
                .setSide(side)
                .setTime(now.toString())
                .setTradeId(tradeId)
                .setLastSize(String.format("%.8f", generateLastSize()))
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

    private double generateLastSize() {
        // F(x) = (Pareto Cumulative Distribution Function)^-1(x)
        double massProbability = random.nextDouble();
        while (massProbability == 0.0) {
            massProbability = random.nextDouble();
        }
        return MIN_TRADE_SIZE * pow((1 - massProbability), (-1 / CRYPTO_MARKET_SHAPE));
    }

    private double generateVolume24h(double hour, double minute, double priceChange) {
        final double fractionalTime = hour + minute / 60;
        final double normalizedTime = (fractionalTime - 12) / 12;
        final double intraDayPattern =
                1 + 0.5 * pow(normalizedTime, 2); // Highest at opening and closing. Lowest at noon
        final double volatilityMultiplier =
                BASE_VOLUME_MULTIPLIER + BASE_VOLUME_VOLATILITY_SENSIBILITY + pow(abs(priceChange), 0.8);
        final double capedVolatilityMultiplier = max(volatilityMultiplier, 5.0);
        final double randomFactor = (1 - volatility) + volatility * 2 * random.nextDouble();
        return BASE_DAILY_VOLUME * intraDayPattern * capedVolatilityMultiplier * randomFactor;
    }

    private double generateVolume30d(double todaysVolume) {
        final double randomFactor = (1 - volatility * HALF) + volatility * HALF * random.nextDouble();
        return todaysVolume * DAYS_PER_MONTH * randomFactor;
    }

    // ODO: Documentation on Initialize and then always pop before insert
    private final class MarketHistory {
        private static final int COINBASE_TICKS_PER_DAY = 17280; // Every 5000ms (5s) = 24*60*60 / 5
        private final double[] priceHistory;
        private double lastPrice;
        private int tickNumber;

        private MarketHistory() {
            priceHistory = new double[COINBASE_TICKS_PER_DAY];
            lastPrice = 0;
            tickNumber = 0;
        }

        private void record(double price, double size) {
            lastPrice = price;
            priceHistory[tickNumber] = price;
            tickNumber = (tickNumber + 1) % COINBASE_TICKS_PER_DAY;
        }

        private double getLastPrice() {
            return lastPrice;
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
