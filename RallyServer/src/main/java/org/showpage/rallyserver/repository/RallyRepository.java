package org.showpage.rallyserver.repository;

import org.showpage.rallyserver.entity.Rally;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RallyRepository extends JpaRepository<Rally, Integer> {
}
