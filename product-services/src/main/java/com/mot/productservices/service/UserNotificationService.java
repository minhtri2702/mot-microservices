package com.mot.productservices.service;

import com.mot.productservices.dto.PagedResponseDTO;
import com.mot.productservices.dto.UserNotificationDTO;
import com.mot.productservices.entity.UserNotification;
import com.mot.productservices.repository.UserNotificationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service @RequiredArgsConstructor
public class UserNotificationService {
    private final UserNotificationRepository repository;

    @Transactional
    public void createForNewChapter(Integer chapterId) { repository.createForNewChapter(chapterId); }

    @Transactional(readOnly = true)
    public PagedResponseDTO<UserNotificationDTO> list(String userId, int page, int size) {
        Page<UserNotification> result = repository.findByUserIdOrderByCreatedAtDesc(
                userId, PageRequest.of(Math.max(0, page), Math.min(30, Math.max(1, size))));
        return PagedResponseDTO.<UserNotificationDTO>builder()
                .content(result.getContent().stream().map(this::toDto).toList())
                .page(result.getNumber()).size(result.getSize()).totalElements(result.getTotalElements())
                .totalPages(result.getTotalPages()).first(result.isFirst()).last(result.isLast()).build();
    }

    @Transactional(readOnly = true)
    public long unreadCount(String userId) { return repository.countByUserIdAndReadAtIsNull(userId); }

    @Transactional
    public void markAllRead(String userId) { repository.markAllRead(userId, LocalDateTime.now()); }

    private UserNotificationDTO toDto(UserNotification n) {
        return UserNotificationDTO.builder().id(n.getId()).mangaId(n.getMangaId())
                .chapterId(n.getChapterId()).type(n.getType()).title(n.getTitle())
                .read(n.getReadAt() != null).createdAt(n.getCreatedAt()).build();
    }
}
