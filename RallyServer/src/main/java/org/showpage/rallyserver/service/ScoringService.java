package org.showpage.rallyserver.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.showpage.rallyserver.entity.*;
import org.showpage.rallyserver.exception.NotFoundException;
import org.showpage.rallyserver.exception.ValidationException;
import org.showpage.rallyserver.repository.*;
import org.showpage.rallyserver.ui.CreateEarnedBonusPointRequest;
import org.showpage.rallyserver.ui.CreateEarnedCombinationRequest;
import org.showpage.rallyserver.ui.UpdateOdometerRequest;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Service for scoring operations during rallies.
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class ScoringService {
    private final RallyParticipantRepository rallyParticipantRepository;
    private final BonusPointRepository bonusPointRepository;
    private final CombinationRepository combinationRepository;
    private final EarnedBonusPointRepository earnedBonusPointRepository;
    private final EarnedCombinationRepository earnedCombinationRepository;

    /**
     * Update starting odometer for a rider. Can be done by scorer (ORGANIZER/AIDE) or by the rider themselves.
     */
    public RallyParticipant updateStartingOdometer(Member currentMember, Integer rallyId, UpdateOdometerRequest request)
            throws NotFoundException, ValidationException
    {
        validateRequest(request);
        checkIsScorerForRally(currentMember, rallyId);

        RallyParticipant participant = rallyParticipantRepository.getRiderForRally(rallyId, request.getRiderId());

        participant.setOdometerIn(request.getOdometer());
        return rallyParticipantRepository.save(participant);
    }

    /**
     * Update ending odometer for a rider. Can be done by scorer (ORGANIZER/AIDE) or by the rider themselves.
     */
    public RallyParticipant updateEndingOdometer(Member currentMember, Integer rallyId, UpdateOdometerRequest request)
            throws NotFoundException, ValidationException
    {
        validateRequest(request);
        checkIsScorerForRally(currentMember, rallyId);

        RallyParticipant participant = rallyParticipantRepository.getRiderForRally(rallyId, request.getRiderId());

        participant.setOdometerOut(request.getOdometer());
        return rallyParticipantRepository.save(participant);
    }

    /**
     * Create an earned bonus point entry. Can be done by scorer (ORGANIZER/AIDE) or by the rider themselves.
     */
    public EarnedBonusPoint createEarnedBonusPoint(Member currentMember, Integer rallyId, CreateEarnedBonusPointRequest request)
            throws NotFoundException, ValidationException
    {
        int riderId = request.getRiderId() != null ? request.getRiderId() : currentMember.getId();
        RallyParticipant participant = rallyParticipantRepository.getRiderForRally(rallyId, riderId);

        // Permission check: must be scorer (ORGANIZER/AIDE) or the rider themselves
        checkScoringPermission(currentMember, participant);

        BonusPoint bonusPoint = bonusPointRepository.findById_WithThrow(request.getBonusPointId());

        // Verify bonus point belongs to this rally
        if (!bonusPoint.getRallyId().equals(rallyId)) {
            throw new ValidationException("Bonus point does not belong to this rally");
        }

        // Verify they don't already have one.
        if (!earnedBonusPointRepository.findByRallyParticipantIdAndBonusPointId(rallyId, bonusPoint.getId()).isEmpty()) {
            throw new ValidationException("This earned bonus point already exists");
        }

        EarnedBonusPoint earned = EarnedBonusPoint
                .builder()
                .rallyParticipant(participant)
                .bonusPoint(bonusPoint)
                .bonusPointId(bonusPoint.getId())
                .odometer(request.getOdometer())
                .earnedAt(request.getEarnedAt())
                .confirmed(request.getConfirmed() != null ? request.getConfirmed() : false)
                .build();

        return earnedBonusPointRepository.save(earned);
    }

    /**
     * Update the confirmed flag on an earned bonus point. Only scorers (ORGANIZER/AIDE) can do this.
     */
    public EarnedBonusPoint updateEarnedBonusPointConfirmation(Member currentMember, Integer earnedBonusPointId, Boolean confirmed)
            throws NotFoundException, ValidationException {
        EarnedBonusPoint earned = earnedBonusPointRepository.findById(earnedBonusPointId)
                .orElseThrow(() -> new NotFoundException("Earned bonus point not found"));

        RallyParticipant participant = earned.getRallyParticipant();

        // Permission check: must be scorer (ORGANIZER/AIDE) only
        checkIsScorerForRally(currentMember, participant.getRallyId());

        earned.setConfirmed(confirmed);
        return earnedBonusPointRepository.save(earned);
    }

    /**
     * Create an earned combination entry. Can be done by scorer (ORGANIZER/AIDE) or by the rider themselves.
     */
    public EarnedCombination createEarnedCombination(Member currentMember, Integer rallyId, CreateEarnedCombinationRequest request)
            throws NotFoundException, ValidationException
    {
        int riderId = request.getRiderId() != null ? request.getRiderId() : currentMember.getId();
        RallyParticipant participant = rallyParticipantRepository.findByRallyIdAndMemberId(rallyId, riderId)
                .orElseThrow(() -> new NotFoundException("Rider not registered for this rally"));

        // Permission check: must be scorer (ORGANIZER/AIDE) or the rider themselves
        checkScoringPermission(currentMember, participant);

        Combination combination = combinationRepository.findById(request.getCombinationId())
                .orElseThrow(() -> new NotFoundException("Combination not found"));

        // Verify combination belongs to this rally
        if (!combination.getRallyId().equals(rallyId)) {
            throw new ValidationException("Combination does not belong to this rally");
        }

        // We also set IDs. The repo ignores them but also doesn't populate them for us.
        // We want them when we return to the user, and it's harmless.
        EarnedCombination earned = EarnedCombination
                .builder()
                .rallyParticipant(participant)
                .rallyParticipantId(participant.getId())
                .combination(combination)
                .combinationId(combination.getId())
                .confirmed(request.getConfirmed() != null ? request.getConfirmed() : false)
                .build();

        return earnedCombinationRepository.save(earned);
    }

    /**
     * Update the confirmed flag on an earned combination. Only scorers (ORGANIZER/AIDE) can do this.
     */
    public EarnedCombination updateEarnedCombinationConfirmation(Member currentMember, Integer earnedCombinationId, Boolean confirmed)
            throws NotFoundException, ValidationException {
        EarnedCombination earned = earnedCombinationRepository.findById(earnedCombinationId)
                .orElseThrow(() -> new NotFoundException("Earned combination not found"));

        RallyParticipant participant = earned.getRallyParticipant();

        // Permission check: must be scorer (ORGANIZER/AIDE) only
        checkIsScorerForRally(currentMember, participant.getRallyId());

        earned.setConfirmed(confirmed);
        return earnedCombinationRepository.save(earned);
    }

    /**
     * Get all earned bonus points for a rider.
     */
    public List<EarnedBonusPoint> getEarnedBonusPoints(Member currentMember, Integer rallyParticipantId)
            throws NotFoundException, ValidationException {
        RallyParticipant participant = rallyParticipantRepository.findById(rallyParticipantId)
                .orElseThrow(() -> new NotFoundException("Rally participant not found"));

        // Permission check: must be scorer (ORGANIZER/AIDE) or the rider themselves
        checkScoringPermission(currentMember, participant);

        return earnedBonusPointRepository.findByRallyParticipantId(rallyParticipantId);
    }

    /**
     * Get all earned combinations for a rider.
     */
    public List<EarnedCombination> getEarnedCombinations(Member currentMember, Integer rallyParticipantId)
            throws NotFoundException, ValidationException {
        RallyParticipant participant = rallyParticipantRepository.findById(rallyParticipantId)
                .orElseThrow(() -> new NotFoundException("Rally participant not found"));

        // Permission check: must be scorer (ORGANIZER/AIDE) or the rider themselves
        checkScoringPermission(currentMember, participant);

        return earnedCombinationRepository.findByRallyParticipantId(rallyParticipantId);
    }

    //======================================================================
    // Permission Helpers
    //======================================================================

    /**
     * Make sure this is a valid request.
     */
    private void validateRequest(UpdateOdometerRequest request) throws ValidationException {
        if (request == null) {
            throw new ValidationException("Request is null");
        }
        if (request.getRiderId() == null || request.getRiderId() == 0) {
            throw new ValidationException("RIDER ID is required");
        }
        if (request.getOdometer() == null || request.getOdometer() == 0) {
            throw new ValidationException("ODOMETER is required");
        }
    }


    /**
     * Check if the current member can perform scoring operations for the given participant.
     * This is true if:
     * 1. The current member is the rider themselves, OR
     * 2. The current member is an ORGANIZER or AIDE for the rally
     */
    private void checkScoringPermission(Member currentMember, RallyParticipant targetParticipant) throws ValidationException {
        // Is this the rider themselves?
        if (currentMember.getId().equals(targetParticipant.getMemberId())) {
            return;
        }

        // Is this member a scorer for the rally?
        checkIsScorerForRally(currentMember, targetParticipant.getRallyId());
    }

    /**
     * Check if the current member is a scorer (ORGANIZER or AIDE) for the given rally.
     */
    private void checkIsScorerForRally(Member currentMember, Integer rallyId) throws ValidationException {
        RallyParticipant currentParticipant = rallyParticipantRepository.findByRallyIdAndMemberId(rallyId, currentMember.getId())
                .orElseThrow(() -> new ValidationException("Not authorized to score for this rally"));

        RallyParticipantType type = currentParticipant.getParticipantType();
        if (type != RallyParticipantType.ORGANIZER && type != RallyParticipantType.AIDE) {
            throw new ValidationException("Only organizers and aides can perform this operation");
        }
    }
}
