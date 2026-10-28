package com.nexturn.lms.repository;

import com.nexturn.lms.entity.AuditLog;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

/** Persistence access for audit log repository. */

public interface AuditLogRepository extends JpaRepository<AuditLog, Long> {

    org.springframework.data.domain.Page<AuditLog> findByEmpId(Integer empId, org.springframework.data.domain.Pageable page);

    List<AuditLog> findByEmpIdOrderByActionDateDesc(Integer empId);
}
