package com.example.demo.member.domain;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.LinkedHashSet;
import java.util.Set;

@Entity
@Getter
@Table(name = "members")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Member {

    @Id
    @Column(name = "member_id", length = 100)
    private String memberId;

    @Column(name = "display_name", nullable = false, length = 100)
    private String displayName;

    @Column(name = "encoded_password", nullable = false, length = 100)
    private String encodedPassword;

    @Column(nullable = false)
    private boolean enabled;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(
        name = "member_roles",
        joinColumns = @JoinColumn(name = "member_id"),
        uniqueConstraints = @UniqueConstraint(
            name = "uk_member_role",
            columnNames = {"member_id", "role"}
        )
    )
    @Column(name = "role", nullable = false, length = 30)
    @Enumerated(EnumType.STRING)
    private Set<MemberRole> roles = new LinkedHashSet<>();

    private Member(
        String memberId,
        String displayName,
        String encodedPassword,
        boolean enabled,
        Set<MemberRole> roles
    ) {
        this.memberId = memberId;
        this.displayName = displayName;
        this.encodedPassword = encodedPassword;
        this.enabled = enabled;
        this.roles = new LinkedHashSet<>(roles);
    }

    public static Member create(String memberId, String displayName, String encodedPassword, boolean enabled, Set<MemberRole> roles) {
        return new Member(memberId, displayName, encodedPassword, enabled, roles);
    }

}
