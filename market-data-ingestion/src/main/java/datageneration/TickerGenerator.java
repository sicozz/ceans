/*
 * Copyright (c) 2025 Example Company Inc. All rights reserved.
 */
package datageneration;

import static java.lang.Math.abs;
import static java.lang.Math.exp;
import static java.lang.Math.min;
import static java.lang.Math.pow;
import static java.lang.Math.sqrt;

import java.time.Instant;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.Random;

import com.sicozz.ceans.common.model.TickerMessage;

public final class TickerGenerator {

    public record MarketDynamics(double drift, double volatility) {}

    public record VolumeSettings(
            double baseDailyVolume, double baseVolumeMultiplier, double baseVolumeVolatilitySensitivity) {}

    public record TradeSettings(
            double spreadBasisPoints, double minimumTradeSize, double cryptoMarketShape, double timeDelta) {}

    public record SequenceSettings(long initialSequence, long initialTradeId) {}

    public static class Builder {
        private Double initialPrice;
        private Double initialSize;
        private MarketDynamics marketDynamics;
        private VolumeSettings volumeSettings;
        private TradeSettings tradeSettings;
        private SequenceSettings sequenceSettings;

        public Builder withInitialValues(double price, double size) {
            this.initialPrice = price;
            this.initialSize = size;
            return this;
        }

        public Builder withMarketDynamics(double marketDrift, double marketVolatility) {
            this.marketDynamics = new MarketDynamics(marketDrift, marketVolatility);
            return this;
        }

        public Builder withVolumeSettings(
                double dailyVolume, double volumeMultiplier, double volumeVolatilitySensitivity) {
            this.volumeSettings = new VolumeSettings(dailyVolume, volumeMultiplier, volumeVolatilitySensitivity);
            return this;
        }

        public Builder withTradeSettings(
                double spreadPoints, double minTradeSize, double marketShape, double deltaTime) {
            this.tradeSettings = new TradeSettings(spreadPoints, minTradeSize, marketShape, deltaTime);
            return this;
        }

        public Builder withSequenceSettings(long initialSequence, long initialTradeId) {
            this.sequenceSettings = new SequenceSettings(initialSequence, initialTradeId);
            return this;
        }

        public TickerGenerator build() {
            if (initialPrice == null
                    || initialSize == null
                    || marketDynamics == null
                    || volumeSettings == null
                    || tradeSettings == null
                    || sequenceSettings == null) {
                throw new IllegalStateException("All parameters must be set before building TickerGenerator");
            }

            return new TickerGenerator(
                    initialPrice, initialSize, marketDynamics, volumeSettings, tradeSettings, sequenceSettings);
        }
    }

    public static Builder builder() {
        return new Builder();
    }

    private static final double HALF = 0.5;
    private static final double DAYS_PER_MONTH = 30.0;
    private static final String TICKER_TYPE = "ticker";
    private static final String PRODUCT_ID = "BTC-USD";
    private static final String[] TRADE_SIDES = {"buy", "sell"};
    private static final double PRICE_FORMAT_PRECISION = 2;
    private static final double SIZE_FORMAT_PRECISION = 8;
    private static final double SPREAD_VOLATILITY = 0.3;
    private static final double MAX_VOLUME_MULTIPLIER = 5.0;

    private final double baseDailyVolume;
    private final double baseVolumeMultiplier;
    private final double baseVolumeVolatilitySensitivity;
    private final double spreadBasisPoints;
    private final double minimumTradeSize;
    private final double cryptoMarketShape;
    private final double timeDelta;
    private final double drift;
    private final double volatility;
    private final MarketHistory history;
    private final Random random;

    private long sequence;
    private long tradeId;

    TickerGenerator(
            final double initialPrice,
            final double initialSize,
            final MarketDynamics marketDynamics,
            final VolumeSettings volumeSettings,
            final TradeSettings tradeSettings,
            final SequenceSettings sequenceSettings) {
        if (initialPrice <= 0) {
            throw new IllegalArgumentException("Start price must be positive");
        }
        if (initialSize <= 0) {
            throw new IllegalArgumentException("Start price must be positive");
        }
        if (!Double.isFinite(marketDynamics.drift())) {
            throw new IllegalArgumentException("Drift must be finite");
        }
        if (marketDynamics.volatility() < 0 || !Double.isFinite(marketDynamics.volatility())) {
            throw new IllegalArgumentException("Volatility must be non-negative and finite");
        }

        this.drift = marketDynamics.drift();
        this.volatility = marketDynamics.volatility();
        this.baseDailyVolume = volumeSettings.baseDailyVolume();
        this.baseVolumeMultiplier = volumeSettings.baseVolumeMultiplier();
        this.baseVolumeVolatilitySensitivity = volumeSettings.baseVolumeVolatilitySensitivity();
        this.spreadBasisPoints = tradeSettings.spreadBasisPoints();
        this.minimumTradeSize = tradeSettings.minimumTradeSize();
        this.cryptoMarketShape = tradeSettings.cryptoMarketShape();
        this.timeDelta = tradeSettings.timeDelta();
        this.sequence = sequenceSettings.initialSequence();
        this.tradeId = sequenceSettings.initialTradeId();
        this.history = new MarketHistory();
        this.random = new Random();
    }

