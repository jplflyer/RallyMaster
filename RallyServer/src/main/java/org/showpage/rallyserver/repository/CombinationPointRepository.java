package org.showpage.rallyserver.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.showpage.rallyserver.entity.CombinationPoint;

import java.util.List;

public interface CombinationPointRepository extends JpaRepository<CombinationPoint, Integer> {
    List<CombinationPoint> findByCombinationId(Integer combinationId);
}
