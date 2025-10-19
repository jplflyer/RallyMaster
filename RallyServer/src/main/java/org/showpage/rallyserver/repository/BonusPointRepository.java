package org.showpage.rallyserver.repository;

import org.showpage.rallyserver.exception.NotFoundException;
import org.springframework.data.jpa.repository.JpaRepository;
import org.showpage.rallyserver.entity.BonusPoint;

import java.util.List;

public interface BonusPointRepository extends JpaRepository<BonusPoint, Integer> {
    List<BonusPoint> findByRallyId(Integer rallyId);

    //======================================================================
    // We have some calls that should throw on failure. Let's reduce
    // boilerplate everywhere else.
    //======================================================================
    default BonusPoint findById_WithThrow(int id) throws NotFoundException {
        return findById(id).orElseThrow(() -> new NotFoundException(""));
    }
}
