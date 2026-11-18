package com.simbio.core.identity.infrastructure;

import java.util.Locale;

import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.simbio.core.identity.application.AccountDetailsLoader;
import com.simbio.core.identity.application.UserAccountStore;
import com.simbio.core.identity.security.PortalUserPrincipal;

@Service
class PortalUserDetailsService implements UserDetailsService, AccountDetailsLoader {

    private final UserAccountStore accounts;

    PortalUserDetailsService(UserAccountStore accounts) {
        this.accounts = accounts;
    }

    @Override
    @Transactional(readOnly = true)
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        if (username == null || username.length() > 320) {
            throw new UsernameNotFoundException("Invalid credentials");
        }
        return accounts.findByEmail(username.trim().toLowerCase(Locale.ROOT))
                .map(PortalUserPrincipal::new)
                .orElseThrow(() -> new UsernameNotFoundException("Invalid credentials"));
    }
}
