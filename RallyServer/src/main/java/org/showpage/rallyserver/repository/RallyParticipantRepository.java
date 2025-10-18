package org.showpage.rallyserver.repository;

import org.showpage.rallyserver.entity.RallyParticipant;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface RallyParticipantRepository extends JpaRepository<RallyParticipant, Integer> {
    Optional<RallyParticipant> findByRallyIdAndMemberId(Integer rallyId, Integer memberId);
}
