package com.jvault.jvault.repo;

import com.jvault.jvault.model.AuditLog;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AuditLogRepo extends JpaRepository<AuditLog, Long> {
}
