package com.gradia.repository;

import com.gradia.model.ProfileMetadata;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface ProfileMetadataRepository extends JpaRepository<ProfileMetadata, UUID> {
    Optional<ProfileMetadata> findByProfileId(UUID profileId);
}

