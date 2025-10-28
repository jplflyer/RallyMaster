package org.showpage.rallyserver.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
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
@Tag(name = "Rally Scoring", description = "Endpoints for managing rally scoring, odometer readings, earned bonus points, and earned combinations")
public class ScoringController {
    private final ServiceCaller serviceCaller;
    private final ScoringService scoringService;

    @Operation(
        summary = "Update starting odometer",
        description = "Record the starting odometer reading for a rider in a rally. This is typically done at rally check-in.",
        responses = {
            @ApiResponse(responseCode = "200", description = "Starting odometer updated successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid odometer value"),
            @ApiResponse(responseCode = "401", description = "Not authenticated"),
            @ApiResponse(responseCode = "403", description = "Not authorized to update odometer for this rally"),
            @ApiResponse(responseCode = "404", description = "Rally or participant not found")
        }
    )
    @PutMapping("/rally/{rallyId}/odometer/start")
    ResponseEntity<RestResponse<UiRallyParticipant>> updateStartingOdometer(
            @Parameter(description = "Rally ID", example = "1", required = true)
            @PathVariable Integer rallyId,
            @io.swagger.v3.oas.annotations.parameters.RequestBody(description = "Odometer reading request including the rider's member ID and odometer value", required = true)
            @RequestBody UpdateOdometerRequest request
    ) {
        return serviceCaller.call((member) ->
                DtoMapper.toUiRallyParticipant(scoringService.updateStartingOdometer(member, rallyId, request)));
    }

    @Operation(
        summary = "Update ending odometer",
        description = "Record the ending odometer reading for a rider in a rally. This is typically done at rally check-out or completion.",
        responses = {
            @ApiResponse(responseCode = "200", description = "Ending odometer updated successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid odometer value"),
            @ApiResponse(responseCode = "401", description = "Not authenticated"),
            @ApiResponse(responseCode = "403", description = "Not authorized to update odometer for this rally"),
            @ApiResponse(responseCode = "404", description = "Rally or participant not found")
        }
    )
    @PutMapping("/rally/{rallyId}/odometer/end")
    ResponseEntity<RestResponse<UiRallyParticipant>> updateEndingOdometer(
            @Parameter(description = "Rally ID", example = "1", required = true)
            @PathVariable Integer rallyId,
            @io.swagger.v3.oas.annotations.parameters.RequestBody(description = "Odometer reading request including the rider's member ID and odometer value", required = true)
            @RequestBody UpdateOdometerRequest request
    ) {
        return serviceCaller.call((member) ->
                DtoMapper.toUiRallyParticipant(scoringService.updateEndingOdometer(member, rallyId, request)));
    }

    @Operation(
        summary = "Record earned bonus point",
        description = "Record that a rider has earned a bonus point during a rally. Includes odometer reading and timestamp.",
        responses = {
            @ApiResponse(responseCode = "200", description = "Earned bonus point recorded successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid request or bonus point already earned"),
            @ApiResponse(responseCode = "401", description = "Not authenticated"),
            @ApiResponse(responseCode = "403", description = "Not authorized to record points for this rally"),
            @ApiResponse(responseCode = "404", description = "Rally, participant, or bonus point not found")
        }
    )
    @PostMapping("/rally/{rallyId}/earned-bonus-point")
    ResponseEntity<RestResponse<UiEarnedBonusPoint>> createEarnedBonusPoint(
            @Parameter(description = "Rally ID", example = "1", required = true)
            @PathVariable Integer rallyId,
            @io.swagger.v3.oas.annotations.parameters.RequestBody(description = "Earned bonus point details including rider, bonus point, odometer, and timestamp", required = true)
            @RequestBody CreateEarnedBonusPointRequest request
    ) {
        return serviceCaller.call((member) ->
                toUiEarnedBonusPoint(scoringService.createEarnedBonusPoint(member, rallyId, request)));
    }

    @Operation(
        summary = "Confirm/unconfirm earned bonus point",
        description = "Update the confirmation status of an earned bonus point. Rally organizers use this to verify submitted bonus points.",
        responses = {
            @ApiResponse(responseCode = "200", description = "Confirmation status updated successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid request"),
            @ApiResponse(responseCode = "401", description = "Not authenticated"),
            @ApiResponse(responseCode = "403", description = "Not authorized to confirm points for this rally"),
            @ApiResponse(responseCode = "404", description = "Earned bonus point not found")
        }
    )
    @PutMapping("/earned-bonus-point/{id}/confirm")
    ResponseEntity<RestResponse<UiEarnedBonusPoint>> updateEarnedBonusPointConfirmation(
            @Parameter(description = "Earned bonus point ID", example = "1", required = true)
            @PathVariable Integer id,
            @Parameter(description = "Confirmation status (true to confirm, false to unconfirm)", example = "true", required = true)
            @RequestParam Boolean confirmed
    ) {
        return serviceCaller.call((member) ->
                toUiEarnedBonusPoint(scoringService.updateEarnedBonusPointConfirmation(member, id, confirmed)));
    }

