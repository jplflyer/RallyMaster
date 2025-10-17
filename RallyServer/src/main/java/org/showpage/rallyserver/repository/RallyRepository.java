package org.showpage.rallyserver.repository;

import org.showpage.rallyserver.entity.Rally;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;

public interface RallyRepository extends JpaRepository<Rally, Integer>, JpaSpecificationExecutor<Rally> {

    List<Rally> findByStartDateLessThanEqualAndEndDateGreaterThanEqual(LocalDate end, LocalDate start);
}
