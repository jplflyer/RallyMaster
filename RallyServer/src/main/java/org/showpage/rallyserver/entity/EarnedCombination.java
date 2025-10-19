package org.showpage.rallyserver.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * This represents a claimed earned Combination. The confirmed flag gets set true/false by the scorer.
 */
@Entity
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EarnedCombination {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name="rally_participant_id")
    private RallyParticipant rallyParticipant;

    @Column(name="rally_participant_id", insertable=false, updatable=false)
    private Integer rallyParticipantId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name="combination_id")
    private Combination combination;

    @Column(name="combination_id", insertable=false, updatable=false)
    private Integer combinationId;

    private Boolean confirmed;
}
