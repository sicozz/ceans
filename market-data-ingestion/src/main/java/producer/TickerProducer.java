/*
 * Copyright (c) 2025 Example Company Inc. All rights reserved.
 */
package producer;

import java.util.Properties;
import java.util.concurrent.CompletableFuture;

import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.Producer;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.serialization.StringSerializer;

import com.google.common.util.concurrent.RateLimiter;
import com.sicozz.ceans.common.model.TickerMessage;

import datageneration.TickerGenerator;
import io.confluent.kafka.serializers.KafkaAvroSerializer;

public class TickerProducer {
    private static final int MESSAGE_COUNT = 100;
    private static final int LINGER_MS = 2;
    private static final double PERMITS_PER_SECOND = 1.0 / 5.0;
    private static final String BOOTSTRAP_SERVERS = "localhost:9092";
    private static final String SCHEMA_REGISTRY_URL = "http://localhost:8081";
    private static final String TICKER_TOPIC = "ticker";

    private static final double INIT_PRICE = 50000.00;
    private static final double INIT_SIZE = 1.00;
    private static final double DRIFT = 0.0001;
    private static final double VOLATILITY = 0.05;
    private static final double BASE_DAILY_VOLUME = 250000.00;
    private static final double BASE_VOLUME_MULTIPLIER = 1.0;
    private static final double BASE_VOLUME_VOLATILITY_SENSITIVITY = 1.8;
    private static final double SPREAD_BASIS_POINTS = 2.0;
    private static final double MINIMUM_TRADE_SIZE = 0.001;
    private static final double CRYPTO_MARKET_SHAPE = 1.5;
    private static final int COINBASE_TICKS_PER_DAY = 17280;
    private static final double TIME_DELTA = 1.0 / COINBASE_TICKS_PER_DAY;
    private static final long START_SEQUENCE = 10000000000L;
    private static final long START_TRADE_ID = 100000000L;

    public static void main(String[] args) {
        Producer<String, TickerMessage> producer = createProducer();
        TickerGenerator generator = createGenerator();
        RateLimiter rateLimiter = RateLimiter.create(PERMITS_PER_SECOND);

        sendMessages(producer, generator, rateLimiter);

        producer.close();
    }

    private static Producer<String, TickerMessage> createProducer() {
        Properties properties = new Properties();
        properties.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, BOOTSTRAP_SERVERS);
        properties.put(ProducerConfig.LINGER_MS_CONFIG, LINGER_MS);
        properties.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class.getName());
        properties.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, KafkaAvroSerializer.class.getName());
        properties.put("schema.registry.url", SCHEMA_REGISTRY_URL);

        return new KafkaProducer<>(properties);
    }

    private static TickerGenerator createGenerator() {
        return TickerGenerator.builder()
                .withInitialValues(INIT_PRICE, INIT_SIZE)
                .withMarketDynamics(DRIFT, VOLATILITY)
                .withVolumeSettings(BASE_DAILY_VOLUME, BASE_VOLUME_MULTIPLIER, BASE_VOLUME_VOLATILITY_SENSITIVITY)
                .withTradeSettings(SPREAD_BASIS_POINTS, MINIMUM_TRADE_SIZE, CRYPTO_MARKET_SHAPE, TIME_DELTA)
                .withSequenceSettings(START_SEQUENCE, START_TRADE_ID)
                .build();
    }

    private static void sendMessages(
            Producer<String, TickerMessage> producer, TickerGenerator generator, RateLimiter rateLimiter) {
        for (int i = 0; i < MESSAGE_COUNT; i++) {
            rateLimiter.acquire();
            CompletableFuture.runAsync(() -> {
                TickerMessage message = generator.next();
                ProducerRecord<String, TickerMessage> record =
                        new ProducerRecord<>(TICKER_TOPIC, message.getProductId(), message);
                producer.send(record);
            });
        }
    }
}
