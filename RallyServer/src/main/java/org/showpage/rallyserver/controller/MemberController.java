package org.showpage.rallyserver.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.showpage.rallyserver.RestResponse;
import org.showpage.rallyserver.entity.Member;
import org.showpage.rallyserver.entity.Motorcycle;
import org.showpage.rallyserver.entity.RallyParticipant;
import org.showpage.rallyserver.service.DtoMapper;
import org.showpage.rallyserver.service.MemberService;
import org.showpage.rallyserver.ui.*;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api")
@Slf4j
@RequiredArgsConstructor
@Tag(name = "Member Management", description = "Endpoints for managing member profiles, motorcycles, and admin operations")
public class MemberController {
    private final ServiceCaller serviceCaller;
    private final MemberService memberService;

    @Operation(
        summary = "Get current member information",
        description = "Retrieve the authenticated member's profile information including active rally participations (future, in-progress, and recently completed rallies)",
        responses = {
            @ApiResponse(responseCode = "200", description = "Member information retrieved successfully"),
            @ApiResponse(responseCode = "401", description = "Not authenticated")
        }
    )
    @GetMapping("/member/info")
    ResponseEntity<RestResponse<UiMember>> myInfo() {
        return serviceCaller.call((member) -> {
            // Get active rally participations (future, in-progress, and recently completed)
            List<RallyParticipant> participations = memberService.getActiveRallyParticipations(member);
            return DtoMapper.toUiMember(member, participations);
        });
    }

    //----------------------------------------------------------------------
    // Admin Operations
    //----------------------------------------------------------------------

    @Operation(
        summary = "Get all members (admin only)",
        description = "Retrieve a list of all members in the system. This endpoint is restricted to administrators only.",
        responses = {
            @ApiResponse(responseCode = "200", description = "Members retrieved successfully"),
            @ApiResponse(responseCode = "401", description = "Not authenticated"),
            @ApiResponse(responseCode = "403", description = "Not authorized - admin privileges required")
        }
    )
    @GetMapping("/admin/members")
    ResponseEntity<RestResponse<List<UiMember>>> getAllMembers() {
        return serviceCaller.call((member) -> {
            List<Member> members = memberService.getAllMembers(member);
            return members.stream()
                    .map(DtoMapper::toUiMember)
                    .collect(Collectors.toList());
        });
    }

    @Operation(
        summary = "Delete a member (admin only)",
        description = "Delete a member account from the system. This endpoint is restricted to administrators only.",
        responses = {
            @ApiResponse(responseCode = "200", description = "Member deleted successfully"),
            @ApiResponse(responseCode = "401", description = "Not authenticated"),
            @ApiResponse(responseCode = "403", description = "Not authorized - admin privileges required"),
            @ApiResponse(responseCode = "404", description = "Member not found")
        }
    )
    @DeleteMapping("/admin/member/{id}")
    ResponseEntity<RestResponse<Void>> deleteMember(
        @Parameter(description = "Member ID to delete", example = "1", required = true)
        @PathVariable Integer id
    ) {
        return serviceCaller.call((member) -> {
            memberService.deleteMember(member, id);
            return null;
        });
    }

    //----------------------------------------------------------------------
    // Member Operations
    //----------------------------------------------------------------------

    @Operation(
        summary = "Update member profile",
        description = "Update the authenticated member's profile information such as name, contact details, and preferences",
        responses = {
            @ApiResponse(responseCode = "200", description = "Member updated successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid member data"),
            @ApiResponse(responseCode = "401", description = "Not authenticated")
        }
    )
    @PutMapping("/member")
    ResponseEntity<RestResponse<UiMember>> updateMember(
        @io.swagger.v3.oas.annotations.parameters.RequestBody(description = "Updated member information", required = true)
        @RequestBody UpdateMemberRequest request
    ) {
        return serviceCaller.call((member) -> {
            Member updated = memberService.updateMember(member, request);
            return DtoMapper.toUiMember(updated);
        });
    }

    @Operation(
        summary = "Change password",
        description = "Change the authenticated member's password. Requires current password for verification.",
        responses = {
            @ApiResponse(responseCode = "200", description = "Password changed successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid password or password requirements not met"),
            @ApiResponse(responseCode = "401", description = "Not authenticated or current password incorrect")
        }
    )
    @PutMapping("/member/password")
    ResponseEntity<RestResponse<Void>> changePassword(
        @io.swagger.v3.oas.annotations.parameters.RequestBody(description = "Password change request with current and new password", required = true)
        @RequestBody ChangePasswordRequest request
    ) {
        return serviceCaller.call((member) -> {
            memberService.changePassword(member, request);
            return null;
        });
    }

    //----------------------------------------------------------------------
    // Motorcycle Operations
    //----------------------------------------------------------------------

    @Operation(
        summary = "Create a motorcycle",
        description = "Add a new motorcycle to the authenticated member's garage",
        responses = {
            @ApiResponse(responseCode = "200", description = "Motorcycle created successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid motorcycle data"),
            @ApiResponse(responseCode = "401", description = "Not authenticated")
        }
    )
    @PostMapping("/member/motorcycle")
    ResponseEntity<RestResponse<UiMotorcycle>> createMotorcycle(
        @io.swagger.v3.oas.annotations.parameters.RequestBody(description = "Motorcycle details including make, model, year, and other information", required = true)
        @RequestBody CreateMotorcycleRequest request
    ) {
        return serviceCaller.call((member) -> {
            Motorcycle motorcycle = memberService.createMotorcycle(member, request);
            return DtoMapper.toUiMotorcycle(motorcycle);
        });
    }

    @Operation(
        summary = "Update a motorcycle",
        description = "Update an existing motorcycle in the authenticated member's garage",
        responses = {
            @ApiResponse(responseCode = "200", description = "Motorcycle updated successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid motorcycle data"),
            @ApiResponse(responseCode = "401", description = "Not authenticated"),
            @ApiResponse(responseCode = "403", description = "Not authorized to update this motorcycle"),
            @ApiResponse(responseCode = "404", description = "Motorcycle not found")
        }
    )
    @PutMapping("/member/motorcycle/{id}")
    ResponseEntity<RestResponse<UiMotorcycle>> updateMotorcycle(
        @Parameter(description = "Motorcycle ID", example = "1", required = true)
        @PathVariable Integer id,
        @io.swagger.v3.oas.annotations.parameters.RequestBody(description = "Updated motorcycle details", required = true)
        @RequestBody UpdateMotorcycleRequest request
    ) {
        return serviceCaller.call((member) -> {
            Motorcycle motorcycle = memberService.updateMotorcycle(member, id, request);
            return DtoMapper.toUiMotorcycle(motorcycle);
        });
    }

    @Operation(
        summary = "Delete a motorcycle",
        description = "Delete a motorcycle from the authenticated member's garage",
        responses = {
            @ApiResponse(responseCode = "200", description = "Motorcycle deleted successfully"),
            @ApiResponse(responseCode = "401", description = "Not authenticated"),
            @ApiResponse(responseCode = "403", description = "Not authorized to delete this motorcycle"),
            @ApiResponse(responseCode = "404", description = "Motorcycle not found")
        }
    )
    @DeleteMapping("/member/motorcycle/{id}")
    ResponseEntity<RestResponse<Void>> deleteMotorcycle(
        @Parameter(description = "Motorcycle ID to delete", example = "1", required = true)
        @PathVariable Integer id
    ) {
        return serviceCaller.call((member) -> {
            memberService.deleteMotorcycle(member, id);
            return null;
        });
    }
}
