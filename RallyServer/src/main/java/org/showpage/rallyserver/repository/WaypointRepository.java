package org.showpage.rallyserver.repository;

import org.showpage.rallyserver.entity.Waypoint;
import org.showpage.rallyserver.exception.NotFoundException;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface WaypointRepository extends JpaRepository<Waypoint, Integer> {

    List<Waypoint> findByRideLegId(Integer rideLegId);

    default Waypoint findById_WithThrow(Integer id) throws NotFoundException {
        return findById(id).orElseThrow(() -> new NotFoundException("Waypoint not found"));
    }
}
