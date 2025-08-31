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
                    TickerMessage tickerMessage = record.value();
                    System.out.printf(
                            "Received ticker message: Product=%s, Price=%s, Sequence=%d%n",
                            tickerMessage.getProductId(), tickerMessage.getPrice(), tickerMessage.getSequence());
                    System.out.printf(
                            "  Type: %s, Trade ID: %d, Side: %s%n",
                            tickerMessage.getType(), tickerMessage.getTradeId(), tickerMessage.getSide());
                    System.out.printf(
                            "  Open 24h: %s, High 24h: %s, Low 24h: %s%n",
                            tickerMessage.getOpen24h(), tickerMessage.getHigh24h(), tickerMessage.getLow24h());
                    System.out.printf(
                            "  24h Volume: %s, Best Bid: %s, Best Ask: %s%n",
                            tickerMessage.getVolume24h(), tickerMessage.getBestBid(), tickerMessage.getBestAsk());
                    System.out.println("  ---");
                }
            }
        } catch (Exception e) {
            System.err.println("Error consuming messages: " + e.getMessage());
            e.printStackTrace();
        } finally {
            consumer.close();
        }
    }
}
