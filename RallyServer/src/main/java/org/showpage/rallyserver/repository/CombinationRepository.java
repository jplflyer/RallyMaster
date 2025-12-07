package org.showpage.rallyserver.repository;

import org.showpage.rallyserver.entity.Combination;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface CombinationRepository extends JpaRepository<Combination, Integer> {
    List<Combination> findByRallyId(Integer rallyId);
}
