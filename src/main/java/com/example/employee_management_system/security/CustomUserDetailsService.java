package com.example.employee_management_system.security;

import com.example.employee_management_system.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class CustomUserDetailsService implements UserDetailsService {

    private final UserRepository userRepository;

     // Implement the loadUserByUsername method to fetch user details from the database
     @Override
     public UserDetails loadUserByUsername(String username) {

         var user = userRepository.findByUsername(username)
                 .orElseThrow(() ->
                         new UsernameNotFoundException("User not found"));

         return new CustomUserDetails(user);
     }
}
