package org.showpage.rallyserver.repository;

import org.showpage.rallyserver.entity.RallyParticipant;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RallyParticipantRepository extends JpaRepository<RallyParticipant, Integer> {
}
