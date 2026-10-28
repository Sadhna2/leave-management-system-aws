package com.nexturn.lms.repository;
import com.nexturn.lms.entity.LeaveApplication;
import com.nexturn.lms.enums.LeaveStatus;
import jakarta.persistence.LockModeType;
import java.util.Optional;
import org.springframework.data.domain.*;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
/** Serializes decisions on one leave and pages the assigned manager's pending queue. */
public interface LeaveRepository extends JpaRepository<LeaveApplication, Integer> {
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select l from LeaveApplication l where l.leaveId = :id")
    Optional<LeaveApplication> findLockedById(@Param("id") Integer id);
    @Query("select l from LeaveApplication l, Employee e where l.empId = e.empId and e.managerId = :manager and l.status = :status")
    Page<LeaveApplication> findTeamLeaves(@Param("manager") Integer manager, @Param("status") LeaveStatus status, Pageable pageable);
}
