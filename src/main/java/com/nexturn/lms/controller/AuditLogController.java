package com.nexturn.lms.controller;
import com.nexturn.lms.entity.*;
import com.nexturn.lms.dto.PageResponse;
import com.nexturn.lms.enums.Role;
import com.nexturn.lms.exception.ApiException;
import com.nexturn.lms.repository.*;
import org.springframework.data.domain.*;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
/** Provides read-only audit access to the employee or their currently assigned manager. */
@RestController
@RequestMapping("/api/audit-logs")
public class AuditLogController {
    private final AuditLogRepository audit;
    private final EmployeeRepository employees;
    public AuditLogController(AuditLogRepository audit, EmployeeRepository employees) { this.audit=audit; this.employees=employees; }
    @GetMapping("/employee/{empId}")
    public PageResponse<AuditLog> list(@PathVariable Integer empId, @RequestAttribute("authenticatedUser") Login actor,
            @PageableDefault(size=20, sort="actionDate", direction=Sort.Direction.DESC) Pageable page) {
        Employee employee = employees.findById(empId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Employee not found"));
        if (!actor.getEmpId().equals(empId) && !(actor.getRole()==Role.MANAGER && actor.getEmpId().equals(employee.getManagerId())))
            throw new ApiException(HttpStatus.FORBIDDEN, "Audit access denied");
        return PageResponse.of(audit.findByEmpId(empId, page));
    }
}
