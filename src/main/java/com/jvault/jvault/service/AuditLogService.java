package com.jvault.jvault.service;

import com.jvault.jvault.repo.AuditLogRepo;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AuditLogService {
    private final AuditLogRepo auditLogRepo;
}
