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

    public static void main(String[] args) {
        Properties properties = new Properties();
        properties.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, "localhost:9092");
        properties.put(ConsumerConfig.GROUP_ID_CONFIG, "ticker-consumer-group");
        properties.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class.getName());
        properties.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, KafkaAvroDeserializer.class.getName());
        properties.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
        properties.put("schema.registry.url", "http://localhost:8081");
        properties.put("specific.avro.reader", "true");

        Consumer<String, TickerMessage> consumer = new KafkaConsumer<>(properties);
        consumer.subscribe(Collections.singletonList("ticker"));

        System.out.println("Starting ticker consumer...");

        try {
            while (true) {
                ConsumerRecords<String, TickerMessage> records = consumer.poll(Duration.ofMillis(POLL_TIMEOUT_MS));

                for (ConsumerRecord<String, TickerMessage> record : records) {
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
        } catch (Exception e) {
            System.err.printf("Error consuming messages: %s%n", e.getMessage());
            e.printStackTrace();
        } finally {
            consumer.close();
        }
    }
}
