package com.linglevel.api.version.repository;

import com.linglevel.api.version.entity.AppVersion;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface AppVersionRepository extends JpaRepository<AppVersion, Long> {

	Optional<AppVersion> findTopByOrderByUpdatedAtDesc();

}
