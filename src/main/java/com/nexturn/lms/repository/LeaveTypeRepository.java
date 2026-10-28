package com.nexturn.lms.repository;

import com.nexturn.lms.entity.LeaveType;
import org.springframework.data.jpa.repository.JpaRepository;

/** Persistence access for leave type repository. */

public interface LeaveTypeRepository extends JpaRepository<LeaveType, Integer> {
}
