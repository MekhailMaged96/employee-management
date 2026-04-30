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

     // ✅ Use findWithRolesByUsername (@EntityGraph) so roles are loaded eagerly.
     //    Without this, accessing user.getRoles() outside a transaction would
     //    throw LazyInitializationException because the filter runs outside @Transactional.
     @Override
     public UserDetails loadUserByUsername(String username) {

         var user = userRepository.findWithRolesByUsername(username)
                  .orElseThrow(() ->
                          new UsernameNotFoundException("User not found"));

          return new CustomUserDetails(user);
      }
}
