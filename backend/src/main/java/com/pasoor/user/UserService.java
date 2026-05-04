package com.pasoor.user;

import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class UserService {
    private final UserRepository userRepository;

    public UserService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Transactional
    public User syncOAuthUser(OAuth2User principal) {
        return syncOAuthUser(toProfile(principal));
    }

    @Transactional
    public User syncOAuthUser(OAuthUserProfile profile) {
        User user = userRepository.findByGoogleSubject(profile.googleSubject())
                .orElseGet(() -> new User(profile.googleSubject(), profile.name(), profile.email(), profile.avatarUrl()));

        user.setName(profile.name());
        user.setEmail(profile.email());
        user.setAvatarUrl(profile.avatarUrl());

        return userRepository.save(user);
    }

    private OAuthUserProfile toProfile(OAuth2User principal) {
        String googleSubject = attribute(principal, "sub");
        String name = attribute(principal, "name");
        String email = attribute(principal, "email");
        String avatarUrl = principal.getAttribute("picture");

        if (googleSubject == null || googleSubject.isBlank()) {
            throw new IllegalArgumentException("OAuth profile is missing Google subject.");
        }
        if (email == null || email.isBlank()) {
            throw new IllegalArgumentException("OAuth profile is missing email.");
        }
        if (name == null || name.isBlank()) {
            name = email;
        }

        return new OAuthUserProfile(googleSubject, name, email, avatarUrl);
    }

    private String attribute(OAuth2User principal, String name) {
        Object value = principal.getAttribute(name);
        return value instanceof String stringValue ? stringValue : null;
    }
}

