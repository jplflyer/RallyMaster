package org.showpage.rallyserver.repository;

import org.showpage.rallyserver.entity.EarnedBonusPoint;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface EarnedBonusPointRepository extends JpaRepository<EarnedBonusPoint, Integer> {
    List<EarnedBonusPoint> findByRallyParticipantId(Integer rallyParticipantId);
}
