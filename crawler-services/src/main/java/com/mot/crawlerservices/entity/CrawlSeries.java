package com.mot.crawlerservices.Entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.GenericGenerator;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "crawl_series",
        uniqueConstraints = @UniqueConstraint(columnNames = {"source", "external_id"}))
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CrawlSeries {

    @Id
    @GeneratedValue(generator = "UUID")
    @GenericGenerator(name = "UUID", strategy = "org.hibernate.id.UUIDGenerator")
    private UUID id;

    @Column(length = 50)
    private String source; // nettruyen, truyenqq

    @Column(name = "external_id", nullable = false)
    private String externalId;

    private String name;

    @Column(columnDefinition = "TEXT")
    private String url;

    @Column(length = 20)
    private String status; // new, done

    @Column(name = "created_at")
    private LocalDateTime createdAt;
}
