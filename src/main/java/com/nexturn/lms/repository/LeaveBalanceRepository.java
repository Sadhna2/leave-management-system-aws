package com.nexturn.lms.repository;
import com.nexturn.lms.entity.LeaveBalance;
import jakarta.persistence.LockModeType;
import java.util.Optional;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
/** Locks the employee/type/year balance so concurrent approvals cannot overspend it. */
public interface LeaveBalanceRepository extends JpaRepository<LeaveBalance, Integer> {
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select b from LeaveBalance b where b.empId=:emp and b.leaveTypeId=:type and b.year=:year")
    Optional<LeaveBalance> findLocked(@Param("emp") Integer emp, @Param("type") Integer type, @Param("year") Integer year);
}
