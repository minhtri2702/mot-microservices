package com.mot.productservices.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.core.ConsumerFactory;
import org.springframework.kafka.listener.CommonErrorHandler;
import org.springframework.kafka.listener.DefaultErrorHandler;
import org.springframework.util.backoff.FixedBackOff;

/**
 * Cấu hình Kafka consumer cho product-services.
 * 
 * ContainerFactory là bean cần thiết để @KafkaListener hoạt động.
 * Nó quản lý việc tạo consumer threads và xử lý lỗi.
 */
@Configuration
public class KafkaConfig {

    /**
     * Tạo container factory cho Kafka listener.
     * 
     * - ConsumerFactory: được Spring auto-config từ application.yml
     * - setConcurrency(1): 1 thread xử lý message (có thể tăng nếu cần)
     * - ErrorHandler: nếu lỗi, thử lại 3 lần, mỗi lần cách 1 giây
     */
    @Bean
    public ConcurrentKafkaListenerContainerFactory<String, String> kafkaListenerContainerFactory(
            ConsumerFactory<String, String> consumerFactory) {
        
        ConcurrentKafkaListenerContainerFactory<String, String> factory =
                new ConcurrentKafkaListenerContainerFactory<>();
        
        factory.setConsumerFactory(consumerFactory);
        factory.setConcurrency(1);

        // Xử lý lỗi: thử lại 3 lần, mỗi lần cách 1 giây
        CommonErrorHandler errorHandler = new DefaultErrorHandler(
                new FixedBackOff(1000L, 3L));
        factory.setCommonErrorHandler(errorHandler);

        return factory;
    }
}
