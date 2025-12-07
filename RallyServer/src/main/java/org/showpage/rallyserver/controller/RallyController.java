package org.showpage.rallyserver.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.showpage.rallyserver.RestResponse;
import org.showpage.rallyserver.repository.BonusPointRepository;
import org.showpage.rallyserver.repository.CombinationPointRepository;
import org.showpage.rallyserver.repository.CombinationRepository;
import org.showpage.rallyserver.service.DtoMapper;
import org.showpage.rallyserver.service.RallyService;
import org.showpage.rallyserver.ui.*;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Rally Management", description = "Endpoints for managing rallies, bonus points, combinations, and participant registration")
public class RallyController {
    private final ServiceCaller serviceCaller;
    private final RallyService rallyService;

    @Operation(
        summary = "Create a new rally",
        description = "Register a new motorcycle rally event. The authenticated user becomes the rally owner/organizer.",
        responses = {
            @ApiResponse(responseCode = "200", description = "Rally created successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid rally data"),
            @ApiResponse(responseCode = "401", description = "Not authenticated")
        }
    )
    @PostMapping("/rally")
    ResponseEntity<RestResponse<UiRally>> createRally(
            @io.swagger.v3.oas.annotations.parameters.RequestBody(description = "Rally details including name, dates, location, and description", required = true)
            @RequestBody CreateRallyRequest request
    ) {
        return serviceCaller.call("/api/rally/", (member) ->
            DtoMapper.toUiRally(member, rallyService.createRally(member, request)));
    }

    @Operation(
        summary = "Update an existing rally",
        description = "Update rally details. Only the rally organizer can update rally information.",
        responses = {
            @ApiResponse(responseCode = "200", description = "Rally updated successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid rally data"),
            @ApiResponse(responseCode = "401", description = "Not authenticated"),
            @ApiResponse(responseCode = "403", description = "Not authorized to update this rally"),
            @ApiResponse(responseCode = "404", description = "Rally not found")
        }
    )
    @PutMapping("/rally/{id}")
    ResponseEntity<RestResponse<UiRally>> updateRally(
            @Parameter(description = "Rally ID", example = "1", required = true)
            @PathVariable Integer id,
            @io.swagger.v3.oas.annotations.parameters.RequestBody(description = "Updated rally details", required = true)
            @RequestBody UpdateRallyRequest request
    ) {
        return serviceCaller.call((member) ->
            DtoMapper.toUiRally(member, rallyService.updateRally(member, id, request)));
    }

    @Operation(
        summary = "Get rally by ID",
        description = "Retrieve detailed information about a specific rally",
        responses = {
            @ApiResponse(responseCode = "200", description = "Rally found"),
            @ApiResponse(responseCode = "401", description = "Not authenticated"),
            @ApiResponse(responseCode = "404", description = "Rally not found")
        }
    )
    @GetMapping("/rally/{id}")
    ResponseEntity<RestResponse<UiRally>> getRally(
            @Parameter(description = "Rally ID", example = "1", required = true)
            @PathVariable Integer id
    ) {
        return serviceCaller.call((member) ->
            DtoMapper.toUiRally(member, rallyService.getRally(member, id)));
    }

