package org.showpage.rallyserver.repository;

import org.showpage.rallyserver.entity.RallyParticipant;
import org.showpage.rallyserver.exception.NotFoundException;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface RallyParticipantRepository extends JpaRepository<RallyParticipant, Integer> {
    Optional<RallyParticipant> findByRallyIdAndMemberId(Integer rallyId, Integer memberId);

    /**
     * Find all participations for a member where the rally hasn't ended more than 1 week ago.
     * Includes future rallies, in-progress rallies, and recently completed rallies.
     */
    @Query("SELECT rp FROM RallyParticipant rp " +
           "JOIN FETCH rp.rally r " +
           "WHERE rp.memberId = :memberId " +
           "AND r.endDate >= :cutoffDate " +
           "ORDER BY r.startDate DESC")
    List<RallyParticipant> findActiveParticipationsByMemberId(
            @Param("memberId") Integer memberId,
            @Param("cutoffDate") LocalDate cutoffDate
    );

    //======================================================================
    // We have some things we do a lot, so let's make convenience methods.
    //======================================================================
    default RallyParticipant getRiderForRally(int rallyId, int riderId) throws NotFoundException {
        return findByRallyIdAndMemberId(rallyId, riderId)
                .orElseThrow(() -> new NotFoundException("Rider not registered for this rally"));
    }
}
