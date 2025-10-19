package org.showpage.rallyserver.repository;

import org.showpage.rallyserver.entity.RallyParticipant;
import org.showpage.rallyserver.exception.NotFoundException;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface RallyParticipantRepository extends JpaRepository<RallyParticipant, Integer> {
    Optional<RallyParticipant> findByRallyIdAndMemberId(Integer rallyId, Integer memberId);

    //======================================================================
    // We have some things we do a lot, so let's make convenience methods.
    //======================================================================
    default RallyParticipant getRiderForRally(int rallyId, int riderId) throws NotFoundException {
        return findByRallyIdAndMemberId(rallyId, riderId)
                .orElseThrow(() -> new NotFoundException("Rider not registered for this rally"));
    }
}