    @Operation(
        summary = "Search rallies",
        description = "Search and filter rallies by name, date range, location, and proximity. Returns paginated results.",
        responses = {
            @ApiResponse(responseCode = "200", description = "Search completed successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid search parameters"),
            @ApiResponse(responseCode = "401", description = "Not authenticated")
        }
    )
    @GetMapping("/rallies")
    ResponseEntity<RestResponse<RestPage<UiRally>>> searchRallies(
            @Parameter(description = "Case-insensitive text search in rally name", example = "Iron Butt")
            @RequestParam(required = false) String name,

            @Parameter(description = "Start date for date range filter (inclusive, ISO format)", example = "2025-06-01")
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @Parameter(description = "End date for date range filter (inclusive, ISO format)", example = "2025-08-31")
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,

            @Parameter(description = "Country filter (e.g., 'US', 'United States', 'CA', 'Canada')", example = "US")
            @RequestParam(required = false) String country,
            @Parameter(description = "State/province/territory filter", example = "California")
            @RequestParam(required = false, name = "region") String region,

            @Parameter(description = "Latitude for proximity search", example = "37.7749")
            @RequestParam(required = false) Double nearLat,
            @Parameter(description = "Longitude for proximity search", example = "-122.4194")
            @RequestParam(required = false) Double nearLng,
            @Parameter(description = "Search radius in miles for proximity search", example = "100")
            @RequestParam(required = false, defaultValue = "100") Double radiusMiles,

            @Parameter(description = "If true, include all rallies regardless of visibility settings")
            @RequestParam(required = false) Boolean all,
            @Parameter(description = "Pagination and sorting parameters (page, size, sort)", example = "page=0&size=20&sort=startDate,asc")
            @PageableDefault(size = 20, sort = "startDate", direction = Sort.Direction.ASC) Pageable pageable
    ) {
        if (from != null) {
            log.info("Searching for rallys from {} to {}", from, to);
        }
        return serviceCaller.call((member) -> {
                Page<UiRally> page = rallyService.search(name, from, to, country, region, nearLat, nearLng, radiusMiles, all != null && all, pageable)
                        .map(rally -> DtoMapper.toUiRally(member, rally));
                return RestPageHelper.from(page);
            }
        );
    }

    @Operation(
        summary = "Register for a rally",
        description = "Register the authenticated user as a participant in the specified rally",
        responses = {
            @ApiResponse(responseCode = "200", description = "Successfully registered"),
            @ApiResponse(responseCode = "400", description = "Already registered or registration not allowed"),
            @ApiResponse(responseCode = "401", description = "Not authenticated"),
            @ApiResponse(responseCode = "404", description = "Rally not found")
        }
    )
    @PostMapping("/rally/{rallyId}/register")
    ResponseEntity<RestResponse<UiRallyParticipant>> registerForRally(
            @Parameter(description = "Rally ID to register for", example = "1", required = true)
            @PathVariable Integer rallyId
    ) {
        return serviceCaller.call((member) ->
            DtoMapper.toUiRallyParticipant(rallyService.registerRider(member, rallyId)));
    }

    @Operation(
        summary = "Promote a rally participant",
        description = "Promote a rally participant to ORGANIZER or AIDE role. Only rally organizers can promote participants.",
        responses = {
            @ApiResponse(responseCode = "200", description = "Participant promoted successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid promotion request"),
            @ApiResponse(responseCode = "401", description = "Not authenticated"),
            @ApiResponse(responseCode = "403", description = "Not authorized to promote participants"),
            @ApiResponse(responseCode = "404", description = "Rally or participant not found")
        }
    )
    @PutMapping("/rally/{rallyId}/promote")
    ResponseEntity<RestResponse<UiRallyParticipant>> promoteParticipant(
            @Parameter(description = "Rally ID", example = "1", required = true)
            @PathVariable Integer rallyId,
            @io.swagger.v3.oas.annotations.parameters.RequestBody(description = "Promotion details including target member ID and new role type", required = true)
            @RequestBody PromoteParticipantRequest request
    ) {
         return serviceCaller.call((member) ->
            DtoMapper.toUiRallyParticipant(rallyService.promoteParticipant(
                member, rallyId, request.getTargetMemberId(), request.getNewType())));
    }

    @Operation(
        summary = "Delete a rally",
        description = "Delete a rally. Only the rally organizer can delete a rally.",
        responses = {
            @ApiResponse(responseCode = "200", description = "Rally deleted successfully"),
            @ApiResponse(responseCode = "401", description = "Not authenticated"),
            @ApiResponse(responseCode = "403", description = "Not authorized to delete this rally"),
            @ApiResponse(responseCode = "404", description = "Rally not found")
        }
    )
    @DeleteMapping("/rally/{rallyId}")
    ResponseEntity<RestResponse<Boolean>> deleteRally(
            @Parameter(description = "Rally ID to delete", example = "1", required = true)
            @PathVariable Integer rallyId
    ) {
        return serviceCaller.call((member) -> rallyService.deleteRally(member, rallyId));
    }

