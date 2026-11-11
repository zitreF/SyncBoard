package me.fertiz.syncboard.board;

import me.fertiz.syncboard.board.dto.request.ClientMessage;
import me.fertiz.syncboard.board.dto.response.ServerMessage;
import org.springframework.stereotype.Component;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.ObjectReader;
import tools.jackson.databind.ObjectWriter;
import tools.jackson.databind.json.JsonMapper;

import java.util.Optional;

@Component
public class MessageCodec {

    private final ObjectReader reader;
    private final ObjectWriter writer;

    public MessageCodec(JsonMapper json) {
        this.reader = json.readerFor(ClientMessage.class);
        this.writer = json.writerFor(ServerMessage.class);
    }

    public Optional<ClientMessage> read(String payload) {
        try {
            return Optional.ofNullable(reader.readValue(payload));
        } catch (JacksonException e) {
            return Optional.empty();
        }
    }

    public String write(ServerMessage message) {
        return writer.writeValueAsString(message);
    }
}
