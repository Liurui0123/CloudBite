package com.cloudbite.ai.repo;

import com.cloudbite.ai.entity.AiLog;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AiLogRepository extends JpaRepository<AiLog, Long> {
}