    //======================================================================
    // BonusPoint CRUD
    //======================================================================

    @Operation(
        summary = "Create a bonus point",
        description = "Add a new bonus point location to a rally. Only rally organizers can create bonus points.",
        responses = {
            @ApiResponse(responseCode = "200", description = "Bonus point created successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid bonus point data"),
            @ApiResponse(responseCode = "401", description = "Not authenticated"),
            @ApiResponse(responseCode = "403", description = "Not authorized to create bonus points for this rally"),
            @ApiResponse(responseCode = "404", description = "Rally not found")
        }
    )
    @PostMapping("/rally/{rallyId}/bonuspoint")
    ResponseEntity<RestResponse<UiBonusPoint>> createBonusPoint(
            @Parameter(description = "Rally ID to add bonus point to", example = "1", required = true)
            @PathVariable Integer rallyId,
            @io.swagger.v3.oas.annotations.parameters.RequestBody(description = "Bonus point details including name, location, points value, and other attributes", required = true)
            @RequestBody CreateBonusPointRequest request
    ) {
        return serviceCaller.call((member) ->
            DtoMapper.toUiBonusPoint(rallyService.createBonusPoint(member, rallyId, request)));
    }

    @Operation(
        summary = "Update a bonus point",
        description = "Update an existing bonus point. Only rally organizers can update bonus points.",
        responses = {
            @ApiResponse(responseCode = "200", description = "Bonus point updated successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid bonus point data"),
            @ApiResponse(responseCode = "401", description = "Not authenticated"),
            @ApiResponse(responseCode = "403", description = "Not authorized to update this bonus point"),
            @ApiResponse(responseCode = "404", description = "Bonus point not found")
        }
    )
    @PutMapping("/bonuspoint/{id}")
    ResponseEntity<RestResponse<UiBonusPoint>> updateBonusPoint(
            @Parameter(description = "Bonus point ID", example = "1", required = true)
            @PathVariable Integer id,
            @io.swagger.v3.oas.annotations.parameters.RequestBody(description = "Updated bonus point details", required = true)
            @RequestBody UpdateBonusPointRequest request
    ) {
        return serviceCaller.call((member) ->
            DtoMapper.toUiBonusPoint(rallyService.updateBonusPoint(member, id, request)));
    }

    @Operation(
        summary = "Delete a bonus point",
        description = "Delete a bonus point from a rally. Only rally organizers can delete bonus points.",
        responses = {
            @ApiResponse(responseCode = "200", description = "Bonus point deleted successfully"),
            @ApiResponse(responseCode = "401", description = "Not authenticated"),
            @ApiResponse(responseCode = "403", description = "Not authorized to delete this bonus point"),
            @ApiResponse(responseCode = "404", description = "Bonus point not found")
        }
    )
    @DeleteMapping("/bonuspoint/{id}")
    ResponseEntity<RestResponse<Void>> deleteBonusPoint(
            @Parameter(description = "Bonus point ID to delete", example = "1", required = true)
            @PathVariable Integer id
    ) {
        return serviceCaller.call((member) -> {
            rallyService.deleteBonusPoint(member, id);
            return null;
        });
    }

    @Operation(
        summary = "Get bonus point by ID",
        description = "Retrieve detailed information about a specific bonus point",
        responses = {
            @ApiResponse(responseCode = "200", description = "Bonus point found"),
            @ApiResponse(responseCode = "401", description = "Not authenticated"),
            @ApiResponse(responseCode = "404", description = "Bonus point not found")
        }
    )
    @GetMapping("/bonuspoint/{id}")
    ResponseEntity<RestResponse<UiBonusPoint>> getBonusPoint(
            @Parameter(description = "Bonus point ID", example = "1", required = true)
            @PathVariable Integer id
    ) {
        return serviceCaller.call((member) ->
            DtoMapper.toUiBonusPoint(rallyService.getBonusPoint(member, id)));
    }

