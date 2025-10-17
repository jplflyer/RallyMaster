package org.showpage.rallyserver.service;

import jakarta.persistence.criteria.Expression;
import jakarta.persistence.criteria.Path;
import jakarta.persistence.criteria.Predicate;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.showpage.rallyserver.entity.Member;
import org.showpage.rallyserver.entity.Rally;
import org.showpage.rallyserver.entity.RallyParticipant;
import org.showpage.rallyserver.entity.RallyParticipantType;
import org.showpage.rallyserver.exception.NotFoundException;
import org.showpage.rallyserver.exception.ValidationException;
import org.showpage.rallyserver.repository.RallyParticipantRepository;
import org.showpage.rallyserver.repository.RallyRepository;
import org.showpage.rallyserver.ui.CreateRallyRequest;
import org.showpage.rallyserver.ui.UpdateRallyRequest;
import org.showpage.rallyserver.util.DataValidator;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;

/**
 * CRUD on Rallies -- but not on riding one.
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class RallyService {
    private final RallyRepository rallyRepository;
    private final RallyParticipantRepository rallyParticipantRepository;

    /**
     * Get a Rally by ID.
     */
    public Rally getRally(Member member, Integer id) throws NotFoundException {
        Rally rally = rallyRepository.findById(id).orElseThrow(() -> new NotFoundException("Rally not found"));

        // This will throw NotFoundException even though it's really an authorization failure.
        checkAccess(member, rally, false);

        return rally;
    }

    /**
     * Create a rally. The person who creates it is automatically a RallyMaster.
     */
    public Rally createRally(Member member, CreateRallyRequest request) throws ValidationException {
        request.checkValid();

        Rally rally = Rally
                .builder()
                .name(request.getName().trim())
                .description(request.getDescription().trim())
                .locationCity(request.getLocationCity().trim())
                .locationState(request.getLocationState() != null ? request.getLocationState().trim() : null)
                .locationCountry(request.getLocationCountry().trim())
                .isPublic(request.getIsPublic() != null && request.getIsPublic())
                .pointsPublic(request.getPointsPublic() != null ? request.getPointsPublic() : false)
                .ridersPublic(request.getRidersPublic() != null ? request.getRidersPublic() : false)
                .organizersPublic(request.getOrganizersPublic() != null ? request.getOrganizersPublic() : false)
                .build();

        Rally created = rallyRepository.save(rally);
        RallyParticipant participant = RallyParticipant
                .builder()
                .rally(created)
                .member(member)
                .participantType(RallyParticipantType.ORGANIZER)
                .build();

        RallyParticipant createdParticipant = rallyParticipantRepository.save(participant);

        return created;
    }

    /**
     * We do not nullify any fields. We only modify provided fields that are non-null.
     */
    public Rally updateRally(Member member, Integer id, UpdateRallyRequest request) throws NotFoundException, ValidationException {
        Rally rally = rallyRepository.findById(id).orElseThrow(() -> new NotFoundException("Rally not found"));
        checkAccess(member, rally, true);

        LocalDate earliest = LocalDate.now().minusYears(5);
        LocalDate latest = LocalDate.now().plusYears(5);

        if (request.getStartDate() != null) {
            DataValidator.validDate(request.getStartDate(), earliest, latest, "Start Date");
        }
        if (request.getEndDate() != null) {
            DataValidator.validDate(request.getEndDate(), earliest, latest, "End Date");
        }
        if (request.getStartDate() != null && request.getEndDate() != null && request.getEndDate().isBefore(request.getStartDate())) {
            throw new ValidationException("Start date cannot be after end date");
        }

        if (DataValidator.nonEmpty(request.getName())) {
            rally.setName(request.getName().trim());
        }
        if (DataValidator.nonEmpty(request.getDescription())) {
            rally.setDescription(request.getDescription().trim());
        }
        if (DataValidator.nonEmpty(request.getLocationCity())) {
            rally.setLocationCity(request.getLocationCity().trim());
        }
        if (DataValidator.nonEmpty(request.getLocationState())) {
            rally.setLocationState(request.getLocationState().trim());
        }
        if (DataValidator.nonEmpty(request.getLocationCountry())) {
            rally.setLocationCountry(request.getLocationCountry().trim());
        }

        if (request.getStartDate() != null) {
            rally.setStartDate(request.getStartDate());
            if (rally.getStartDate().isAfter(earliest)) {
                rally.setEndDate(rally.getStartDate());
            }
        }
        if (request.getEndDate() != null) {
            rally.setEndDate(request.getEndDate());
            if (rally.getStartDate().isAfter(earliest)) {
                rally.setStartDate(rally.getEndDate());
            }
        }

        if (request.getIsPublic() != null) {
            rally.setIsPublic(request.getIsPublic());
        }

        if (request.getPointsPublic() != null) {
            rally.setPointsPublic(request.getPointsPublic());
        }

        if (request.getRidersPublic() != null) {
            rally.setRidersPublic(request.getRidersPublic());
        }

        if (request.getOrganizersPublic() != null) {
            rally.setOrganizersPublic(request.getOrganizersPublic());
        }

        Rally updated = rallyRepository.save(rally);

        return updated;
    }

    /**
     * A fairly full-featured search.
     */
    public Page<Rally> search(
          // text search: case-insensitive "contains"
          String name,

          // date range (inclusive)
          LocalDate from,
          LocalDate to,

          // location filters
          String country,   // e.g., "US", "United States", "CA", "Canada"
          String region, // state/province/territory text

          // optional "within radius of point" (no PostGIS required)
          Double nearLat,
          Double nearLng,
          Double radiusMiles,

          Pageable pageable
    ){
        Specification<Rally> spec = Specification.<Rally>unrestricted()
                .and(nameContains(name))
                .and(dateOverlaps(from, to))
                .and(countryMatches(country))
                .and(regionMatches(region))
                .and(withinRadius(nearLat, nearLng, radiusMiles));

        return rallyRepository.findAll(spec, pageable);
    }

    //======================================================================
    // General helpers.
    //======================================================================

    /**
     * Throw NotFoundException if this is a private rally and this person isn't
     * registered.
     */
    private void checkAccess(Member member, Rally rally, boolean mustBeMaster) throws NotFoundException {
        if (rally.getIsPublic()) {
            return;
        }

        List<RallyParticipant> participants = rally.getParticipants();
        if (participants != null) {
            for (RallyParticipant participant : participants) {
                if (participant.getMemberId().equals(member.getId()) &&
                        (!mustBeMaster || participant.isRallyMaster())
                ) {
                    return;
                }
            }
        }

        throw new NotFoundException("Rally not found");
    }

    //======================================================================
    // Produce the fetch spec for rally search.
    //======================================================================

    private static Specification<Rally> nameContains(String name) {
        if (name == null || name.isBlank()) return null;
        final String like = "%" + name.toLowerCase() + "%";
        return (root, q, cb) -> cb.like(cb.lower(root.get("name")), like);
    }


    /**
     * Inclusive overlap with [from, to]:
     * r.startDate <= to AND r.endDate >= from
     * Handles single-ended ranges too.
     */
    private static Specification<Rally> dateOverlaps(LocalDate from, LocalDate to) {
        if (from == null && to == null) return null;
        return (root, q, cb) -> {
            Path<LocalDate> start = root.get("startDate");
            Path<LocalDate> end   = root.get("endDate");

            if (from != null && to != null) {
                return cb.and(
                        cb.lessThanOrEqualTo(start, to),
                        cb.greaterThanOrEqualTo(end, from)
                );
            } else if (from != null) {
                // anything that ends on/after 'from'
                return cb.greaterThanOrEqualTo(end, from);
            } else {
                // to != null: anything that starts on/before 'to'
                return cb.lessThanOrEqualTo(start, to);
            }
        };
    }

    private static Specification<Rally> countryMatches(String countryInput) {
        if (countryInput == null || countryInput.isBlank()) return null;
        String norm = normalizeCountry(countryInput);

        return (root, q, cb) -> {
            // assume entity has: countryCode (e.g., "US") and countryName (e.g., "United States")
            Expression<String> code = cb.upper(root.get("countryCode"));
            Expression<String> name = cb.upper(root.get("countryName"));
            String like = "%" + norm.toUpperCase() + "%";

            // match by exact code OR contains in name
            return cb.or(
                    cb.equal(code, norm.toUpperCase()),
                    cb.like(name, like)
            );
        };
    }

    private static Specification<Rally> regionMatches(String regionInput) {
        if (regionInput == null || regionInput.isBlank()) return null;
        String like = "%" + regionInput.toLowerCase() + "%";
        return (root, q, cb) -> cb.like(cb.lower(root.get("region")), like);
    }

    /**
     * Portable “no PostGIS” radius using:
     * 1) fast bounding box to leverage indexes
     * 2) precise Haversine predicate with SQL trig functions
     *
     * Assumes entity has latitude (double) and longitude (double).
     * Works on PostgreSQL/MySQL/MariaDB where COS/SIN/ACOS/RADIANS exist.
     */
    private static Specification<Rally> withinRadius(Double lat, Double lng, Double radiusMiles) {
        if (lat == null || lng == null || radiusMiles == null || radiusMiles <= 0) return null;

        // 1) bounding box in degrees
        double milesPerDegLat = 69.0;           // approx
        double milesPerDegLng = 69.172 * Math.cos(Math.toRadians(lat)); // varies with latitude
        double dLat = radiusMiles / milesPerDegLat;
        double dLng = radiusMiles / Math.max(0.000001, milesPerDegLng);

        double minLat = lat - dLat, maxLat = lat + dLat;
        double minLng = lng - dLng, maxLng = lng + dLng;

        return (root, q, cb) -> {
            Path<Double> rLat = root.get("latitude");
            Path<Double> rLng = root.get("longitude");

            // bounding box
            Predicate box = cb.and(
                    cb.between(rLat, minLat, maxLat),
                    cb.between(rLng, minLng, maxLng)
            );

            // 2) precise Haversine (in miles): 3958.7613 * acos(…)
            // Build with SQL functions via CriteriaBuilder.function
            Expression<Double> latRad   = cb.function("radians", Double.class, rLat);
            Expression<Double> lngRad   = cb.function("radians", Double.class, rLng);
            Expression<Double> pLatRad  = cb.function("radians", Double.class, cb.literal(lat));
            Expression<Double> pLngRad  = cb.function("radians", Double.class, cb.literal(lng));

            Expression<Double> cosLat   = cb.function("cos", Double.class, pLatRad);
            Expression<Double> sinLat   = cb.function("sin", Double.class, pLatRad);
            Expression<Double> cosRL    = cb.function("cos", Double.class, latRad);
            Expression<Double> sinRL    = cb.function("sin", Double.class, latRad);

            Expression<Double> dLngRad  = cb.diff(lngRad, pLngRad);
            Expression<Double> cosDLng  = cb.function("cos", Double.class, dLngRad);

            Expression<Double> acosArg = cb.sum(
                    cb.prod(cb.prod(cosLat, cosRL), cosDLng),
                    cb.prod(sinLat, sinRL)
            );

            // Clamp to [-1, 1] to avoid NaNs from rounding
            Expression<Double> one    = cb.literal(1.0);
            Expression<Double> negOne = cb.literal(-1.0);

            // CASE WHEN acosArg > 1 THEN 1 WHEN acosArg < -1 THEN -1 ELSE acosArg END
            Expression<Double> clamped = cb.<Double>selectCase()
                    .when(cb.gt(acosArg, one), one)
                    .when(cb.lt(acosArg, negOne), negOne)
                    .otherwise(acosArg);

            // angle = acos(clamped)
            Expression<Double> angle = cb.function("acos", Double.class, clamped);

            // miles = 3958.7613 * angle
            Expression<Double> miles = cb.prod(cb.literal(3958.7613), angle);

            Predicate circle = cb.le(miles, radiusMiles);
            return cb.and(box, circle);
        };
    }

    // --- helpers ---

    private static String normalizeCountry(String raw) {
        if (raw == null) return null;
        String s = raw.trim().toUpperCase();

        // minimal normalizer; extend as needed
        switch (s) {
            case "USA": case "U.S.A.": case "UNITED STATES": case "UNITED STATES OF AMERICA": case "AMERICA":
                return "US";
            case "UK": case "U.K.": case "GREAT BRITAIN": case "BRITAIN":
                return "GB";
            case "CAN": case "CA": case "CANADA":
                return "CA";
            case "AUS": case "AUSTRALIA": case "AUS.": case "AU":
                return "AU";
            default:
                // allow passing through 2–3 letter codes or names
                return s;
        }
    }
}
