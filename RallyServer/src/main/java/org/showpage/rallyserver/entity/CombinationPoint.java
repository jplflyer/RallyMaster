package org.showpage.rallyserver.entity;


import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * For Combination bonuses, this identifies one BonusPoint that must be visited.
 */
@Entity
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CombinationPoint {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false,  cascade = CascadeType.ALL)
    @JoinColumn(name = "combination_id")
    private Combination combination;

    @Column(name = "combination_id", updatable = false, insertable = false)
    private Integer combinationId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "bonus_point_id")
    private BonusPoint bonusPoint;

    @Column(name = "bonus_point_id", updatable = false, insertable = false)
    private Integer bonusPointId;

    private Boolean required;
}
