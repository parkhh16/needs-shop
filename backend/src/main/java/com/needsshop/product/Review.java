package com.needsshop.product;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.LocalDateTime;

/**
 * 상품 후기. 지금은 시드로만 만들어지는 샘플 데이터라 작성·수정 API가 없다.
 * 상품은 id로만 참조한다 (OrderItem과 같은 방식. FK는 마이그레이션에 있다).
 */
@Getter
@Entity
@Table(name = "product_review", indexes = @Index(name = "idx_product_review_product_created", columnList = "product_id, created_at"))
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Review {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "product_id", nullable = false)
    private Long productId;

    /** 1~5. 컬럼은 TINYINT + CHECK */
    @JdbcTypeCode(SqlTypes.TINYINT)
    @Column(nullable = false)
    private int rating;

    /** 별점만 남긴 후기는 null */
    @Column(length = 1000)
    private String body;

    @Column(nullable = false, length = 30)
    private String nickname;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    public Review(Long productId, int rating, String body, String nickname, LocalDateTime createdAt) {
        if (rating < 1 || rating > 5) throw new IllegalArgumentException("별점은 1~5: " + rating);
        this.productId = productId;
        this.rating = rating;
        this.body = body;
        this.nickname = nickname;
        this.createdAt = createdAt;
    }
}
