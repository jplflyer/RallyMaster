package org.showpage.rallyserver.service;

import jakarta.persistence.criteria.Expression;
import jakarta.persistence.criteria.Path;
import jakarta.persistence.criteria.Predicate;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.showpage.rallyserver.entity.*;
import org.showpage.rallyserver.repository.*;
import org.showpage.rallyserver.exception.NotFoundException;
import org.showpage.rallyserver.exception.ValidationException;
import org.showpage.rallyserver.ui.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.PageImpl;
import org.showpage.rallyserver.util.DataValidator;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

/**
 * CRUD on Rallies -- but not on riding one.
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class RallyService {
    private final RallyRepository rallyRepository;
    private final RallyParticipantRepository rallyParticipantRepository;
    private final BonusPointRepository bonusPointRepository;
    private final CombinationRepository combinationRepository;
    private final CombinationPointRepository combinationPointRepository;
    private final EarnedCombinationRepository earnedCombinationRepository;
    private final EarnedBonusPointRepository earnedBonusPointRepository;

    @Value("${rallymaster.options.can-delete-rallies:false}")
    private boolean canDeleteRallies;

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
                .latitude(request.getLatitude())
                .longitude(request.getLongitude())
                .locationCity(request.getLocationCity().trim())
                .locationState(request.getLocationState() != null ? request.getLocationState().trim() : null)
                .locationCountry(request.getLocationCountry().trim())
                .startDate(request.getStartDate())
                .endDate(request.getEndDate())
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

        if (request.getLatitude() != null) {
            rally.setLatitude(request.getLatitude());
        }
        if (request.getLongitude() != null) {
            rally.setLongitude(request.getLongitude());
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
     * Delete a Rally. You can't do this if there are participants unless the flag is set (which it will
     * be in dev mode but not prod).
     */
    public boolean deleteRally(Member member, Integer rallyId) throws NotFoundException, ValidationException {
        Rally rally = rallyRepository.findById(rallyId)
                .orElseThrow(() -> new NotFoundException("Rally not found"));
        checkAccess(member, rally, true);

        if (rally.getParticipants() != null && rally.getParticipants().size() > 1 && !canDeleteRallies) {
            throw new ValidationException("You can not delete a rally once people have signed up");
        }

        // We get weird issues trying to delete with cascade, so we're going to cascade directly.

        for (RallyParticipant participant : rally.getParticipants()) {
            earnedCombinationRepository.deleteAll(participant.getEarnedCombinations());
            earnedBonusPointRepository.deleteAll(participant.getEarnedBonusPoints());
        }
        rallyParticipantRepository.deleteAll(rally.getParticipants());

        for (Combination combination : rally.getCombinations()) {
            combinationPointRepository.deleteAll(combination.getCombinationPoints());
        }
        combinationRepository.deleteAll(rally.getCombinations());

        bonusPointRepository.deleteAll(rally.getBonusPoints());

        rallyRepository.delete(rally);

        return true;
    }

    /**
     * Register a rider for a rally. Prevents duplicate registration.
     */
    public RallyParticipant registerRider(Member member, Integer rallyId) throws NotFoundException, ValidationException {
        Rally rally = rallyRepository.findById(rallyId)
                .orElseThrow(() -> new NotFoundException("Rally not found"));

        // Check if rider is already registered
        Optional<RallyParticipant> existing = rallyParticipantRepository.findByRallyIdAndMemberId(rallyId, member.getId());
        if (existing.isPresent()) {
            throw new ValidationException("Member is already registered for this rally");
        }

        RallyParticipant participant = RallyParticipant.builder()
                .rally(rally)
                .member(member)
                .participantType(RallyParticipantType.RIDER)
                .build();

        return rallyParticipantRepository.save(participant);
    }

    /**
     * Promote a rider to ORGANIZER or AIDE. Only existing organizers can do this.
     */
    public RallyParticipant promoteParticipant(Member member, Integer rallyId, Integer targetMemberId, RallyParticipantType newType)
            throws NotFoundException, ValidationException {
        if (newType == RallyParticipantType.RIDER) {
            throw new ValidationException("Cannot promote to RIDER type. Use this endpoint only for ORGANIZER or AIDE.");
        }

        Rally rally = rallyRepository.findById(rallyId)
                .orElseThrow(() -> new NotFoundException("Rally not found"));

        // Check that the current member is an organizer for this rally
        checkAccess(member, rally, true);

        // Find the target participant
        RallyParticipant targetParticipant = rallyParticipantRepository.findByRallyIdAndMemberId(rallyId, targetMemberId)
                .orElseThrow(() -> new NotFoundException("Target member is not registered for this rally"));

        targetParticipant.setParticipantType(newType);
        return rallyParticipantRepository.save(targetParticipant);
    }

    //======================================================================
    // BonusPoint CRUD
    //======================================================================

    /**
     * Create a bonus point for a rally. Only organizers can do this.
     */
    public BonusPoint createBonusPoint(Member member, Integer rallyId, CreateBonusPointRequest request)
            throws NotFoundException, ValidationException {
        Rally rally = rallyRepository.findById(rallyId)
                .orElseThrow(() -> new NotFoundException("Rally not found"));
        checkAccess(member, rally, true);

        BonusPoint bonusPoint = BonusPoint
                .builder()
                .rally(rally)
                .rallyId(rallyId)
                .code(request.getCode())
                .name(request.getName())
                .description(request.getDescription())
                .latitude(request.getLatitude())
                .longitude(request.getLongitude())
                .address(request.getAddress())
                .points(request.getPoints())
                .required(request.getRequired() != null ? request.getRequired() : false)
                .repeatable(request.getRepeatable() != null ? request.getRepeatable() : false)
                .build();

        return bonusPointRepository.save(bonusPoint);
    }

    /**
     * Update a bonus point. Only organizers can do this.
     */
    public BonusPoint updateBonusPoint(Member member, Integer bonusPointId, UpdateBonusPointRequest request)
            throws NotFoundException, ValidationException {
        BonusPoint bonusPoint = bonusPointRepository.findById(bonusPointId)
                .orElseThrow(() -> new NotFoundException("Bonus point not found"));

        Rally rally = rallyRepository.findById(bonusPoint.getRallyId())
                .orElseThrow(() -> new NotFoundException("Rally not found"));
        checkAccess(member, rally, true);

        if (DataValidator.nonEmpty(request.getCode())) {
            bonusPoint.setCode(request.getCode());
        }
        if (DataValidator.nonEmpty(request.getName())) {
            bonusPoint.setName(request.getName());
        }
        if (DataValidator.nonEmpty(request.getDescription())) {
            bonusPoint.setDescription(request.getDescription());
        }
        if (request.getLatitude() != null) {
            bonusPoint.setLatitude(request.getLatitude());
        }
        if (request.getLongitude() != null) {
            bonusPoint.setLongitude(request.getLongitude());
        }
        if (DataValidator.nonEmpty(request.getAddress())) {
            bonusPoint.setAddress(request.getAddress());
        }
        if (request.getPoints() != null) {
            bonusPoint.setPoints(request.getPoints());
        }
        if (request.getRequired() != null) {
            bonusPoint.setRequired(request.getRequired());
        }
        if (request.getRepeatable() != null) {
            bonusPoint.setRepeatable(request.getRepeatable());
        }

        return bonusPointRepository.save(bonusPoint);
    }

    /**
     * Delete a bonus point. Only organizers can do this.
     */
    public void deleteBonusPoint(Member member, Integer bonusPointId) throws NotFoundException, ValidationException {
        BonusPoint bonusPoint = bonusPointRepository.findById(bonusPointId)
                .orElseThrow(() -> new NotFoundException("Bonus point not found"));

        Rally rally = rallyRepository.findById(bonusPoint.getRallyId())
                .orElseThrow(() -> new NotFoundException("Rally not found"));
        checkAccess(member, rally, true);

        bonusPointRepository.delete(bonusPoint);
    }

    /**
     * Get a bonus point by ID.
     */
    public BonusPoint getBonusPoint(Member member, Integer bonusPointId) throws NotFoundException {
        BonusPoint bonusPoint = bonusPointRepository.findById(bonusPointId)
                .orElseThrow(() -> new NotFoundException("Bonus point not found"));

        Rally rally = rallyRepository.findById(bonusPoint.getRallyId())
                .orElseThrow(() -> new NotFoundException("Rally not found"));
        checkAccess(member, rally, false);

        return bonusPoint;
    }

    /**
     * List all bonus points for a rally.
     */
    public List<BonusPoint> listBonusPoints(Member member, Integer rallyId) throws NotFoundException {
        Rally rally = rallyRepository.findById(rallyId)
                .orElseThrow(() -> new NotFoundException("Rally not found"));
        checkAccess(member, rally, false);

        return bonusPointRepository.findByRallyId(rallyId);
    }

    //======================================================================
    // Combination CRUD
    //======================================================================

    /**
     * Create a combination for a rally. Only organizers can do this.
     */
    public Combination createCombination(Member member, Integer rallyId, CreateCombinationRequest request)
            throws NotFoundException, ValidationException
    {
        Rally rally = rallyRepository.findById(rallyId)
                .orElseThrow(() -> new NotFoundException("Rally not found"));
        checkAccess(member, rally, true);

        Combination combination = Combination
                .builder()
                .rally(rally)
                .rallyId(rallyId)
                .code(request.getCode())
                .name(request.getName())
                .description(request.getDescription())
                .points(request.getPoints())
                .requiresAll(request.getRequiresAll())
                .numRequired(request.getNumRequired())
                .build();

        Combination saved = combinationRepository.save(combination);
        log.info("Created combination: {}", saved.getId());

        // Create the combination points
        if (request.getCombinationPoints() != null) {
            for (CreateCombinationPointRequest cpRequest : request.getCombinationPoints()) {
                BonusPoint bonusPoint = bonusPointRepository.findById(cpRequest.getBonusPointId())
                        .orElseThrow(() -> new NotFoundException("Bonus point not found: " + cpRequest.getBonusPointId()));

                CombinationPoint cp = CombinationPoint
                        .builder()
                        .combination(saved)
                        .combinationId(saved.getId())
                        .bonusPoint(bonusPoint)
                        .bonusPointId(bonusPoint.getId())
                        .required(cpRequest.getRequired())
                        .build();

                cp = combinationPointRepository.save(cp);
                saved.addCombinationPoint(cp);
            }
            log.info("Created {} combination points. Saved shows {}",
                    request.getCombinationPoints().size(),
                    saved.getCombinationPoints() != null ? saved.getCombinationPoints().size() : 0);
        }

        return saved;
    }

    /**
     * Update a combination. Only organizers can do this.
     */
    public Combination updateCombination(Member member, Integer combinationId, UpdateCombinationRequest request)
            throws NotFoundException, ValidationException {
        Combination combination = combinationRepository.findById(combinationId)
                .orElseThrow(() -> new NotFoundException("Combination not found"));

        Rally rally = rallyRepository.findById(combination.getRallyId())
                .orElseThrow(() -> new NotFoundException("Rally not found"));
        checkAccess(member, rally, true);

        if (DataValidator.nonEmpty(request.getCode())) {
            combination.setCode(request.getCode());
        }
        if (DataValidator.nonEmpty(request.getName())) {
            combination.setName(request.getName());
        }
        if (DataValidator.nonEmpty(request.getDescription())) {
            combination.setDescription(request.getDescription());
        }
        if (request.getPoints() != null) {
            combination.setPoints(request.getPoints());
        }
        if (request.getRequiresAll() != null) {
            combination.setRequiresAll(request.getRequiresAll());
        }
        if (request.getNumRequired() != null) {
            combination.setNumRequired(request.getNumRequired());
        }

        return combinationRepository.save(combination);
    }

    /**
     * Delete a combination. Only organizers can do this.
     */
    public void deleteCombination(Member member, Integer combinationId) throws NotFoundException, ValidationException {
        Combination combination = combinationRepository.findById(combinationId)
                .orElseThrow(() -> new NotFoundException("Combination not found"));

        Rally rally = rallyRepository.findById(combination.getRallyId())
                .orElseThrow(() -> new NotFoundException("Rally not found"));
        checkAccess(member, rally, true);

        // Delete all combination points first
        List<CombinationPoint> points = combinationPointRepository.findByCombinationId(combinationId);
        combinationPointRepository.deleteAll(points);

        combinationRepository.delete(combination);
    }

    /**
     * Get a combination by ID.
     */
    public Combination getCombination(Member member, Integer combinationId) throws NotFoundException {
        Combination combination = combinationRepository.findById(combinationId)
                .orElseThrow(() -> new NotFoundException("Combination not found"));

        Rally rally = rallyRepository.findById(combination.getRallyId())
                .orElseThrow(() -> new NotFoundException("Rally not found"));
        checkAccess(member, rally, false);

        return combination;
    }

    /**
     * List all combinations for a rally.
     */
    public List<Combination> listCombinations(Member member, Integer rallyId) throws NotFoundException {
        Rally rally = rallyRepository.findById(rallyId)
                .orElseThrow(() -> new NotFoundException("Rally not found"));
        checkAccess(member, rally, false);

        return combinationRepository.findByRallyId(rallyId);
    }

    //======================================================================
    // CombinationPoint CRUD
    //======================================================================

    /**
     * Add a bonus point to a combination. Only organizers can do this.
     */
    public CombinationPoint addCombinationPoint(Member member, Integer combinationId, CreateCombinationPointRequest request)
            throws NotFoundException, ValidationException {
        Combination combination = combinationRepository.findById(combinationId)
                .orElseThrow(() -> new NotFoundException("Combination not found"));

        Rally rally = rallyRepository.findById(combination.getRallyId())
                .orElseThrow(() -> new NotFoundException("Rally not found"));
        checkAccess(member, rally, true);

        BonusPoint bonusPoint = bonusPointRepository.findById(request.getBonusPointId())
                .orElseThrow(() -> new NotFoundException("Bonus point not found"));

        CombinationPoint cp = new CombinationPoint();
        cp.setCombination(combination);
        cp.setCombinationId(combination.getId());  // Manually set IDs since they're insertable=false, updatable=false
        cp.setBonusPoint(bonusPoint);
        cp.setBonusPointId(bonusPoint.getId());
        cp.setRequired(request.getRequired());

        return combinationPointRepository.save(cp);
    }

    /**
     * Remove a bonus point from a combination. Only organizers can do this.
     */
    public void deleteCombinationPoint(Member member, Integer combinationPointId) throws NotFoundException, ValidationException {
        CombinationPoint cp = combinationPointRepository.findById(combinationPointId)
                .orElseThrow(() -> new NotFoundException("Combination point not found"));

        Combination combination = combinationRepository.findById(cp.getCombinationId())
                .orElseThrow(() -> new NotFoundException("Combination not found"));

        Rally rally = rallyRepository.findById(combination.getRallyId())
                .orElseThrow(() -> new NotFoundException("Rally not found"));
        checkAccess(member, rally, true);

        combinationPointRepository.delete(cp);
    }

    /**
     * List all combination points for a combination.
     */
    public List<CombinationPoint> listCombinationPoints(Member member, Integer combinationId) throws NotFoundException {
        Combination combination = combinationRepository.findById(combinationId)
                .orElseThrow(() -> new NotFoundException("Combination not found"));

        Rally rally = rallyRepository.findById(combination.getRallyId())
                .orElseThrow(() -> new NotFoundException("Rally not found"));
        checkAccess(member, rally, false);

        return combinationPointRepository.findByCombinationId(combinationId);
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

          boolean all,
          Pageable pageable
    ){
        Specification<Rally> spec = Specification.<Rally>unrestricted()
                .and(nameContains(name))
                .and(dateOverlaps(from, to))
                .and(countryMatches(country))
                .and(regionMatches(region))
                .and(withinRadius(nearLat, nearLng, radiusMiles));

        if (all) {
            List<Rally> list = rallyRepository.findAll(spec); // unpaged
            return new PageImpl<>(list, Pageable.unpaged(), list.size());
        }
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
            Expression<String> name = cb.upper(root.get("locationCountry"));
            String like = "%" + norm.toUpperCase() + "%";

            // match by exact code OR contains in name
            return cb.or(
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
