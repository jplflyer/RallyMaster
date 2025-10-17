package org.showpage.rallyserver.controller;

import lombok.RequiredArgsConstructor;
import org.showpage.rallyserver.RestResponse;
import org.showpage.rallyserver.service.DtoMapper;
import org.showpage.rallyserver.service.RallyService;
import org.showpage.rallyserver.ui.CreateRallyRequest;
import org.showpage.rallyserver.ui.UiRally;
import org.showpage.rallyserver.ui.UpdateRallyRequest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class RallyController {
    private final ServiceCaller serviceCaller;
    private final RallyService rallyService;

    /**
     * Register a new rally.
     */
    @PostMapping("/rally")
    ResponseEntity<RestResponse<UiRally>> createRally(@RequestBody CreateRallyRequest request) {
        return serviceCaller.call("/api/rally/", (member) -> DtoMapper.toUiRally(member, rallyService.createRally(member, request)));
    }

    /**
     * Update the rally.
     */
    @PutMapping("/rally/{id}")
    ResponseEntity<RestResponse<UiRally>> updateRally(
            @PathVariable Integer id,
            @RequestBody UpdateRallyRequest request
    ) {
        return serviceCaller.call((member) -> DtoMapper.toUiRally(member, rallyService.updateRally(member, id, request)));
    }

    @GetMapping("/rally/{id}")
    ResponseEntity<RestResponse<UiRally>> getRally(@PathVariable Integer id) {
        return serviceCaller.call((member) -> DtoMapper.toUiRally(member, rallyService.getRally(member, id)));
    }

    /**
     * This performs a search.
     */
    @GetMapping("/rallies")
    ResponseEntity<RestResponse<Page<UiRally>>> searchRallies(
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

            @PageableDefault(size = 20, sort = "startDate", direction = Sort.Direction.ASC) Pageable pageable
    ) {
        return serviceCaller.call((member) ->
                rallyService.search(name, from, to, country, region, nearLat, nearLng, radiusMiles, pageable)
                        .map(rally -> DtoMapper.toUiRally(member, rally))
        );
    }
}
