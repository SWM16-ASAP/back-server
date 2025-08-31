package com.linglevel.api.version.service;

import com.linglevel.api.version.dto.VersionResponse;
import com.linglevel.api.version.dto.VersionUpdateRequest;
import com.linglevel.api.version.dto.VersionUpdateResponse;
import com.linglevel.api.version.entity.AppVersion;
import com.linglevel.api.version.repository.AppVersionRepository;
import com.linglevel.api.version.exception.VersionErrorCode;
import com.linglevel.api.version.exception.VersionException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class VersionService {
    
    private final AppVersionRepository appVersionRepository;

    public VersionResponse getVersion() {
        AppVersion appVersion = appVersionRepository.findTopByOrderByUpdatedAtDesc()
                .orElseThrow(() -> new VersionException(VersionErrorCode.VERSION_NOT_FOUND));
        
        return VersionResponse.builder()
                .latestVersion(appVersion.getLatestVersion())
                .minimumVersion(appVersion.getMinimumVersion())
                .build();
    }

    @Transactional
    public VersionUpdateResponse updateVersion(VersionUpdateRequest request) {
        if (request.getLatestVersion() == null && request.getMinimumVersion() == null) {
            throw new VersionException(VersionErrorCode.VERSION_FIELD_REQUIRED);
        }

        Optional<AppVersion> existingVersion = appVersionRepository.findTopByOrderByUpdatedAtDesc();
        AppVersion appVersion;

        if (existingVersion.isPresent()) {
            appVersion = existingVersion.get();
            if (request.getLatestVersion() != null) {
                appVersion.setLatestVersion(request.getLatestVersion());
            }
            if (request.getMinimumVersion() != null) {
                appVersion.setMinimumVersion(request.getMinimumVersion());
            }
        } else {
            appVersion = new AppVersion();
            appVersion.setLatestVersion(request.getLatestVersion() != null ? request.getLatestVersion() : "1.0.0");
            appVersion.setMinimumVersion(request.getMinimumVersion() != null ? request.getMinimumVersion() : "1.0.0");
        }

        appVersion.setUpdatedAt(LocalDateTime.now());
        AppVersion savedVersion = appVersionRepository.save(appVersion);

        return VersionUpdateResponse.builder()
                .latestVersion(savedVersion.getLatestVersion())
                .minimumVersion(savedVersion.getMinimumVersion())
                .updatedAt(savedVersion.getUpdatedAt())
                .build();
    }
}