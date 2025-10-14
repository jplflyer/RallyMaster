package org.showpage.rallyserver.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * This represents a participant in the rally.
 */
@Entity
@Data
@NoArgsConstructor
@AllArgsConstructor
public class RallyParticipant {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    Integer id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "rally_id")
    private Rally rally;

    @Column(name = "rally_id", insertable = false, updatable = false)
    private Integer rallyId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "member_id")
    private Member member;

    @Column(name = "member_id", insertable = false, updatable = false)
    Integer memberId;

    @Enumerated(EnumType.STRING)
    private RallyParticipantType participantType;

    private Integer odometerIn;
    private Integer odometerOut;
    private Boolean finisher;
    private Integer finalScore;
}
