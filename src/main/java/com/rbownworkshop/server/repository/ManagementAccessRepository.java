package com.rbownworkshop.server.repository;

import com.rbownworkshop.server.entity.ManagementAccess;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ManagementAccessRepository extends JpaRepository<ManagementAccess, Long> {
}
