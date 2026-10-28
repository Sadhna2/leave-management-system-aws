package com.nexturn.lms.repository;
import com.nexturn.lms.entity.Employee;
import jakarta.persistence.LockModeType;
import java.util.Optional;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
/** Stabilizes employee assignment with a shared lock and SELECT-only MySQL permissions. */
public interface EmployeeRepository extends JpaRepository<Employee, Integer> {
    @Lock(LockModeType.PESSIMISTIC_READ)
    @Query("select e from Employee e where e.empId = :id")
    Optional<Employee> findLockedById(@Param("id") Integer id);
}
