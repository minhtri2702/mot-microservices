package com.mot.productservices.dto;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class DebeziumEventTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void readsChapterIdFromCreateEvent() throws Exception {
        DebeziumEvent event = objectMapper.readValue("""
                {"payload":{"before":null,"after":{"id":140073,"manga_id":"ad568103-e4c9-4a3b-b664-8b8a8ec018ee"},"op":"c"}}
                """, DebeziumEvent.class);

        assertThat(event.getChapterRecordId()).isEqualTo(140073);
        assertThat(event.getOperation()).isEqualTo("CREATE");
    }

    @Test
    void readsChapterIdFromDeleteBeforeImage() throws Exception {
        DebeziumEvent event = objectMapper.readValue("""
                {"payload":{"before":{"id":140073,"manga_id":"ad568103-e4c9-4a3b-b664-8b8a8ec018ee"},"after":null,"op":"d"}}
                """, DebeziumEvent.class);

        assertThat(event.getChapterRecordId()).isEqualTo(140073);
        assertThat(event.getOperation()).isEqualTo("DELETE");
    }
}
