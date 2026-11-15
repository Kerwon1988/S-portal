package com.simbio.core.identity.application;

import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

public interface AccountDetailsLoader {

    UserDetails loadUserByUsername(String username) throws UsernameNotFoundException;
}
