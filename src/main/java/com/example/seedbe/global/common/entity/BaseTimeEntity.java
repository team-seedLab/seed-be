package com.example.seedbe.global.common.entity;

import jakarta.persistence.Column;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.MappedSuperclass;
import lombok.Getter;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;

@Getter
@MappedSuperclass // 1. 상속받는 자식 엔티티에게 아래 필드들을 컬럼으로 인식시킴
@EntityListeners(AuditingEntityListener.class) // 2. JPA에게 "이 엔티티에 이벤트(생성, 수정)가 발생하면 네가 시간 좀 채워줘!" 하고 리스너를 붙임
public abstract class BaseTimeEntity {

    @CreatedDate
    @Column(name = "created_at", updatable = false) // 생성일은 한 번 들어가면 절대 수정되면 안 됨! (방어 로직)
    private LocalDateTime createdAt;

    @LastModifiedDate
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}
