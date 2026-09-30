package com.rbownworkshop.server.enumtype;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static com.rbownworkshop.server.enumtype.Permission.*;

@RequiredArgsConstructor
public enum Role {

    UN_AUTH(Collections.emptySet()),

    ADMIN(Set.of(
            ADMIN_READ,
            ADMIN_UPDATE,
            ADMIN_DELETE,
            ADMIN_CREATE
    )),

    USER(Set.of(
            USER_READ,
            USER_UPDATE,
            USER_DELETE,
            USER_CREATE
    ));

    @Getter
    private final Set<Permission> permissions;

    public String getLabel() {
        return switch (this) {
            case UN_AUTH -> "Chưa xác thực";
            case ADMIN   -> "Administrator";
            case USER    -> "Người dùng";
        };
    }

    public List<SimpleGrantedAuthority> getAuthorities() {
        var authorities = getPermissions()
                .stream()
                .map(permission -> new SimpleGrantedAuthority(permission.getPermission()))
                .collect(Collectors.toList());

        authorities.add(
                new SimpleGrantedAuthority("ROLE_" + this.name())
        );

        return authorities;
    }
}
