-- Member 3 schema for a NEW MySQL 8 database. Run as the schema owner, not the application user.
-- Select/create the database yourself first. This script deliberately does not alter the previous project database.
-- Shared employee, leave_type, leave_application and leave_balance tables are integration dependencies.
CREATE TABLE IF NOT EXISTS employee (
 emp_id INT PRIMARY KEY, emp_name VARCHAR(100) NOT NULL, email VARCHAR(100) UNIQUE,
 phone_number VARCHAR(10) UNIQUE, designation VARCHAR(50) NOT NULL, dept_id INT,
 manager_id INT, gender VARCHAR(10) NOT NULL, address VARCHAR(200) NOT NULL,
 CONSTRAINT fk_employee_manager FOREIGN KEY (manager_id) REFERENCES employee(emp_id),
 INDEX ix_employee_manager (manager_id)
) ENGINE=InnoDB;
CREATE TABLE IF NOT EXISTS leave_type (
 leave_type_id INT AUTO_INCREMENT PRIMARY KEY, leave_type_name VARCHAR(50) NOT NULL,
 description VARCHAR(200) NOT NULL, annual_quota INT NOT NULL, allow_negative_balance BOOLEAN NOT NULL DEFAULT FALSE,
 CONSTRAINT ck_quota CHECK (annual_quota >= 0)
) ENGINE=InnoDB;
CREATE TABLE IF NOT EXISTS leave_application (
 leave_id INT AUTO_INCREMENT PRIMARY KEY, emp_id INT NOT NULL, leave_type_id INT NOT NULL,
 start_date DATE NOT NULL, end_date DATE NOT NULL, no_of_days_requested INT NOT NULL,
 reason VARCHAR(200) NOT NULL, status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
 CONSTRAINT fk_leave_employee FOREIGN KEY (emp_id) REFERENCES employee(emp_id),
 CONSTRAINT fk_leave_type FOREIGN KEY (leave_type_id) REFERENCES leave_type(leave_type_id),
 CONSTRAINT ck_leave_dates CHECK (end_date >= start_date),
 CONSTRAINT ck_leave_days CHECK (no_of_days_requested > 0),
 CONSTRAINT ck_leave_status CHECK (status IN ('PENDING','APPROVED','REJECTED','CANCELLED')),
 INDEX ix_leave_employee_status (emp_id,status)
) ENGINE=InnoDB;
CREATE TABLE IF NOT EXISTS leave_balance (
 balance_id INT AUTO_INCREMENT PRIMARY KEY, emp_id INT NOT NULL, leave_type_id INT NOT NULL,
 total_allocated_days INT NOT NULL, used_days INT NOT NULL DEFAULT 0, remaining_days INT NOT NULL, `year` INT NOT NULL,
 CONSTRAINT uk_balance UNIQUE (emp_id,leave_type_id,`year`),
 CONSTRAINT fk_balance_employee FOREIGN KEY (emp_id) REFERENCES employee(emp_id),
 CONSTRAINT fk_balance_type FOREIGN KEY (leave_type_id) REFERENCES leave_type(leave_type_id),
 CONSTRAINT ck_balance CHECK (total_allocated_days >= 0 AND used_days >= 0 AND remaining_days=total_allocated_days-used_days)
) ENGINE=InnoDB;
CREATE TABLE IF NOT EXISTS login (
 username VARCHAR(50) PRIMARY KEY, password VARCHAR(255) NOT NULL, role VARCHAR(20) NOT NULL, emp_id INT NOT NULL UNIQUE,
 CONSTRAINT fk_login_employee FOREIGN KEY (emp_id) REFERENCES employee(emp_id),
 CONSTRAINT ck_login_role CHECK (role IN ('EMPLOYEE','MANAGER','HR','ADMIN'))
) ENGINE=InnoDB;
-- Sessions are stored centrally because Lambda instances do not share process memory.
CREATE TABLE IF NOT EXISTS auth_session (
 token_hash VARCHAR(64) PRIMARY KEY, username VARCHAR(50) NOT NULL, expires_at TIMESTAMP(6) NOT NULL,
 CONSTRAINT fk_session_login FOREIGN KEY (username) REFERENCES login(username) ON DELETE CASCADE,
 INDEX ix_session_expiry (expires_at)
) ENGINE=InnoDB;
CREATE TABLE IF NOT EXISTS approval (
 approval_id BIGINT AUTO_INCREMENT PRIMARY KEY, leave_id INT NOT NULL UNIQUE, manager_id INT NOT NULL,
 decision VARCHAR(20) NOT NULL, comment VARCHAR(200) NOT NULL, action_date DATETIME(6) NOT NULL,
 CONSTRAINT fk_approval_leave FOREIGN KEY (leave_id) REFERENCES leave_application(leave_id),
 CONSTRAINT fk_approval_manager FOREIGN KEY (manager_id) REFERENCES employee(emp_id),
 CONSTRAINT ck_decision CHECK (decision IN ('APPROVED','REJECTED'))
) ENGINE=InnoDB;
CREATE TABLE IF NOT EXISTS notification (
 notification_id BIGINT AUTO_INCREMENT PRIMARY KEY, emp_id INT NOT NULL, type VARCHAR(30) NOT NULL,
 message VARCHAR(500) NOT NULL, read_flag BOOLEAN NOT NULL DEFAULT FALSE, created_at DATETIME(6) NOT NULL,
 CONSTRAINT fk_notification_employee FOREIGN KEY (emp_id) REFERENCES employee(emp_id),
 INDEX ix_notification_employee_time (emp_id,created_at)
) ENGINE=InnoDB;
CREATE TABLE IF NOT EXISTS audit_log (
 audit_id BIGINT AUTO_INCREMENT PRIMARY KEY, emp_id INT NOT NULL, leave_id INT NOT NULL, actor_emp_id INT NOT NULL,
 action_status VARCHAR(50) NOT NULL, details VARCHAR(200) NOT NULL, action_date DATETIME(6) NOT NULL,
 CONSTRAINT fk_audit_employee FOREIGN KEY (emp_id) REFERENCES employee(emp_id),
 CONSTRAINT fk_audit_leave FOREIGN KEY (leave_id) REFERENCES leave_application(leave_id),
 CONSTRAINT fk_audit_actor FOREIGN KEY (actor_emp_id) REFERENCES employee(emp_id),
 INDEX ix_audit_employee_time (emp_id,action_date)
) ENGINE=InnoDB;

-- Pending cloud events are part of the approval transaction; SNS delivery occurs after commit.
CREATE TABLE IF NOT EXISTS outbox_event (
 event_id VARCHAR(36) PRIMARY KEY, payload TEXT NOT NULL, created_at TIMESTAMP(6) NOT NULL,
 published_at TIMESTAMP(6) NULL, INDEX ix_outbox_pending (published_at,created_at)
) ENGINE=InnoDB;