    @Operation(
        summary = "List all bonus points for a rally",
        description = "Retrieve all bonus points associated with a specific rally",
        responses = {
            @ApiResponse(responseCode = "200", description = "Bonus points retrieved successfully"),
            @ApiResponse(responseCode = "401", description = "Not authenticated"),
            @ApiResponse(responseCode = "404", description = "Rally not found")
        }
    )
    @GetMapping("/rally/{rallyId}/bonuspoints")
    ResponseEntity<RestResponse<List<UiBonusPoint>>> listBonusPoints(
            @Parameter(description = "Rally ID to list bonus points for", example = "1", required = true)
            @PathVariable Integer rallyId
    ) {
        return serviceCaller.call((member) ->
            rallyService.listBonusPoints(member, rallyId).stream()
                    .map(DtoMapper::toUiBonusPoint)
                    .toList());
    }

    //======================================================================
    // Combination CRUD
    //======================================================================

    @Operation(
        summary = "Create a bonus combination",
        description = "Create a new bonus point combination for a rally. Combinations allow riders to earn extra points by visiting specific sets of bonus points. Only rally organizers can create combinations.",
        responses = {
            @ApiResponse(responseCode = "200", description = "Combination created successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid combination data"),
            @ApiResponse(responseCode = "401", description = "Not authenticated"),
            @ApiResponse(responseCode = "403", description = "Not authorized to create combinations for this rally"),
            @ApiResponse(responseCode = "404", description = "Rally not found")
        }
    )
    @PostMapping("/rally/{rallyId}/combination")
    ResponseEntity<RestResponse<UiCombination>> createCombination(
            @Parameter(description = "Rally ID to add combination to", example = "1", required = true)
            @PathVariable Integer rallyId,
            @io.swagger.v3.oas.annotations.parameters.RequestBody(description = "Combination details including name, description, and point value", required = true)
            @RequestBody CreateCombinationRequest request
    ) {
        return serviceCaller.call((member) ->
            DtoMapper.toUiCombination(rallyService.createCombination(member, rallyId, request)));
    }

    @Operation(
        summary = "Update a combination",
        description = "Update an existing bonus point combination. Only rally organizers can update combinations.",
        responses = {
            @ApiResponse(responseCode = "200", description = "Combination updated successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid combination data"),
            @ApiResponse(responseCode = "401", description = "Not authenticated"),
            @ApiResponse(responseCode = "403", description = "Not authorized to update this combination"),
            @ApiResponse(responseCode = "404", description = "Combination not found")
        }
    )
    @PutMapping("/combination/{id}")
    ResponseEntity<RestResponse<UiCombination>> updateCombination(
            @Parameter(description = "Combination ID", example = "1", required = true)
            @PathVariable Integer id,
            @io.swagger.v3.oas.annotations.parameters.RequestBody(description = "Updated combination details", required = true)
            @RequestBody UpdateCombinationRequest request
    ) {
        return serviceCaller.call((member) ->
            DtoMapper.toUiCombination(rallyService.updateCombination(member, id, request)));
    }

    @Operation(
        summary = "Delete a combination",
        description = "Delete a bonus point combination from a rally. Only rally organizers can delete combinations.",
        responses = {
            @ApiResponse(responseCode = "200", description = "Combination deleted successfully"),
            @ApiResponse(responseCode = "401", description = "Not authenticated"),
            @ApiResponse(responseCode = "403", description = "Not authorized to delete this combination"),
            @ApiResponse(responseCode = "404", description = "Combination not found")
        }
    )
    @DeleteMapping("/combination/{id}")
    ResponseEntity<RestResponse<Void>> deleteCombination(
            @Parameter(description = "Combination ID to delete", example = "1", required = true)
            @PathVariable Integer id
    ) {
        return serviceCaller.call((member) -> {
            rallyService.deleteCombination(member, id);
            return null;
        });
    }

    @Operation(
        summary = "Get combination by ID",
        description = "Retrieve detailed information about a specific bonus point combination",
        responses = {
            @ApiResponse(responseCode = "200", description = "Combination found"),
            @ApiResponse(responseCode = "401", description = "Not authenticated"),
            @ApiResponse(responseCode = "404", description = "Combination not found")
        }
    )
    @GetMapping("/combination/{id}")
    ResponseEntity<RestResponse<UiCombination>> getCombination(
            @Parameter(description = "Combination ID", example = "1", required = true)
            @PathVariable Integer id
    ) {
        return serviceCaller.call((member) ->
            DtoMapper.toUiCombination(rallyService.getCombination(member, id)));
    }

