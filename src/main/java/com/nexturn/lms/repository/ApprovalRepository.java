package com.nexturn.lms.repository;

import com.nexturn.lms.entity.Approval;
import org.springframework.data.jpa.repository.JpaRepository;

/** Persistence access for approval repository. */

public interface ApprovalRepository extends JpaRepository<Approval, Long> {
}
