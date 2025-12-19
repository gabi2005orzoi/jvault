package com.jvault.jvault.service;

import com.jvault.jvault.model.AuditLog;
import com.jvault.jvault.repo.AuditLogRepo;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AuditLogService {
    private final AuditLogRepo auditLogRepo;

    public void logAction(String email, String action, String details, String ipAddress){
        AuditLog auditLog = new AuditLog(email, action, details, ipAddress);
        auditLogRepo.save(auditLog);
    }
}