    @Operation(
        summary = "Record earned combination",
        description = "Record that a rider has completed a bonus point combination during a rally, earning extra points.",
        responses = {
            @ApiResponse(responseCode = "200", description = "Earned combination recorded successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid request or combination already earned"),
            @ApiResponse(responseCode = "401", description = "Not authenticated"),
            @ApiResponse(responseCode = "403", description = "Not authorized to record combinations for this rally"),
            @ApiResponse(responseCode = "404", description = "Rally, participant, or combination not found")
        }
    )
    @PostMapping("/rally/{rallyId}/earned-combination")
    ResponseEntity<RestResponse<UiEarnedCombination>> createEarnedCombination(
            @Parameter(description = "Rally ID", example = "1", required = true)
            @PathVariable Integer rallyId,
            @io.swagger.v3.oas.annotations.parameters.RequestBody(description = "Earned combination details including rider and combination ID", required = true)
            @RequestBody CreateEarnedCombinationRequest request
    ) {
        return serviceCaller.call((member) ->
                toUiEarnedCombination(scoringService.createEarnedCombination(member, rallyId, request)));
    }

    @Operation(
        summary = "Confirm/unconfirm earned combination",
        description = "Update the confirmation status of an earned combination. Rally organizers use this to verify submitted combinations.",
        responses = {
            @ApiResponse(responseCode = "200", description = "Confirmation status updated successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid request"),
            @ApiResponse(responseCode = "401", description = "Not authenticated"),
            @ApiResponse(responseCode = "403", description = "Not authorized to confirm combinations for this rally"),
            @ApiResponse(responseCode = "404", description = "Earned combination not found")
        }
    )
    @PutMapping("/earned-combination/{id}/confirm")
    ResponseEntity<RestResponse<UiEarnedCombination>> updateEarnedCombinationConfirmation(
            @Parameter(description = "Earned combination ID", example = "1", required = true)
            @PathVariable Integer id,
            @Parameter(description = "Confirmation status (true to confirm, false to unconfirm)", example = "true", required = true)
            @RequestParam Boolean confirmed
    ) {
        return serviceCaller.call((member) ->
                toUiEarnedCombination(scoringService.updateEarnedCombinationConfirmation(member, id, confirmed)));
    }

    @Operation(
        summary = "Get earned bonus points",
        description = "Retrieve all bonus points earned by a specific rally participant",
        responses = {
            @ApiResponse(responseCode = "200", description = "Earned bonus points retrieved successfully"),
            @ApiResponse(responseCode = "401", description = "Not authenticated"),
            @ApiResponse(responseCode = "403", description = "Not authorized to view this participant's points"),
            @ApiResponse(responseCode = "404", description = "Rally participant not found")
        }
    )
    @GetMapping("/rally-participant/{rallyParticipantId}/earned-bonus-points")
    ResponseEntity<RestResponse<List<UiEarnedBonusPoint>>> getEarnedBonusPoints(
            @Parameter(description = "Rally participant ID", example = "1", required = true)
            @PathVariable Integer rallyParticipantId
    ) {
        return serviceCaller.call((member) ->
                scoringService.getEarnedBonusPoints(member, rallyParticipantId).stream()
                        .map(this::toUiEarnedBonusPoint)
                        .toList());
    }

    @Operation(
        summary = "Get earned combinations",
        description = "Retrieve all bonus point combinations earned by a specific rally participant",
        responses = {
            @ApiResponse(responseCode = "200", description = "Earned combinations retrieved successfully"),
            @ApiResponse(responseCode = "401", description = "Not authenticated"),
            @ApiResponse(responseCode = "403", description = "Not authorized to view this participant's combinations"),
            @ApiResponse(responseCode = "404", description = "Rally participant not found")
        }
    )
    @GetMapping("/rally-participant/{rallyParticipantId}/earned-combinations")
    ResponseEntity<RestResponse<List<UiEarnedCombination>>> getEarnedCombinations(
            @Parameter(description = "Rally participant ID", example = "1", required = true)
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
