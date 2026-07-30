package com.paicbd.smsc.ws;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.messaging.simp.stomp.StompSession;

import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class SocketSessionTest {

    @Mock
    StompSession stompSession;

    SocketSession socketSession;

    @BeforeEach
    void setUp() {
        socketSession = new SocketSession("gw");
    }

    @Test
    void sendStatusTest() {
        // Verify message sending when StompSession is available
        socketSession.setStompSession(stompSession);
        socketSession.sendStatus("systemId", "param", "value");

        // Verify stompSession.send was called with correct destination and message format
        verify(stompSession).send("/app/handler-status", "gw,systemId,param,value");

        // Verify no interaction when StompSession is null
        socketSession.setStompSession(null);
        socketSession.sendStatus("systemId", "param", "value");

        // Verify no additional calls to send method
        verify(stompSession).send("/app/handler-status", "gw,systemId,param,value");
    }
}