    public void initializeHistory(double initialPrice, double initialSize) {
        double lastPrice = initialPrice;
        double lastSize = initialSize;
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
        final double baseSpread = (spreadBasisPoints / 1000) * price;
        final double halfSpread = baseSpread * exp(random.nextGaussian(0, SPREAD_VOLATILITY)) * HALF;
        final double bestBid = price - halfSpread;
        final double bestAsk = price + halfSpread;
        final double bestBidSize = generateLastSize();
        final double bestAskSize = generateLastSize();
        final String side = TRADE_SIDES[random.nextInt(TRADE_SIDES.length)];

        final TickerMessage message = TickerMessage.newBuilder()
                .setType(TICKER_TYPE)
                .setSequence(sequence)
                .setProductId(PRODUCT_ID)
                .setPrice(formatPrice(price))
                .setOpen24h(formatPrice(history.open24h()))
                .setVolume24h(formatPrice(volume24h))
                .setLow24h(formatPrice(history.low24h()))
                .setHigh24h(formatPrice(history.high24h()))
                .setVolume30d(formatPrice(generateVolume30d(volume24h)))
                .setBestBid(formatPrice(bestBid))
                .setBestBidSize(formatSize(bestBidSize))
                .setBestAsk(formatPrice(bestAsk))
                .setBestAskSize(formatSize(bestAskSize))
                .setSide(side)
                .setTime(now.toString())
                .setTradeId(tradeId)
                .setLastSize(formatSize(size))
                .build();

        sequence++;
        tradeId++;

        return message;
    }

    private String formatPrice(double value) {
        return String.format("%." + (int) PRICE_FORMAT_PRECISION + "f", value);
    }

    private String formatSize(double value) {
        return String.format("%." + (int) SIZE_FORMAT_PRECISION + "f", value);
    }

    private double generatePriceMovement() {
        final double randomShock = random.nextGaussian();
        final double driftComponent = (drift - (pow(volatility, 2) * HALF)) * timeDelta;
        final double diffusionComponent = volatility * sqrt(timeDelta) * randomShock;
        final double movement = exp(driftComponent + diffusionComponent);
        return movement;
    }

    private double generateLastSize() {
        // F(x) = (Pareto Cumulative Distribution Function)^-1(x)
        double massProbability = random.nextDouble();
        while (massProbability == 0.0) {
            massProbability = random.nextDouble();
        }
        return minimumTradeSize * pow((1 - massProbability), (-1 / cryptoMarketShape));
    }

    private double generateVolume24h(double hour, double minute, double priceChange) {
        final double fractionalTime = hour + minute / 60;
        final double normalizedTime = (fractionalTime - 12) / 12;
        final double intraDayPattern =
                1 + 0.5 * pow(normalizedTime, 2); // Highest at opening and closing. Lowest at noon
        final double volatilityMultiplier =
                baseVolumeMultiplier + baseVolumeVolatilitySensitivity + pow(abs(priceChange), 0.8);
        final double capedVolatilityMultiplier = min(volatilityMultiplier, MAX_VOLUME_MULTIPLIER);
        final double randomFactor = (1 - volatility) + volatility * 2 * random.nextDouble();
        return baseDailyVolume * intraDayPattern * capedVolatilityMultiplier * randomFactor;
    }

    private double generateVolume30d(double todaysVolume) {
        final double randomFactor = (1 - volatility * HALF) + volatility * HALF * random.nextDouble();
        return todaysVolume * DAYS_PER_MONTH * randomFactor;
    }

    private static final class MarketHistory {
        private static final int COINBASE_TICKS_PER_DAY = 17280;

        private final double[] priceHistory = new double[COINBASE_TICKS_PER_DAY];
        private double lastPrice;
        private int tickNumber;

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
            return findMaxPrice();
        }

        private double low24h() {
            return findMinPrice();
        }

        private double findMaxPrice() {
            double max = 0;
            for (double price : priceHistory) {
                if (price > max) {
                    max = price;
                }
            }
            return max;
        }

        private double findMinPrice() {
            double min = Double.MAX_VALUE;
            for (double price : priceHistory) {
                if (price < min && price > 0) {
                    min = price;
                }
            }
            return min;
        }
    }
}
