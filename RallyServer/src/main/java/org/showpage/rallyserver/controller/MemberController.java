package org.showpage.rallyserver.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.showpage.rallyserver.RestResponse;
import org.showpage.rallyserver.entity.Member;
import org.showpage.rallyserver.entity.Motorcycle;
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
public class MemberController {
    private final ServiceCaller serviceCaller;
    private final MemberService memberService;

    /**
     * Return my information.
     */
    @GetMapping("/member/info")
    ResponseEntity<RestResponse<UiMember>> myInfo() {
        return serviceCaller.call((member) -> DtoMapper.toUiMember(member));
    }

    //----------------------------------------------------------------------
    // Admin Operations
    //----------------------------------------------------------------------

    /**
     * Get all members (admin only).
     */
    @GetMapping("/admin/members")
    ResponseEntity<RestResponse<List<UiMember>>> getAllMembers() {
        return serviceCaller.call((member) -> {
            List<Member> members = memberService.getAllMembers(member);
            return members.stream()
                    .map(DtoMapper::toUiMember)
                    .collect(Collectors.toList());
        });
    }

    /**
     * Delete a member (admin only).
     */
    @DeleteMapping("/admin/member/{id}")
    ResponseEntity<RestResponse<Void>> deleteMember(@PathVariable Integer id) {
        return serviceCaller.call((member) -> {
            memberService.deleteMember(member, id);
            return null;
        });
    }

    //----------------------------------------------------------------------
    // Member Operations
    //----------------------------------------------------------------------

    /**
     * Update member info.
     */
    @PutMapping("/member")
    ResponseEntity<RestResponse<UiMember>> updateMember(@RequestBody UpdateMemberRequest request) {
        return serviceCaller.call((member) -> {
            Member updated = memberService.updateMember(member, request);
            return DtoMapper.toUiMember(updated);
        });
    }

    /**
     * Change password.
     */
    @PutMapping("/member/password")
    ResponseEntity<RestResponse<Void>> changePassword(@RequestBody ChangePasswordRequest request) {
        return serviceCaller.call((member) -> {
            memberService.changePassword(member, request);
            return null;
        });
    }

    //----------------------------------------------------------------------
    // Motorcycle Operations
    //----------------------------------------------------------------------

    /**
     * Create a motorcycle.
     */
    @PostMapping("/member/motorcycle")
    ResponseEntity<RestResponse<UiMotorcycle>> createMotorcycle(
        @RequestBody CreateMotorcycleRequest request
    ) {
        return serviceCaller.call((member) -> {
            Motorcycle motorcycle = memberService.createMotorcycle(member, request);
            return DtoMapper.toUiMotorcycle(motorcycle);
        });
    }

    /**
     * Update a motorcycle.
     */
    @PutMapping("/member/motorcycle/{id}")
    ResponseEntity<RestResponse<UiMotorcycle>> updateMotorcycle(
        @PathVariable Integer id,
        @RequestBody UpdateMotorcycleRequest request
    ) {
        return serviceCaller.call((member) -> {
            Motorcycle motorcycle = memberService.updateMotorcycle(member, id, request);
            return DtoMapper.toUiMotorcycle(motorcycle);
        });
    }

    /**
     * Delete a motorcycle.
     */
    @DeleteMapping("/member/motorcycle/{id}")
    ResponseEntity<RestResponse<Void>> deleteMotorcycle(@PathVariable Integer id) {
        return serviceCaller.call((member) -> {
            memberService.deleteMotorcycle(member, id);
            return null;
        });
    }
}
