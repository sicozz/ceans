/*
 * Copyright (c) 2025 Example Company Inc. All rights reserved.
 */
package consumer;

import java.time.Duration;
import java.util.Collections;
import java.util.Properties;

import org.apache.kafka.clients.consumer.Consumer;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.common.serialization.StringDeserializer;

import com.sicozz.ceans.common.model.TickerMessage;

import io.confluent.kafka.serializers.KafkaAvroDeserializer;

public class TickerConsumer {
    private static final int POLL_TIMEOUT_MS = 1000;
    private static final String BOOTSTRAP_SERVERS = "localhost:9092";
    private static final String CONSUMER_GROUP = "ticker-consumer-group";
    private static final String SCHEMA_REGISTRY_URL = "http://localhost:8081";
    private static final String TICKER_TOPIC = "ticker";

    public static void main(String[] args) {
        Consumer<String, TickerMessage> consumer = createConsumer();
        consumer.subscribe(Collections.singletonList(TICKER_TOPIC));

        System.out.println("Starting ticker consumer...");
        consumeMessages(consumer);
    }

    private static Consumer<String, TickerMessage> createConsumer() {
        Properties properties = new Properties();
        properties.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, BOOTSTRAP_SERVERS);
        properties.put(ConsumerConfig.GROUP_ID_CONFIG, CONSUMER_GROUP);
        properties.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class.getName());
        properties.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, KafkaAvroDeserializer.class.getName());
        properties.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
        properties.put("schema.registry.url", SCHEMA_REGISTRY_URL);
        properties.put("specific.avro.reader", "true");

        return new KafkaConsumer<>(properties);
    }

    private static void consumeMessages(Consumer<String, TickerMessage> consumer) {
        try {
            while (true) {
                ConsumerRecords<String, TickerMessage> records = consumer.poll(Duration.ofMillis(POLL_TIMEOUT_MS));
                records.forEach(TickerConsumer::printTickerMessage);
            }
        } catch (Exception e) {
            System.err.printf("Error consuming messages: %s%n", e.getMessage());
            e.printStackTrace();
        } finally {
            consumer.close();
        }
    }

    private static void printTickerMessage(ConsumerRecord<String, TickerMessage> record) {
        TickerMessage msg = record.value();
        System.out.printf(
                "{product: %s, price: %s, seq: %d, type: %s, tradeId: %d, side: %s, "
                        + "open24h: %s, high24h: %s, low24h: %s, vol24h: %s, vol30d: %s, "
                        + "bestBid: %s, bestBidSize: %s, bestAsk: %s, bestAskSize: %s, "
                        + "time: %s, lastSize: %s}%n",
                msg.getProductId(),
                msg.getPrice(),
                msg.getSequence(),
                msg.getType(),
                msg.getTradeId(),
                msg.getSide(),
                msg.getOpen24h(),
                msg.getHigh24h(),
                msg.getLow24h(),
                msg.getVolume24h(),
                msg.getVolume30d(),
                msg.getBestBid(),
                msg.getBestBidSize(),
                msg.getBestAsk(),
                msg.getBestAskSize(),
                msg.getTime(),
                msg.getLastSize());
    }
}
