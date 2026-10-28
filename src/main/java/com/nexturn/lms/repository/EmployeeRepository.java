package com.nexturn.lms.repository;
import com.nexturn.lms.entity.Employee;
import jakarta.persistence.LockModeType;
import java.util.Optional;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
/** Reads employee assignment; a lock stabilizes it while a decision is being recorded. */
public interface EmployeeRepository extends JpaRepository<Employee, Integer> {
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select e from Employee e where e.empId = :id")
    Optional<Employee> findLockedById(@Param("id") Integer id);
}
