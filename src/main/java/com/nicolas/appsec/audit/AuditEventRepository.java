package com.nicolas.appsec.audit;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface AuditEventRepository extends JpaRepository<AuditEvent, Long> {

    Page<AuditEvent> findAllByOrderByEventTimeDesc(Pageable pageable);

    @Query("""
            SELECT e FROM AuditEvent e
            WHERE (:actor  IS NULL OR LOWER(e.actor)  LIKE LOWER(CONCAT('%', :actor,  '%')))
              AND (:action IS NULL OR LOWER(e.action) LIKE LOWER(CONCAT('%', :action, '%')))
              AND (:target IS NULL OR LOWER(e.target) LIKE LOWER(CONCAT('%', :target, '%')))
            ORDER BY e.eventTime DESC
            """)
    Page<AuditEvent> findFiltered(
            @Param("actor")  String actor,
            @Param("action") String action,
            @Param("target") String target,
            Pageable pageable
    );
}
