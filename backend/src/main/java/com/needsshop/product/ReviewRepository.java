package com.needsshop.product;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface ReviewRepository extends JpaRepository<Review, Long> {

    /** 최신순. 같은 시각이면 나중에 들어온 것이 먼저 */
    Page<Review> findByProductIdOrderByCreatedAtDescIdDesc(Long productId, Pageable pageable);

    /** 별점별 개수 (분포) */
    @Query("select r.rating as rating, count(r) as count from Review r where r.productId = :productId group by r.rating")
    List<RatingCount> countByRating(@Param("productId") Long productId);

    interface RatingCount {
        int getRating();

        long getCount();
    }
}
