package org.showpage.rallyserver.repository;

import org.showpage.rallyserver.entity.Rally;
import org.showpage.rallyserver.exception.NotFoundException;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;

public interface RallyRepository extends JpaRepository<Rally, Integer>, JpaSpecificationExecutor<Rally> {

    List<Rally> findByStartDateLessThanEqualAndEndDateGreaterThanEqual(LocalDate end, LocalDate start);

    //======================================================================
    // Forms that throw exceptions.
    //======================================================================
    default Rally findById_WithThrow(int id) throws NotFoundException {
        return findById(id).orElseThrow(() -> new NotFoundException("Rally not found"));
    }
}
