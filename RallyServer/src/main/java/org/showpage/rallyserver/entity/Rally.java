package org.showpage.rallyserver.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.showpage.rallyserver.interfaces.HasId;

import java.time.LocalDate;
import java.util.List;

@Entity
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Rally implements HasId<Rally> {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    private String name;
    private String description;

    private LocalDate startDate;
    private LocalDate endDate;
    private String locationCity;
    private String locationState;
    private String locationCountry; /** 2-digit code. */

    private Boolean isPublic;
    private Boolean pointsPublic;
    private Boolean ridersPublic;
    private Boolean organizersPublic;

    @OneToMany(
            mappedBy = "rally",                // owning side is RallyParticipant.rally
            cascade = CascadeType.ALL,         // persist/update/remove participants with the rally
            orphanRemoval = true,              // remove rows when detached from collection
            fetch = FetchType.LAZY
    )
    private List<RallyParticipant> participants;

    @OneToMany(
            mappedBy = "rally",                // owning side is RallyParticipant.rally
            cascade = CascadeType.ALL,         // persist/update/remove participants with the rally
            orphanRemoval = true,              // remove rows when detached from collection
            fetch = FetchType.LAZY
    )
    private List<BonusPoint> bonusPoints;

    @OneToMany(
            mappedBy = "rally",                // owning side is RallyParticipant.rally
            cascade = CascadeType.ALL,         // persist/update/remove participants with the rally
            orphanRemoval = true,              // remove rows when detached from collection
            fetch = FetchType.LAZY
    )
    private List<Combination> combinations;
}
