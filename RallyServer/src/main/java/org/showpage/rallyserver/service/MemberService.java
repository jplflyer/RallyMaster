package org.showpage.rallyserver.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.showpage.rallyserver.entity.Member;
import org.showpage.rallyserver.entity.Motorcycle;
import org.showpage.rallyserver.entity.RallyParticipant;
import org.showpage.rallyserver.exception.NotFoundException;
import org.showpage.rallyserver.exception.UnauthorizedException;
import org.showpage.rallyserver.exception.ValidationException;
import org.showpage.rallyserver.repository.MemberRepository;
import org.showpage.rallyserver.repository.MotorcycleRepository;
import org.showpage.rallyserver.repository.RallyParticipantRepository;
import org.showpage.rallyserver.ui.ChangePasswordRequest;
import org.showpage.rallyserver.ui.CreateMotorcycleRequest;
import org.showpage.rallyserver.ui.UpdateMemberRequest;
import org.showpage.rallyserver.ui.UpdateMotorcycleRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class MemberService implements UserDetailsService {
    private final MemberRepository memberRepository;
    private final MotorcycleRepository motorcycleRepository;
    private final PasswordEncoder passwordEncoder;
    private final RallyParticipantRepository rallyParticipantRepository;

    @Override
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        Member member = memberRepository.findByEmail(email)
                .orElseThrow(() -> new UsernameNotFoundException("User not found: " + email));

        return User.builder()
                .username(member.getEmail())
                .password(member.getPassword())
                .authorities(Collections.emptyList())
                .build();
    }

    public Member createMember(String email, String password) throws ValidationException {
        Member found = memberRepository.findByEmail(email).orElse(null);
        if (found != null) {
            throw new ValidationException("Email already exists: " + email);
        }

        log.info("Creating member with email {}", email);
        Member member = Member
                .builder()
                .email(email)
                .password(passwordEncoder.encode(password))
                .isAdmin(false)
                .build();
        return memberRepository.save(member);
    }

    public Optional<Member> findByEmail(String email) {
        return memberRepository.findByEmail(email);
    }

    //----------------------------------------------------------------------
    // Refresh Token Management
    //----------------------------------------------------------------------

    /**
     * Store a refresh token for a member. This allows tracking one-time use.
     */
    public void storeRefreshToken(Member member, String refreshToken) {
        member.setRefreshToken(refreshToken);
        memberRepository.save(member);
    }

    /**
     * Validate and consume a refresh token. Returns the member if valid, throws exception otherwise.
     * The refresh token can only be used once - it's cleared after validation.
     */
    public Member validateAndConsumeRefreshToken(String refreshToken, String email)
        throws UnauthorizedException
    {
        Member member = memberRepository.findByEmail(email)
                .orElseThrow(() -> new UnauthorizedException("Invalid refresh token"));

        // Check if the stored refresh token matches
        if (member.getRefreshToken() == null || !member.getRefreshToken().equals(refreshToken)) {
            throw new UnauthorizedException("Invalid or already used refresh token");
        }

        // Clear the refresh token so it can't be used again
        member.setRefreshToken(null);
        memberRepository.save(member);

        return member;
    }

    //----------------------------------------------------------------------
    // Admin Operations
    //----------------------------------------------------------------------

    /**
     * Get all members. Admin-only operation.
     */
    public List<Member> getAllMembers(Member currentMember) throws UnauthorizedException {
        if (!Boolean.TRUE.equals(currentMember.getIsAdmin())) {
            throw new UnauthorizedException("Only admins can retrieve all members");
        }
        return memberRepository.findAll();
    }

    /**
     * Delete a member. Admin-only operation.
     */
    public void deleteMember(Member currentMember, Integer memberId)
        throws UnauthorizedException, NotFoundException
    {
        if (!Boolean.TRUE.equals(currentMember.getIsAdmin())) {
            throw new UnauthorizedException("Only admins can delete members");
        }

        Member member = memberRepository.findById(memberId)
                .orElseThrow(() -> new NotFoundException("Member not found: " + memberId));

        log.info("Admin {} deleting member {} (ID: {})",
                currentMember.getEmail(), member.getEmail(), memberId);

        memberRepository.delete(member);
    }

    //----------------------------------------------------------------------
    // Member Operations
    //----------------------------------------------------------------------

    /**
     * Update member info (currently only spotwallaUsername).
     */
    public Member updateMember(Member currentMember, UpdateMemberRequest request) {
        if (request.getSpotwallaUsername() != null) {
            currentMember.setSpotwallaUsername(request.getSpotwallaUsername());
        }

        return memberRepository.save(currentMember);
    }

    /**
     * Change member password. Verifies old password before setting new one.
     */
    public void changePassword(Member currentMember, ChangePasswordRequest request)
        throws ValidationException
    {
        // Validate request
        if (request.getOldPassword() == null || request.getOldPassword().trim().isEmpty()) {
            throw new ValidationException("Old password is required");
        }
        if (request.getNewPassword() == null || request.getNewPassword().trim().isEmpty()) {
            throw new ValidationException("New password is required");
        }

        // Verify old password
        if (!passwordEncoder.matches(request.getOldPassword(), currentMember.getPassword())) {
            throw new ValidationException("Old password is incorrect");
        }

        // Set new password
        currentMember.setPassword(passwordEncoder.encode(request.getNewPassword()));
        memberRepository.save(currentMember);

        log.info("Password changed for member: {}", currentMember.getEmail());
    }

    //----------------------------------------------------------------------
    // Motorcycle CRUD Operations
    //----------------------------------------------------------------------

    /**
     * Create a motorcycle for the current member.
     */
    public Motorcycle createMotorcycle(Member currentMember, CreateMotorcycleRequest request) {
        Motorcycle motorcycle = new Motorcycle();
        motorcycle.setMember(currentMember);
        motorcycle.setMemberId(currentMember.getId());
        motorcycle.setMake(request.getMake());
        motorcycle.setModel(request.getModel());
        motorcycle.setYear(request.getYear());
        motorcycle.setColor(request.getColor());
        motorcycle.setStatus(request.getStatus());
        motorcycle.setActive(request.getActive() != null ? request.getActive() : true);

        return motorcycleRepository.save(motorcycle);
    }

    /**
     * Update a motorcycle. Only the owner can update it.
     */
    public Motorcycle updateMotorcycle(
        Member currentMember,
        Integer motorcycleId,
        UpdateMotorcycleRequest request
    )
        throws NotFoundException, UnauthorizedException
    {
        Motorcycle motorcycle = motorcycleRepository.findById(motorcycleId)
                .orElseThrow(() -> new NotFoundException("Motorcycle not found: " + motorcycleId));

        // Verify ownership
        if (!motorcycle.getMemberId().equals(currentMember.getId())) {
            throw new UnauthorizedException("You can only update your own motorcycles");
        }

        // Update only non-null fields
        if (request.getMake() != null) {
            motorcycle.setMake(request.getMake());
        }
        if (request.getModel() != null) {
            motorcycle.setModel(request.getModel());
        }
        if (request.getYear() != null) {
            motorcycle.setYear(request.getYear());
        }
        if (request.getColor() != null) {
            motorcycle.setColor(request.getColor());
        }
        if (request.getStatus() != null) {
            motorcycle.setStatus(request.getStatus());
        }
        if (request.getActive() != null) {
            motorcycle.setActive(request.getActive());
        }

        return motorcycleRepository.save(motorcycle);
    }

    /**
     * Delete a motorcycle. Only the owner can delete it.
     */
    public void deleteMotorcycle(Member currentMember, Integer motorcycleId)
        throws NotFoundException, UnauthorizedException
    {
        Motorcycle motorcycle = motorcycleRepository.findById(motorcycleId)
                .orElseThrow(() -> new NotFoundException("Motorcycle not found: " + motorcycleId));

        // Verify ownership
        if (!motorcycle.getMemberId().equals(currentMember.getId())) {
            throw new UnauthorizedException("You can only delete your own motorcycles");
        }

        log.info("Member {} deleting motorcycle ID: {}",
                currentMember.getEmail(), motorcycleId);

        motorcycleRepository.delete(motorcycle);
    }

    //----------------------------------------------------------------------
    // Rally Participation Operations
    //----------------------------------------------------------------------

    /**
     * Get all active rally participations for a member.
     * Includes future rallies, in-progress rallies, and rallies completed within the last week.
     */
    public List<RallyParticipant> getActiveRallyParticipations(Member member) {
        // Calculate cutoff date (1 week ago)
        LocalDate cutoffDate = LocalDate.now().minusWeeks(1);

        return rallyParticipantRepository.findActiveParticipationsByMemberId(
                member.getId(),
                cutoffDate
        );
    }
}