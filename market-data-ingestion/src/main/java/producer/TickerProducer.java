/*
 * Copyright (c) 2025 Example Company Inc. All rights reserved.
 */
package producer;

import java.util.Properties;

import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.Producer;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.serialization.StringSerializer;

import com.sicozz.ceans.common.model.TickerMessage;

import datageneration.TickerGenerator;
import io.confluent.kafka.serializers.KafkaAvroSerializer;

public class TickerProducer {
    private static final int MESSAGE_COUNT = 100;
    private static final double INIT_PRICE = 50000.00;
    private static final double INIT_SIZE = 1.00;
    private static final double DRIFT = 0.0001;
    private static final double VOLATILITY = 0.05;

    public static void main(String[] args) {
        Properties properties = new Properties();
        properties.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, "localhost:9092");
        properties.put(ProducerConfig.LINGER_MS_CONFIG, 2);
        properties.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class.getName());
        properties.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, KafkaAvroSerializer.class.getName());
        properties.put("schema.registry.url", "http://localhost:8081");

        Producer<String, TickerMessage> producer = new KafkaProducer<>(properties);

        TickerGenerator generator = new TickerGenerator(INIT_PRICE, INIT_SIZE, DRIFT, VOLATILITY);
        generator.initializeHistory(INIT_PRICE, INIT_SIZE);

        for (int i = 0; i < MESSAGE_COUNT; i++) {
            TickerMessage tickerMessage = generator.next();

            System.out.printf("Sending ticker message %d: %s%n", i, tickerMessage.getProductId());
            ProducerRecord<String, TickerMessage> record =
                    new ProducerRecord<>("ticker", tickerMessage.getProductId(), tickerMessage);
            producer.send(record);
        }

        producer.close();
    }
}
