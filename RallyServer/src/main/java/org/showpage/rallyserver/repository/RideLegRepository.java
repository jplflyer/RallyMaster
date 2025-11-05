package org.showpage.rallyserver.repository;

import org.showpage.rallyserver.entity.RideLeg;
import org.showpage.rallyserver.exception.NotFoundException;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface RideLegRepository extends JpaRepository<RideLeg, Integer> {

    List<RideLeg> findByRouteId(Integer routeId);

    default RideLeg findById_WithThrow(Integer id) throws NotFoundException {
        return findById(id).orElseThrow(() -> new NotFoundException("RideLeg not found"));
    }
}
