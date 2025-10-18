package org.showpage.rallyserver.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.showpage.rallyserver.entity.BonusPoint;

import java.util.List;

public interface BonusPointRepository extends JpaRepository<BonusPoint, Integer> {
    List<BonusPoint> findByRallyId(Integer rallyId);
}
