package org.showpage.rallyserver.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

/**
 * This represents a claimed Bonus Point. Confirmed gets set true by the scorer. Points come from
 * the BonusPoint record.
 */
@Entity
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EarnedBonusPoint {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "rally_participant_id")
    private RallyParticipant rallyParticipant;

    @Column(name = "rally_participant_id", insertable=false, updatable=false)
    private Integer rallyParticipantId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "bonus_point_id")
    private BonusPoint bonusPoint;

    @Column(name="bonus_point_id", insertable=false, updatable=false)
    private Integer bonusPointId;

    private Integer odometer;
    private Instant earnedAt;
    private Boolean confirmed;
}
