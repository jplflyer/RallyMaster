package org.showpage.rallyserver.controller;

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
public class RallyController {
    private final ServiceCaller serviceCaller;
    private final RallyService rallyService;

    /**
     * Register a new rally.
     */
    @PostMapping("/rally")
    ResponseEntity<RestResponse<UiRally>> createRally(@RequestBody CreateRallyRequest request) {
        return serviceCaller.call("/api/rally/", (member) ->
            DtoMapper.toUiRally(member, rallyService.createRally(member, request)));
    }

    /**
     * Update the rally.
     */
    @PutMapping("/rally/{id}")
    ResponseEntity<RestResponse<UiRally>> updateRally(
            @PathVariable Integer id,
            @RequestBody UpdateRallyRequest request
    ) {
        return serviceCaller.call((member) ->
            DtoMapper.toUiRally(member, rallyService.updateRally(member, id, request)));
    }

    @GetMapping("/rally/{id}")
    ResponseEntity<RestResponse<UiRally>> getRally(@PathVariable Integer id) {
        return serviceCaller.call((member) ->
            DtoMapper.toUiRally(member, rallyService.getRally(member, id)));
    }

    /**
     * This performs a search.
     */
    @GetMapping("/rallies")
    ResponseEntity<RestResponse<RestPage<UiRally>>> searchRallies(
            // text search: case-insensitive "contains"
            @RequestParam(required = false) String name,

            // date range (inclusive)
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,

            // location filters
            @RequestParam(required = false) String country,   // e.g., "US", "United States", "CA", "Canada"
            @RequestParam(required = false, name = "region") String region, // state/province/territory text

            // optional "within radius of point" (no PostGIS required)
            @RequestParam(required = false) Double nearLat,
            @RequestParam(required = false) Double nearLng,
            @RequestParam(required = false, defaultValue = "100") Double radiusMiles,

            @RequestParam(required = false) Boolean all,
            @PageableDefault(size = 20, sort = "startDate", direction = Sort.Direction.ASC) Pageable pageable
    ) {
        if (from != null) {
            log.info("Searching for rallys from {} to {}", from, to);
        }
        return serviceCaller.call((member) -> {
                Page<UiRally> page = rallyService.search(name, from, to, country, region, nearLat, nearLng, radiusMiles, all != null && all, pageable)
                        .map(rally -> DtoMapper.toUiRally(member, rally));
                return RestPage.from(page);
            }
        );
    }

    /**
     * Register a rider for a rally.
     */
    @PostMapping("/rally/{rallyId}/register")
    ResponseEntity<RestResponse<UiRallyParticipant>> registerForRally(@PathVariable Integer rallyId) {
        return serviceCaller.call((member) ->
            DtoMapper.toUiRallyParticipant(rallyService.registerRider(member, rallyId)));
    }

    /**
     * Promote a participant to ORGANIZER or AIDE.
     */
    @PutMapping("/rally/{rallyId}/promote")
    ResponseEntity<RestResponse<UiRallyParticipant>> promoteParticipant(
            @PathVariable Integer rallyId,
            @RequestBody PromoteParticipantRequest request
    ) {
         return serviceCaller.call((member) ->
            DtoMapper.toUiRallyParticipant(rallyService.promoteParticipant(
                member, rallyId, request.getTargetMemberId(), request.getNewType())));
    }

    @DeleteMapping("/rally/{rallyId}")
    ResponseEntity<RestResponse<Boolean>> deleteRally(@PathVariable Integer rallyId) {
        return serviceCaller.call((member) -> rallyService.deleteRally(member, rallyId));
    }

    //======================================================================
    // BonusPoint CRUD
    //======================================================================

    /**
     * Create a bonus point for a rally.
     */
    @PostMapping("/rally/{rallyId}/bonuspoint")
    ResponseEntity<RestResponse<UiBonusPoint>> createBonusPoint(
            @PathVariable Integer rallyId,
            @RequestBody CreateBonusPointRequest request
    ) {
        return serviceCaller.call((member) ->
            DtoMapper.toUiBonusPoint(rallyService.createBonusPoint(member, rallyId, request)));
    }

    /**
     * Update a bonus point.
     */
    @PutMapping("/bonuspoint/{id}")
    ResponseEntity<RestResponse<UiBonusPoint>> updateBonusPoint(
            @PathVariable Integer id,
            @RequestBody UpdateBonusPointRequest request
    ) {
        return serviceCaller.call((member) ->
            DtoMapper.toUiBonusPoint(rallyService.updateBonusPoint(member, id, request)));
    }

