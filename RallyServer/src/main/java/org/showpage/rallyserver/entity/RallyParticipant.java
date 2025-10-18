package org.showpage.rallyserver.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.util.List;

/**
 * This represents a participant in the rally.
 */
@Entity
@Data
@Builder
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
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)               // <-- key part
    private RallyParticipantType participantType;

    private Integer odometerIn;
    private Integer odometerOut;
    private Boolean finisher;
    private Integer finalScore;

    @OneToMany(
            mappedBy = "rallyParticipant",
            cascade = CascadeType.ALL,
            orphanRemoval = true,
            fetch = FetchType.LAZY
    )
    List<EarnedBonusPoint> earnedBonusPoints;

    @OneToMany(
            mappedBy = "rallyParticipant",
            cascade = CascadeType.ALL,
            orphanRemoval = true,
            fetch = FetchType.LAZY
    )
    List<EarnedCombination> earnedCombinations;

    @JsonIgnore
    public boolean isRallyMaster() {
        return RallyParticipantType.ORGANIZER.equals(participantType);
    }
}
