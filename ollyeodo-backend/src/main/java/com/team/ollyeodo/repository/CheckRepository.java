package com.team.ollyeodo.repository;

import com.team.ollyeodo.domain.Check;
import com.team.ollyeodo.domain.Severity;
import java.util.List;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface CheckRepository extends JpaRepository<Check, Long> {

    long countByOverall(Severity overall);

    /** 최근 검수 (최신순). Finding은 함께 불러오지 않는다 */
    @Query("select c from Check c order by c.createdAt desc, c.id desc")
    List<Check> findRecent(Pageable pageable);
}
