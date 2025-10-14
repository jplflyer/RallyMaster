package org.showpage.rallyserver.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.showpage.rallyserver.entity.BonusPoint;

public interface BonusPointRepository extends JpaRepository<BonusPoint, Integer> {
}
