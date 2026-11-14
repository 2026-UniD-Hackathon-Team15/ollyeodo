package com.team.ollyeodo.domain;

import jakarta.persistence.*;
import java.time.Instant;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

/** 메뉴. Menu 1 : N Check (checks.menu_id 외래키) */
@Entity
@Table(name = "menus")
@Getter
@Setter
@NoArgsConstructor
public class Menu {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    @JdbcTypeCode(SqlTypes.JSON)   // PostgreSQL에서는 jsonb 컬럼으로 생성됨
    private MenuFacts facts;

    private String realImagePath;

    @CreationTimestamp
    @Column(updatable = false)
    private Instant createdAt;

    public Menu(String name, MenuFacts facts, String realImagePath) {
        this.name = name;
        this.facts = facts;
        this.realImagePath = realImagePath;
    }
}