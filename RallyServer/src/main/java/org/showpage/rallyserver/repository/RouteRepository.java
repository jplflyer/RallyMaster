package org.showpage.rallyserver.repository;

import org.showpage.rallyserver.entity.Route;
import org.showpage.rallyserver.exception.NotFoundException;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface RouteRepository extends JpaRepository<Route, Integer> {

    List<Route> findByRideId(Integer rideId);

    default Route findById_WithThrow(Integer id) throws NotFoundException {
        return findById(id).orElseThrow(() -> new NotFoundException("Route not found"));
    }
}
