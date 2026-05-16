package com.demo.event.security;

import com.demo.event.model.entity.User;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import java.util.Collection;
import java.util.stream.Collectors;

@RequiredArgsConstructor
public class CustomUserDetails implements UserDetails {

    @Getter
    private final User user;

    /** Chuyen Set<Role> thanh Collection<GrantedAuthority> cho Spring Security. */
    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return user.getRoles().stream()
                .map(role -> new SimpleGrantedAuthority(role.getName()))
                .collect(Collectors.toSet());
    }

    @Override public String getPassword()   { return user.getPasswordHash(); }
    @Override public String getUsername()   { return user.getEmail(); }
    @Override public boolean isAccountNonExpired()   { return true; }
    @Override public boolean isAccountNonLocked()    { return true; }
    @Override public boolean isCredentialsNonExpired(){ return true; }
    @Override public boolean isEnabled() {
        return Boolean.TRUE.equals(user.getIsActive());
    }

    /** Tien ich: lay userId truc tiep (dung trong @AuthenticationPrincipal). */
    public Long getUserId() { return user.getId(); }
}
