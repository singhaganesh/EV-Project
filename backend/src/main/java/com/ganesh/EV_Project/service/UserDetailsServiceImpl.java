package com.ganesh.EV_Project.service;

import com.ganesh.EV_Project.model.User;
import com.ganesh.EV_Project.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.Collections;

@Service
public class UserDetailsServiceImpl implements UserDetailsService {

    @Autowired
    private UserRepository userRepository;

    @Override
    public UserDetails loadUserByUsername(String mobileNumber) throws UsernameNotFoundException {
        User user = userRepository.findByMobileNumber(mobileNumber)
                .orElseThrow(() -> new UsernameNotFoundException("User not found with mobile number: " + mobileNumber));

        return new org.springframework.security.core.userdetails.User(
                user.getMobileNumber(),
                "", // No password for OTP-based auth
                Collections.singletonList(new SimpleGrantedAuthority("ROLE_" + user.getRole())));
    }
}
