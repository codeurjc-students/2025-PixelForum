package es.codeurjc.backend.security;

import java.util.ArrayList;
import java.util.List;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import es.codeurjc.backend.model.User;
import es.codeurjc.backend.repository.UserRepository;

@Service
public class RepositoryUserDetailsService implements UserDetailsService {

	private final UserRepository userRepository;

	public RepositoryUserDetailsService(UserRepository userRepository) {
		this.userRepository = userRepository;
	}

	@Override
	public UserDetails loadUserByUsername(String input) throws UsernameNotFoundException {

		User user;

		if (input.matches("\\d+")) {
			Long id = Long.parseLong(input);
			user = userRepository.findById(id)
					.orElseThrow(() -> new UsernameNotFoundException("User not found"));
		} else {
			user = userRepository.findByUsername(input)
					.orElseThrow(() -> new UsernameNotFoundException("User not found"));
		}

		List<GrantedAuthority> roles = new ArrayList<>();
		for (String role : user.getRoles()) {
			roles.add(new SimpleGrantedAuthority("ROLE_" + role));
		}

		return new org.springframework.security.core.userdetails.User(user.getUsername(),
				user.getPassword(), roles);

	}
}