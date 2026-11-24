package org.hameed.hameedmoneycomputer.repository;

import org.hameed.hameedmoneycomputer.enums.StagingSessionStatus;
import org.hameed.hameedmoneycomputer.model.entity.IngestionStagingSession;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface IngestionStagingSessionRepository extends JpaRepository<IngestionStagingSession, Long> {

    List<IngestionStagingSession> findByStatusOrderByCreatedAtDesc(StagingSessionStatus status);

    List<IngestionStagingSession> findAllByOrderByCreatedAtDesc();
}
