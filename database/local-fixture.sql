-- OPTIONAL local-only fixture for an empty database; never run on a production/shared database.
-- Accounts are deliberately omitted. Generate BCrypt hashes and provision login rows as shown in README.md.
INSERT INTO employee (emp_id,emp_name,email,designation,manager_id,gender,address) VALUES
 (100,'Demo Manager','manager@example.test','Manager',NULL,'Other','Local demo');
INSERT INTO employee (emp_id,emp_name,email,designation,manager_id,gender,address) VALUES
 (200,'Demo Employee','employee@example.test','Employee',100,'Other','Local demo');
INSERT INTO leave_type (leave_type_id,leave_type_name,description,annual_quota,allow_negative_balance)
 VALUES (1,'Annual','Annual leave',20,FALSE);
INSERT INTO leave_balance (emp_id,leave_type_id,total_allocated_days,used_days,remaining_days,`year`)
 VALUES (200,1,20,0,20,2026);
INSERT INTO leave_application (emp_id,leave_type_id,start_date,end_date,no_of_days_requested,reason,status)
 VALUES (200,1,'2026-11-02','2026-11-04',3,'Local approval demonstration','PENDING');