    /**
     * Delete a bonus point.
     */
    @DeleteMapping("/bonuspoint/{id}")
    ResponseEntity<RestResponse<Void>> deleteBonusPoint(@PathVariable Integer id) {
        return serviceCaller.call((member) -> {
            rallyService.deleteBonusPoint(member, id);
            return null;
        });
    }

    /**
     * Get a bonus point by ID.
     */
    @GetMapping("/bonuspoint/{id}")
    ResponseEntity<RestResponse<UiBonusPoint>> getBonusPoint(@PathVariable Integer id) {
        return serviceCaller.call((member) ->
            DtoMapper.toUiBonusPoint(rallyService.getBonusPoint(member, id)));
    }

    /**
     * List all bonus points for a rally.
     */
    @GetMapping("/rally/{rallyId}/bonuspoints")
    ResponseEntity<RestResponse<List<UiBonusPoint>>> listBonusPoints(@PathVariable Integer rallyId) {
        return serviceCaller.call((member) ->
            rallyService.listBonusPoints(member, rallyId).stream()
                    .map(DtoMapper::toUiBonusPoint)
                    .toList());
    }

    //======================================================================
    // Combination CRUD
    //======================================================================

    /**
     * Create a combination for a rally.
     */
    @PostMapping("/rally/{rallyId}/combination")
    ResponseEntity<RestResponse<UiCombination>> createCombination(
            @PathVariable Integer rallyId,
            @RequestBody CreateCombinationRequest request
    ) {
        return serviceCaller.call((member) ->
            DtoMapper.toUiCombination(rallyService.createCombination(member, rallyId, request)));
    }

    /**
     * Update a combination.
     */
    @PutMapping("/combination/{id}")
    ResponseEntity<RestResponse<UiCombination>> updateCombination(
            @PathVariable Integer id,
            @RequestBody UpdateCombinationRequest request
    ) {
        return serviceCaller.call((member) ->
            DtoMapper.toUiCombination(rallyService.updateCombination(member, id, request)));
    }

    /**
     * Delete a combination.
     */
    @DeleteMapping("/combination/{id}")
    ResponseEntity<RestResponse<Void>> deleteCombination(@PathVariable Integer id) {
        return serviceCaller.call((member) -> {
            rallyService.deleteCombination(member, id);
            return null;
        });
    }

    /**
     * Get a combination by ID.
     */
    @GetMapping("/combination/{id}")
    ResponseEntity<RestResponse<UiCombination>> getCombination(@PathVariable Integer id) {
        return serviceCaller.call((member) ->
            DtoMapper.toUiCombination(rallyService.getCombination(member, id)));
    }

    /**
     * List all combinations for a rally.
     */
    @GetMapping("/rally/{rallyId}/combinations")
    ResponseEntity<RestResponse<List<UiCombination>>> listCombinations(@PathVariable Integer rallyId) {
        return serviceCaller.call((member) ->
            rallyService.listCombinations(member, rallyId).stream()
                    .map(c -> DtoMapper.toUiCombination(c))
                    .toList());
    }

    //======================================================================
    // CombinationPoint CRUD
    //======================================================================

    /**
     * Add a bonus point to a combination.
     */
    @PostMapping("/combination/{combinationId}/bonuspoint")
    ResponseEntity<RestResponse<UiCombinationPoint>> addCombinationPoint(
            @PathVariable Integer combinationId,
            @RequestBody CreateCombinationPointRequest request
    ) {
        return serviceCaller.call((member) ->
            DtoMapper.toUiCombinationPoint(rallyService.addCombinationPoint(member, combinationId, request)));
    }

    /**
     * Remove a bonus point from a combination.
     */
    @DeleteMapping("/combinationpoint/{id}")
    ResponseEntity<RestResponse<Void>> deleteCombinationPoint(@PathVariable Integer id) {
        return serviceCaller.call((member) -> {
            rallyService.deleteCombinationPoint(member, id);
            return null;
        });
    }

    /**
     * List all bonus points for a combination.
     */
    @GetMapping("/combination/{combinationId}/bonuspoints")
    ResponseEntity<RestResponse<List<UiCombinationPoint>>> listCombinationPoints(@PathVariable Integer combinationId) {
        return serviceCaller.call((member) ->
            rallyService.listCombinationPoints(member, combinationId).stream()
                    .map(DtoMapper::toUiCombinationPoint)
                    .toList());
    }

}
