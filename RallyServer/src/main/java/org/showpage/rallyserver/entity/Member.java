package org.showpage.rallyserver.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import org.showpage.rallyserver.interfaces.HasId;

import java.util.List;

@Entity
@Table(name = "member")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Member implements HasId<Member> {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(unique = true, nullable = false)
    private String email;

    @JsonIgnore
    @Column(nullable = false)
    private String password;

    private String spotwallaUsername;

    @JsonIgnore
    @Column(length = 512)
    private String refreshToken;

    @Builder.Default
    private Boolean isAdmin = Boolean.FALSE;

    @OneToMany(
            mappedBy = "member",                // owning side is RallyParticipant.rally
            cascade = CascadeType.ALL,         // persist/update/remove participants with the rally
            orphanRemoval = true,              // remove rows when detached from collection
            fetch = FetchType.LAZY
    )
    private List<Motorcycle> motorcycles;
}