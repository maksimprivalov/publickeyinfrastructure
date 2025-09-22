package com.app.pki_backend.audit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

@Component
public class AuditLogger {

    private static final Logger logger = LoggerFactory.getLogger(AuditLogger.class);

    public void log(String action, String userEmail) {
        logger.info("[AUDIT] {} by user={} at={}", action, userEmail, LocalDateTime.now());
    }
}
