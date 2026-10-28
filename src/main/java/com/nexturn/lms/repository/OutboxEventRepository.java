package com.nexturn.lms.repository;
import com.nexturn.lms.entity.OutboxEvent;
import org.springframework.data.jpa.repository.JpaRepository;
/** Inserts events into the same relational transaction as the manager decision. */
public interface OutboxEventRepository extends JpaRepository<OutboxEvent,String> { }
