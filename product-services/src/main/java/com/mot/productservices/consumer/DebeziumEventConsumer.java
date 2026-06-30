package com.mot.productservices.consumer;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mot.productservices.dto.DebeziumEvent;
import com.mot.productservices.service.DebeziumSyncService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Component;

/**
 * Consumer lắng nghe event từ Debezium CDC.
 * 
 * Debezium đọc WAL (Write-Ahead Log) của PostgreSQL (crawler_db)
 * và push mỗi thay đổi vào Kafka topic.
 * 
 * Consumer này sẽ:
 * 1. Nhận message từ Kafka
 * 2. Parse JSON thành DebeziumEvent
 * 3. Gọi DebeziumSyncService để đồng bộ dữ liệu vào mot_db + xoá cache
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class DebeziumEventConsumer {

    private final ObjectMapper objectMapper;
    private final DebeziumSyncService debeziumSyncService;

    /**
     * Lắng nghe topic dbserver1.public.manga
     * Khi có manga mới hoặc cập nhật từ crawl → đồng bộ vào mot_db
     * 
     * Lưu ý: Với DELETE events, Debezium gửi payload.after = null,
     * dẫn đến message có thể là null hoặc rỗng. Cần kiểm tra trước khi parse.
     */
    @KafkaListener(
        topics = "dbserver1.public.manga",
        groupId = "${spring.kafka.consumer.group-id}",
        containerFactory = "kafkaListenerContainerFactory"
    )
    public void consumeMangaEvent(@Payload(required = false) String message) {
        if (message == null || message.isBlank()) {
            log.warn("Received null/empty manga event (likely a DELETE tombstone), skipping");
            return;
        }
        try {
            DebeziumEvent event = objectMapper.readValue(message, DebeziumEvent.class);
            String operation = event.getOperation();
            String mangaId = event.getMangaId();

            log.info("Received manga event: op={}, mangaId={}", operation, mangaId);
            debeziumSyncService.processMangaEvent(event);
        } catch (Exception e) {
            log.error("Failed to process manga event: {}", e.getMessage());
        }
    }

    /**
     * Lắng nghe topic dbserver1.public.chapter
     * Khi chapter thay đổi → đồng bộ vào mot_db
     */
    @KafkaListener(
        topics = "dbserver1.public.chapter",
        groupId = "${spring.kafka.consumer.group-id}",
        containerFactory = "kafkaListenerContainerFactory"
    )
    public void consumeChapterEvent(@Payload(required = false) String message) {
        if (message == null || message.isBlank()) {
            log.warn("Received null/empty chapter event (tombstone), skipping");
            return;
        }
        try {
            DebeziumEvent event = objectMapper.readValue(message, DebeziumEvent.class);
            log.info("Received chapter event: op={}, mangaId={}",
                    event.getOperation(), event.getMangaIdFromChapter());
            debeziumSyncService.processChapterEvent(event);
        } catch (Exception e) {
            log.error("Failed to process chapter event: {}", e.getMessage());
        }
    }

    /**
     * Lắng nghe topic dbserver1.public.chapter_image
     * Khi chapter_image thay đổi → xoá cache
     */
    @KafkaListener(
        topics = "dbserver1.public.chapter_image",
        groupId = "${spring.kafka.consumer.group-id}",
        containerFactory = "kafkaListenerContainerFactory"
    )
    public void consumeChapterImageEvent(@Payload(required = false) String message) {
        if (message == null || message.isBlank()) {
            log.warn("Received null/empty chapter_image event (tombstone), skipping");
            return;
        }
        try {
            DebeziumEvent event = objectMapper.readValue(message, DebeziumEvent.class);
            log.info("Received chapter_image event: op={}, mangaId={}",
                    event.getOperation(), event.getMangaIdFromChapter());
            debeziumSyncService.processChapterImageEvent(event);
        } catch (Exception e) {
            log.error("Failed to process chapter_image event: {}", e.getMessage());
        }
    }

    /**
     * Lắng nghe topic dbserver1.public.manga_genre
     * Khi genre mapping thay đổi → xoá cache danh sách
     */
    @KafkaListener(
        topics = "dbserver1.public.manga_genre",
        groupId = "${spring.kafka.consumer.group-id}",
        containerFactory = "kafkaListenerContainerFactory"
    )
    public void consumeMangaGenreEvent(String message) {
        try {
            DebeziumEvent event = objectMapper.readValue(message, DebeziumEvent.class);
            log.info("Received manga_genre event: op={}", event.getOperation());
            debeziumSyncService.processMangaGenreEvent(event);
        } catch (Exception e) {
            log.error("Failed to process manga_genre event: {}", e.getMessage());
        }
    }
}
