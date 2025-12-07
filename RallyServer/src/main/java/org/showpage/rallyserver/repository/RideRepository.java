package org.showpage.rallyserver.repository;

import org.showpage.rallyserver.entity.Ride;
import org.showpage.rallyserver.exception.NotFoundException;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.List;

public interface RideRepository extends JpaRepository<Ride, Integer>, JpaSpecificationExecutor<Ride> {

    List<Ride> findByMemberId(Integer memberId);

    List<Ride> findByRallyId(Integer rallyId);

    default Ride findById_WithThrow(Integer id) throws NotFoundException {
        return findById(id).orElseThrow(() -> new NotFoundException("Ride not found"));
    }
}
