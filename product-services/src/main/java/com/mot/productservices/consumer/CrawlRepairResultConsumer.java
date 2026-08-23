package com.mot.productservices.consumer;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mot.productservices.service.ChapterRepairService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component @RequiredArgsConstructor @Slf4j
public class CrawlRepairResultConsumer {
    private final ObjectMapper objectMapper;
    private final ChapterRepairService repairService;

    @KafkaListener(
            topics = "${mot.crawl.repair-result-topic:mot.crawl.repair.result}",
            groupId = "${mot.crawl.repair-result-group:product-services-crawl-repair-results}")
    public void consume(String message) {
        try {
            repairService.applyResult(objectMapper.readValue(message, new TypeReference<Map<String, Object>>() {}));
        } catch (Exception exception) {
            log.error("Invalid crawl repair result: {}", exception.getMessage());
        }
    }
}
