package org.showpage.rallyserver.repository;

import org.showpage.rallyserver.entity.EarnedCombination;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface EarnedCombinationRepository extends JpaRepository<EarnedCombination, Integer> {
    List<EarnedCombination> findByRallyParticipantId(Integer rallyParticipantId);
}
