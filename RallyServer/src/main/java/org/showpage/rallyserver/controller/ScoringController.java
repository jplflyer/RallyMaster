package org.showpage.rallyserver.controller;

import lombok.RequiredArgsConstructor;
import org.showpage.rallyserver.RestResponse;
import org.showpage.rallyserver.entity.EarnedBonusPoint;
import org.showpage.rallyserver.entity.EarnedCombination;
import org.showpage.rallyserver.service.DtoMapper;
import org.showpage.rallyserver.service.ScoringService;
import org.showpage.rallyserver.ui.*;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Controller for scoring operations during rallies.
 */
@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class ScoringController {
    private final ServiceCaller serviceCaller;
    private final ScoringService scoringService;

    /**
     * Update starting odometer for a rider.
     */
    @PutMapping("/rally/{rallyId}/odometer/start")
    ResponseEntity<RestResponse<UiRallyParticipant>> updateStartingOdometer(
            @PathVariable Integer rallyId,
            @RequestBody UpdateOdometerRequest request
    ) {
        return serviceCaller.call((member) ->
                DtoMapper.toUiRallyParticipant(scoringService.updateStartingOdometer(member, rallyId, request)));
    }

    /**
     * Update ending odometer for a rider.
     */
    @PutMapping("/rally/{rallyId}/odometer/end")
    ResponseEntity<RestResponse<UiRallyParticipant>> updateEndingOdometer(
            @PathVariable Integer rallyId,
            @RequestBody UpdateOdometerRequest request
    ) {
        return serviceCaller.call((member) ->
                DtoMapper.toUiRallyParticipant(scoringService.updateEndingOdometer(member, rallyId, request)));
    }

    /**
     * Create an earned bonus point entry.
     */
    @PostMapping("/rally/{rallyId}/earned-bonus-point")
    ResponseEntity<RestResponse<UiEarnedBonusPoint>> createEarnedBonusPoint(
            @PathVariable Integer rallyId,
            @RequestBody CreateEarnedBonusPointRequest request
    ) {
        return serviceCaller.call((member) ->
                toUiEarnedBonusPoint(scoringService.createEarnedBonusPoint(member, rallyId, request)));
    }

    /**
     * Update the confirmed flag on an earned bonus point.
     */
    @PutMapping("/earned-bonus-point/{id}/confirm")
    ResponseEntity<RestResponse<UiEarnedBonusPoint>> updateEarnedBonusPointConfirmation(
            @PathVariable Integer id,
            @RequestParam Boolean confirmed
    ) {
        return serviceCaller.call((member) ->
                toUiEarnedBonusPoint(scoringService.updateEarnedBonusPointConfirmation(member, id, confirmed)));
    }

    /**
     * Create an earned combination entry.
     */
    @PostMapping("/rally/{rallyId}/earned-combination")
    ResponseEntity<RestResponse<UiEarnedCombination>> createEarnedCombination(
            @PathVariable Integer rallyId,
            @RequestBody CreateEarnedCombinationRequest request
    ) {
        return serviceCaller.call((member) ->
                toUiEarnedCombination(scoringService.createEarnedCombination(member, rallyId, request)));
    }

    /**
     * Update the confirmed flag on an earned combination.
     */
    @PutMapping("/earned-combination/{id}/confirm")
    ResponseEntity<RestResponse<UiEarnedCombination>> updateEarnedCombinationConfirmation(
            @PathVariable Integer id,
            @RequestParam Boolean confirmed
    ) {
        return serviceCaller.call((member) ->
                toUiEarnedCombination(scoringService.updateEarnedCombinationConfirmation(member, id, confirmed)));
    }

    /**
     * Get all earned bonus points for a rally participant.
     */
    @GetMapping("/rally-participant/{rallyParticipantId}/earned-bonus-points")
    ResponseEntity<RestResponse<List<UiEarnedBonusPoint>>> getEarnedBonusPoints(
            @PathVariable Integer rallyParticipantId
    ) {
        return serviceCaller.call((member) ->
                scoringService.getEarnedBonusPoints(member, rallyParticipantId).stream()
                        .map(this::toUiEarnedBonusPoint)
                        .toList());
    }

    /**
     * Get all earned combinations for a rally participant.
     */
    @GetMapping("/rally-participant/{rallyParticipantId}/earned-combinations")
    ResponseEntity<RestResponse<List<UiEarnedCombination>>> getEarnedCombinations(
            @PathVariable Integer rallyParticipantId
    ) {
        return serviceCaller.call((member) ->
                scoringService.getEarnedCombinations(member, rallyParticipantId).stream()
                        .map(this::toUiEarnedCombination)
                        .toList());
    }

    //======================================================================
    // Helper methods for mapping entities to DTOs
    //======================================================================

    private UiEarnedBonusPoint toUiEarnedBonusPoint(EarnedBonusPoint earned) {
        if (earned == null) {
            return null;
        }
        return UiEarnedBonusPoint.builder()
                .id(earned.getId())
                .rallyParticipantId(earned.getRallyParticipantId())
                .bonusPointId(earned.getBonusPointId())
                .odometer(earned.getOdometer())
                .earnedAt(earned.getEarnedAt())
                .confirmed(earned.getConfirmed())
                .build();
    }

    private UiEarnedCombination toUiEarnedCombination(EarnedCombination earned) {
        if (earned == null) {
            return null;
        }
        return UiEarnedCombination.builder()
                .id(earned.getId())
                .rallyParticipantId(earned.getRallyParticipantId())
                .combinationId(earned.getCombinationId())
                .confirmed(earned.getConfirmed())
                .build();
    }
}