    @Operation(
        summary = "List all combinations for a rally",
        description = "Retrieve all bonus point combinations associated with a specific rally",
        responses = {
            @ApiResponse(responseCode = "200", description = "Combinations retrieved successfully"),
            @ApiResponse(responseCode = "401", description = "Not authenticated"),
            @ApiResponse(responseCode = "404", description = "Rally not found")
        }
    )
    @GetMapping("/rally/{rallyId}/combinations")
    ResponseEntity<RestResponse<List<UiCombination>>> listCombinations(
            @Parameter(description = "Rally ID to list combinations for", example = "1", required = true)
            @PathVariable Integer rallyId
    ) {
        return serviceCaller.call((member) ->
            rallyService.listCombinations(member, rallyId).stream()
                    .map(c -> DtoMapper.toUiCombination(c))
                    .toList());
    }

    //======================================================================
    // CombinationPoint CRUD
    //======================================================================

    @Operation(
        summary = "Add a bonus point to a combination",
        description = "Add an existing bonus point to a bonus point combination. Only rally organizers can add combination points.",
        responses = {
            @ApiResponse(responseCode = "200", description = "Bonus point added to combination successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid request or bonus point already in combination"),
            @ApiResponse(responseCode = "401", description = "Not authenticated"),
            @ApiResponse(responseCode = "403", description = "Not authorized to modify this combination"),
            @ApiResponse(responseCode = "404", description = "Combination or bonus point not found")
        }
    )
    @PostMapping("/combination/{combinationId}/bonuspoint")
    ResponseEntity<RestResponse<UiCombinationPoint>> addCombinationPoint(
            @Parameter(description = "Combination ID to add bonus point to", example = "1", required = true)
            @PathVariable Integer combinationId,
            @io.swagger.v3.oas.annotations.parameters.RequestBody(description = "Request specifying which bonus point to add to the combination", required = true)
            @RequestBody CreateCombinationPointRequest request
    ) {
        return serviceCaller.call((member) ->
            DtoMapper.toUiCombinationPoint(rallyService.addCombinationPoint(member, combinationId, request)));
    }

    @Operation(
        summary = "Remove a bonus point from a combination",
        description = "Remove a bonus point from a bonus point combination. Only rally organizers can delete combination points.",
        responses = {
            @ApiResponse(responseCode = "200", description = "Bonus point removed from combination successfully"),
            @ApiResponse(responseCode = "401", description = "Not authenticated"),
            @ApiResponse(responseCode = "403", description = "Not authorized to modify this combination"),
            @ApiResponse(responseCode = "404", description = "Combination point not found")
        }
    )
    @DeleteMapping("/combinationpoint/{id}")
    ResponseEntity<RestResponse<Void>> deleteCombinationPoint(
            @Parameter(description = "Combination point ID to delete", example = "1", required = true)
            @PathVariable Integer id
    ) {
        return serviceCaller.call((member) -> {
            rallyService.deleteCombinationPoint(member, id);
            return null;
        });
    }

    @Operation(
        summary = "List all bonus points in a combination",
        description = "Retrieve all bonus points that are part of a specific combination",
        responses = {
            @ApiResponse(responseCode = "200", description = "Combination points retrieved successfully"),
            @ApiResponse(responseCode = "401", description = "Not authenticated"),
            @ApiResponse(responseCode = "404", description = "Combination not found")
        }
    )
    @GetMapping("/combination/{combinationId}/bonuspoints")
    ResponseEntity<RestResponse<List<UiCombinationPoint>>> listCombinationPoints(
            @Parameter(description = "Combination ID to list bonus points for", example = "1", required = true)
            @PathVariable Integer combinationId
    ) {
        return serviceCaller.call((member) ->
            rallyService.listCombinationPoints(member, combinationId).stream()
                    .map(DtoMapper::toUiCombinationPoint)
                    .toList());
    }

}
