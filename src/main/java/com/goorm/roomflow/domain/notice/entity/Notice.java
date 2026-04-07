package com.goorm.roomflow.domain.notice.entity;

import com.goorm.roomflow.domain.BaseEntity;
import com.goorm.roomflow.domain.user.entity.User;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "notices")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Notice extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long noticeId;

    @Column(nullable = false, length = 200)
    private String title;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String content;

    @Column(name = "is_pinned", nullable = false)
    private boolean pinned = false;

    @Column(name = "is_visibled", nullable = false)
    private boolean visible = true;

    @Column(nullable = false)
    private int viewCount = 0;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "created_by")
    private User createdBy;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "updated_by")
    private User updatedBy;

    @Builder
    public Notice(String title, String content, boolean pinned, boolean visible, User createdBy) {
        validateNotice(title, content);

        this.title = title;
        this.content = content;
        this.pinned = pinned;
        this.visible = visible;
        this.createdBy = createdBy;
        this.updatedBy = createdBy;
    }

    public void modify(String title, String content, boolean pinned, boolean visible, User updatedBy) {
        validateNotice(title, content);

        this.title = title;
        this.content = content;
        this.pinned = pinned;
        this.visible = visible;
        this.updatedBy = updatedBy;
    }

    public void increaseViewCount() {
        this.viewCount++;
    }

    private void validateNotice(String title, String content) {
        if (title == null || title.isBlank()) {
            throw new IllegalArgumentException("공지 제목은 비어 있을 수 없습니다.");
        }

        if (content == null || content.isBlank()) {
            throw new IllegalArgumentException("공지 내용은 비어 있을 수 없습니다.");
        }
    }
}
