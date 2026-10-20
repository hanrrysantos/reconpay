package br.com.hanrry.reconpay.security;

import br.com.hanrry.reconpay.auth.entity.UserEntity;
import br.com.hanrry.reconpay.auth.repository.IUserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.time.Clock;

@Service
@RequiredArgsConstructor
public class UserDetailsServiceImpl implements UserDetailsService {

    private final IUserRepository userRepository;
    private final Clock clock;

    @Override
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        UserEntity user = userRepository.findByEmail(email)
                .orElseThrow(
                        () -> new UsernameNotFoundException("User not found with this email:" + email)
                );
        // An expired demo account is treated as gone; the JWT filter turns this into a 401.
        if (user.getDemoExpiresAt() != null && !user.getDemoExpiresAt().isAfter(clock.instant())) {
            throw new UsernameNotFoundException("Demo account expired: " + email);
        }
        return new CustomUserDetails(user);
    }
}
