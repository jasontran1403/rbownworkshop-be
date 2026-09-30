package com.rbownworkshop.server.repository;

import com.rbownworkshop.server.entity.AppConfig;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AppConfigRepository extends JpaRepository<AppConfig, String> {
}
