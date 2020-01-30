package lol.gilliard.websocketstranscription;

import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.AbstractWebSocketHandler;

import java.util.HashMap;
import java.util.Map;

public class TwilioMediaStreamsHandler extends AbstractWebSocketHandler {

    private Map<WebSocketSession, GoogleTextToSpeechService> sessions = new HashMap<>();

    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        sessions.put(session, new GoogleTextToSpeechService(
            transcription -> {
                System.out.println("Transcription: " + transcription);
            }
        ));
    }

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) {
        sessions.get(session).send(message.getPayload());
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) throws Exception {
        sessions.get(session).close();
        sessions.remove(session);
    }
}
