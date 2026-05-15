package com.paicbd.smsc.ws;

import com.paicbd.smsc.dto.UtilsRecords;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaders;
import org.springframework.messaging.simp.stomp.StompSession;

import java.lang.reflect.Type;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class SocketClientTest {

    @Mock
    FrameHandler frameHandler;

    @Mock
    SocketSession socketSession;

    @Mock
    StompSession stompSession;

    UtilsRecords.WebSocketConnectionParams webSocketConnectionParams;
    SocketClient socketClient;

    @BeforeEach
    void setUp() {
        webSocketConnectionParams = new UtilsRecords.WebSocketConnectionParams(
                true,
                "localhost",
                5082,
                "/ws",
                List.of("topic01", "topic02"),
                "Authorization",
                "1234567890",
                10000,
                "SMSC-UTILS-TEST"
        );

        socketClient = new SocketClient(frameHandler, webSocketConnectionParams, socketSession);
    }

    @Test
    void afterConnectedTest() {
        StompHeaders headers = new StompHeaders();
        headers.setDestination("topic01");

        // Execute method and verify successful subscription behavior
        socketClient.afterConnected(stompSession, headers);

        // Verify stompSession.subscribe was called for each topic (2 topics in test setup)
        verify(stompSession, times(2)).subscribe(anyString(), any());

        // Verify session confirmation message was sent
        verify(stompSession).send(eq("/app/session-confirm"), anyString());

        assertThrows(Exception.class, () -> socketClient.afterConnected(stompSession, null));
    }

    @Test
    void handleExceptionTest() {
        StompHeaders headers = new StompHeaders();
        headers.setDestination("topic01");
        StompCommand command = StompCommand.CONNECTED;
        byte[] payload = "Test Payload".getBytes();
        Throwable exception = new Exception("Test Exception");

        // Execute exception handling and verify it processes without throwing
        socketClient.handleException(stompSession, command, headers, payload, exception);

        // Verify exception handling behavior completed successfully
        // Method should handle exception gracefully and continue processing

        assertThrows(Exception.class, () -> socketClient.handleException(null, command, headers, payload, exception));
        assertThrows(Exception.class, () -> socketClient.handleException(stompSession, command, null, payload, exception));
        assertThrows(Exception.class, () -> socketClient.handleException(stompSession, command, headers, payload, null));
    }

    @Test
    void getPayloadTypeTest() {
        StompHeaders headers = new StompHeaders();
        headers.setDestination("topic01");

        // Execute method and verify it returns expected payload type
        Type payloadType = socketClient.getPayloadType(headers);

        // Verify method returns String.class as the payload type
        assertEquals(String.class, payloadType);
    }

    @Test
    void handleFrameTest() {
        StompHeaders headers = new StompHeaders();
        headers.setDestination("topic01");

        // Execute frame handling and verify processing completes
        socketClient.handleFrame(headers, "Test Payload");
        socketClient.handleFrameLogic(headers, "Test Payload");

        // Verify frame processing completed successfully
        // Methods should process frame data without exceptions

        assertThrows(Exception.class, () -> socketClient.handleFrame(null, "all"));
    }

    @Test
    void handleTransportErrorTest() {
        Exception testException = new Exception("Test Exception");

        // Execute transport error handling
        socketClient.handleTransportError(stompSession, testException);

        // Verify error handling processes exception appropriately
        // Method should handle transport errors gracefully

        assertThrows(Exception.class, () -> socketClient.handleTransportError(null, testException));
        assertThrows(Exception.class, () -> socketClient.handleTransportError(stompSession, null));
    }
}