package com.paicbd.smsc.kafka;

import com.paicbd.smsc.dto.MessageEvent;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class KafkaConsumerHandlerTest {

    private KafkaConsumerHandler handler;

    @BeforeEach
    void setUp() {
        handler = new KafkaConsumerHandler();
    }

    @Test
    void partitionListShouldSplitListCorrectly() {
        List<MessageEvent> events = new ArrayList<>();
        for (int i = 0; i < 10; i++) {
            events.add(MessageEvent.builder().id("msg-" + i).build());
        }
        List<List<MessageEvent>> parts = handler.partitionList(events, 3);
        assertEquals(4, parts.size());
    }

    @Test
    void batchPerWorkerShouldDistributeProperly() {
        assertEquals(5, handler.batchPerWorker(101, 20, 100));
        assertEquals(1, handler.batchPerWorker(0, 5, 100));
        assertEquals(10, handler.batchPerWorker(10, 0, 10));
        assertEquals(3, handler.batchPerWorker(10, 3, 10));
    }

    @Test
    void fetchDataInRawShouldDrainQueuesRespectingQuotas() {
        int maxPollRecords = 10;
        BlockingQueue<String> queue = new ArrayBlockingQueue<>(20);
        String messageInRaw = MessageEvent.builder()
                .id(System.currentTimeMillis() + "-" + System.nanoTime())
                .messageId(System.currentTimeMillis() + "-" + System.nanoTime())
                .parentId("parent-123")
                .systemId("smpp_sp")
                .sourceAddr("8888")
                .destinationAddr("1234")
                .shortMessage("Hello!")
                .originNetworkId(3)
                .originNetworkType("SP")
                .originProtocol("SMPP")
                .destNetworkType("GW")
                .destProtocol("SS7")
                .destNetworkId(5)
                .sourceAddrTon(1)
                .sourceAddrNpi(4)
                .destAddrTon(1)
                .destAddrNpi(4)
                .dataCoding(0)
                .registeredDelivery(1)
                .build().toString();

        for (int i = 0; i < 20; i++) {
            queue.add(messageInRaw);
        }


        KafkaConsumerCustomImpl kafkaConsumerCustom = mock(KafkaConsumerCustomImpl.class);
        when(kafkaConsumerCustom.getBufferQueue()).thenReturn(queue);

        var result = handler.fetchDataInRaw(maxPollRecords, kafkaConsumerCustom);
        assertNotNull(result);
        assertEquals(10, kafkaConsumerCustom.getBufferQueue().size());
    }



    @Test
    void fetchMessagesShouldDrainQueuesRespectingQuotas() {
        int highQuota = 5;
        int medQuota = 3;
        int lowQuota = 2;

        int maxPollRecords = 10;

        BlockingQueue<String> high = new ArrayBlockingQueue<>(10);
        BlockingQueue<String> med = new ArrayBlockingQueue<>(10);
        BlockingQueue<String> low = new ArrayBlockingQueue<>(10);

        String messageInRaw = MessageEvent.builder()
                .id(System.currentTimeMillis() + "-" + System.nanoTime())
                .messageId(System.currentTimeMillis() + "-" + System.nanoTime())
                .parentId("parent-123")
                .systemId("smpp_sp")
                .sourceAddr("8888")
                .destinationAddr("1234")
                .shortMessage("Hello!")
                .originNetworkId(3)
                .originNetworkType("SP")
                .originProtocol("SMPP")
                .destNetworkType("GW")
                .destProtocol("SS7")
                .destNetworkId(5)
                .sourceAddrTon(1)
                .sourceAddrNpi(4)
                .destAddrTon(1)
                .destAddrNpi(4)
                .dataCoding(0)
                .registeredDelivery(1)
                .build().toString();


        for (int i = 0; i < 10; i++) {
            high.add(messageInRaw);
            med.add(messageInRaw);
            low.add(messageInRaw);
        }


        KafkaConsumerCustomImpl highConsumer = mock(KafkaConsumerCustomImpl.class);
        KafkaConsumerCustomImpl medConsumer = mock(KafkaConsumerCustomImpl.class);
        KafkaConsumerCustomImpl lowConsumer = mock(KafkaConsumerCustomImpl.class);

        when(highConsumer.getBufferQueue()).thenReturn(high);
        when(medConsumer.getBufferQueue()).thenReturn(med);
        when(lowConsumer.getBufferQueue()).thenReturn(low);


        var result = handler.fetchMessages(maxPollRecords, highQuota, medQuota, lowQuota,
                highConsumer, medConsumer, lowConsumer);

        assertNotNull(result);
        assertEquals(5, highConsumer.getBufferQueue().size());
        assertEquals(7, medConsumer.getBufferQueue().size());
        assertEquals(8, lowConsumer.getBufferQueue().size());
    }


    @Test
    void fetchMessagesShouldFillRemainingFromHigherQueues() {
        int highQuota = 5;
        int lowQuota = 2;

        int maxPollRecords = 10;

        BlockingQueue<String> high = new ArrayBlockingQueue<>(10);
        BlockingQueue<String> low = new ArrayBlockingQueue<>(10);

        String messageInRaw = MessageEvent.builder()
                .id(System.currentTimeMillis() + "-" + System.nanoTime())
                .messageId(System.currentTimeMillis() + "-" + System.nanoTime())
                .parentId("parent-123")
                .systemId("smpp_sp")
                .sourceAddr("8888")
                .destinationAddr("1234")
                .shortMessage("Hello!")
                .originNetworkId(3)
                .originNetworkType("SP")
                .originProtocol("SMPP")
                .destNetworkType("GW")
                .destProtocol("SS7")
                .destNetworkId(5)
                .sourceAddrTon(1)
                .sourceAddrNpi(4)
                .destAddrTon(1)
                .destAddrNpi(4)
                .dataCoding(0)
                .registeredDelivery(1)
                .build().toString();


        for (int i = 0; i < 10; i++) {
            high.add(messageInRaw);
        }

        for (int i = 0; i < 10; i++) {
            low.add(messageInRaw);
        }


        KafkaConsumerCustomImpl highConsumer = mock(KafkaConsumerCustomImpl.class);
        KafkaConsumerCustomImpl lowConsumer = mock(KafkaConsumerCustomImpl.class);

        when(highConsumer.getBufferQueue()).thenReturn(high);
        when(lowConsumer.getBufferQueue()).thenReturn(low);


        var result = handler.fetchMessages(maxPollRecords, highQuota, 0, lowQuota,
                highConsumer, null, lowConsumer);

        assertNotNull(result);
        assertEquals(2, highConsumer.getBufferQueue().size());
        assertEquals(8, lowConsumer.getBufferQueue().size());
    }


    @Test
    void fetchMessagesShouldFillRemainingFromAllQueuesInSecondRound() {
        int highQuota = 5;
        int medQuota = 3;
        int lowQuota = 2;

        int maxPollRecords = 10;

        BlockingQueue<String> high = new ArrayBlockingQueue<>(10);
        BlockingQueue<String> med = new ArrayBlockingQueue<>(10);
        BlockingQueue<String> low = new ArrayBlockingQueue<>(10);

        String messageInRaw = MessageEvent.builder()
                .id(System.currentTimeMillis() + "-" + System.nanoTime())
                .messageId(System.currentTimeMillis() + "-" + System.nanoTime())
                .parentId("parent-123")
                .systemId("smpp_sp")
                .sourceAddr("8888")
                .destinationAddr("1234")
                .shortMessage("Hello!")
                .originNetworkId(3)
                .originNetworkType("SP")
                .originProtocol("SMPP")
                .destNetworkType("GW")
                .destProtocol("SS7")
                .destNetworkId(5)
                .sourceAddrTon(1)
                .sourceAddrNpi(4)
                .destAddrTon(1)
                .destAddrNpi(4)
                .dataCoding(0)
                .registeredDelivery(1)
                .build().toString();


        for (int i = 0; i < 1; i++) {
            high.add(messageInRaw);
            med.add(messageInRaw);
            low.add(messageInRaw);
        }


        KafkaConsumerCustomImpl highConsumer = mock(KafkaConsumerCustomImpl.class);
        KafkaConsumerCustomImpl medConsumer = mock(KafkaConsumerCustomImpl.class);
        KafkaConsumerCustomImpl lowConsumer = mock(KafkaConsumerCustomImpl.class);

        when(highConsumer.getBufferQueue()).thenReturn(high);
        when(medConsumer.getBufferQueue()).thenReturn(med);
        when(lowConsumer.getBufferQueue()).thenReturn(low);


        var result = handler.fetchMessages(maxPollRecords, highQuota, medQuota, lowQuota,
                highConsumer, medConsumer, lowConsumer);

        assertNotNull(result);
        assertEquals(0, highConsumer.getBufferQueue().size());
        assertEquals(0, medConsumer.getBufferQueue().size());
        assertEquals(0, lowConsumer.getBufferQueue().size());
    }
}