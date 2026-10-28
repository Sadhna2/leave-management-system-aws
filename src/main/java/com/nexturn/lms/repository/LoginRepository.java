package com.nexturn.lms.repository;
import com.nexturn.lms.entity.Login;
import org.springframework.data.jpa.repository.JpaRepository;
/** Resolves provisioned accounts by unique username. */
public interface LoginRepository extends JpaRepository<Login, String> { }
