package com.badminton.shop.modules.auth.security.oauth2;

import com.badminton.shop.modules.auth.entity.AuthProvider;
import com.badminton.shop.modules.auth.entity.Role;
import com.badminton.shop.modules.auth.entity.User;
import com.badminton.shop.modules.auth.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
@RequiredArgsConstructor
public class CustomOAuth2UserService extends DefaultOAuth2UserService {

    private final UserRepository userRepository;

    @Override
    public OAuth2User loadUser(OAuth2UserRequest userRequest) throws OAuth2AuthenticationException {
        OAuth2User oAuth2User = super.loadUser(userRequest);
        String registrationId = userRequest.getClientRegistration().getRegistrationId();
        
        return processOAuth2User(registrationId, oAuth2User);
    }

    private OAuth2User processOAuth2User(String registrationId, OAuth2User oAuth2User) {
        String email = oAuth2User.getAttribute("email");
        if (email == null) {
            throw new OAuth2AuthenticationException("Email not found from OAuth2 provider");
        }

        Optional<User> userOptional = userRepository.findByEmail(email);
        User user;
        if (userOptional.isPresent()) {
            user = userOptional.get();
            if (!user.getProvider().equals(AuthProvider.valueOf(registrationId.toUpperCase()))) {
                throw new OAuth2AuthenticationException("Looks like you're signed up with " +
                        user.getProvider() + " account. Please use your " + user.getProvider() +
                        " account to login.");
            }
            user = updateExistingUser(user, oAuth2User);
        } else {
            user = registerNewUser(registrationId, oAuth2User);
        }

        return CustomOAuth2User.create(user, oAuth2User.getAttributes());
    }

    private User registerNewUser(String registrationId, OAuth2User oAuth2User) {
        User user = User.builder()
                .username(oAuth2User.getAttribute("email")) // Use email as username for social login
                .email(oAuth2User.getAttribute("email"))
                .provider(AuthProvider.valueOf(registrationId.toUpperCase()))
                .providerId(oAuth2User.getName())
                .role(Role.CUSTOMER)
                .isActive(true)
                .isEmailVerified(true)
                .build();
        return userRepository.save(user);
    }

    private User updateExistingUser(User existingUser, OAuth2User oAuth2User) {
        // Update user info if needed
        return userRepository.save(existingUser);
    }
}
