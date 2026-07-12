package org.hameed.hameedmoneycli.repository;

import org.hameed.hameedmoneycli.enums.StagingSessionStatus;
import org.hameed.hameedmoneycli.model.entity.IngestionStagingSession;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface IngestionStagingSessionRepository extends JpaRepository<IngestionStagingSession, Long> {

    List<IngestionStagingSession> findByStatusOrderByCreatedAtDesc(StagingSessionStatus status);

    List<IngestionStagingSession> findAllByOrderByCreatedAtDesc();
}
