package com.team.ollyeodo.repository;

import com.team.ollyeodo.domain.Check;
import com.team.ollyeodo.domain.Severity;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface CheckRepository extends JpaRepository<Check, Long> {

    /** 내 검수만 (#71). 다른 사람 기록은 없는 것처럼 404 */
    Optional<Check> findByIdAndUserId(Long id, UUID userId);

    long countByUserId(UUID userId);

    long countByUserIdAndOverall(UUID userId, Severity overall);

    /** 내 최근 검수 (최신순). Finding은 함께 불러오지 않는다 */
    @Query("select c from Check c where c.userId = :userId order by c.createdAt desc, c.id desc")
    List<Check> findRecent(@Param("userId") UUID userId, Pageable pageable);
}
