/*
 * Copyright (c) 2025 Example Company Inc. All rights reserved.
 */
package producer;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.ByteBuffer;

import org.junit.jupiter.api.Test;

import com.sicozz.ceans.common.model.TickerMessage;

class TickerMessageSerializationTest {
    private static final long TEST_SEQUENCE = 123L;
    private static final long TEST_TRADE_ID = 1001L;

    @Test
    void testTickerMessageSerialization() throws Exception {
        // Create a TickerMessage using the builder
        TickerMessage original = TickerMessage.newBuilder()
                .setType("ticker")
                .setSequence(TEST_SEQUENCE)
                .setProductId("BTC-USD")
                .setPrice("50000.00")
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
                .setTradeId(TEST_TRADE_ID)
                .setLastSize("0.1")
                .build();

        // Test serialization
        ByteBuffer serialized = original.toByteBuffer();
        assertThat(serialized).isNotNull();
        assertThat(serialized.hasRemaining()).isTrue();

        // Test deserialization
        TickerMessage deserialized = TickerMessage.fromByteBuffer(serialized);
        assertThat(deserialized).isNotNull();

        // Verify all fields are correctly deserialized
        assertThat(deserialized.getType()).isEqualTo("ticker");
        assertThat(deserialized.getSequence()).isEqualTo(TEST_SEQUENCE);
        assertThat(deserialized.getProductId()).isEqualTo("BTC-USD");
        assertThat(deserialized.getPrice()).isEqualTo("50000.00");
        assertThat(deserialized.getOpen24h()).isEqualTo("49500.00");
        assertThat(deserialized.getVolume24h()).isEqualTo("1000.50");
        assertThat(deserialized.getLow24h()).isEqualTo("49000.00");
        assertThat(deserialized.getHigh24h()).isEqualTo("51000.00");
        assertThat(deserialized.getVolume30d()).isEqualTo("30000.75");
        assertThat(deserialized.getBestBid()).isEqualTo("49999.99");
        assertThat(deserialized.getBestBidSize()).isEqualTo("1.5");
        assertThat(deserialized.getBestAsk()).isEqualTo("50000.01");
        assertThat(deserialized.getBestAskSize()).isEqualTo("2.0");
        assertThat(deserialized.getSide()).isEqualTo("buy");
        assertThat(deserialized.getTime()).isEqualTo("2025-01-01T12:00:00Z");
        assertThat(deserialized.getTradeId()).isEqualTo(TEST_TRADE_ID);
        assertThat(deserialized.getLastSize()).isEqualTo("0.1");

        // Test equality
        assertThat(deserialized).isEqualTo(original);
    }
}
