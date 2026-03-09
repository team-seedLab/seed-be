package com.example.seedbe.global.security.oauth2;

import com.example.seedbe.domain.user.entity.User;
import com.example.seedbe.domain.user.enums.Role;
import com.example.seedbe.domain.user.repository.UserRepository;
import com.example.seedbe.global.security.CustomUserDetails;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;

import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class CustomOAuth2UserService extends DefaultOAuth2UserService {

    private final UserRepository userRepository;

    @Override
    public OAuth2User loadUser(OAuth2UserRequest userRequest) throws OAuth2AuthenticationException {
        // 카카오/구글에서 유저 정보를 땡겨옴
        OAuth2User oAuth2User = super.loadUser(userRequest);

        String registrationId = userRequest.getClientRegistration().getRegistrationId();

        // 플랫폼마다 다른 데이터 구조를 파싱 (아래에 헬퍼 메서드로 뺌)
        OAuth2UserInfo userInfo = extractUserInfo(registrationId, oAuth2User.getAttributes());

        User user = userRepository.findByProviderAndProviderId(registrationId, userInfo.providerId())
                .map(existingUser -> existingUser.updateProfile(userInfo.nickname(), userInfo.profileUrl()))
                .orElseGet(() -> User.builder()
                        .provider(registrationId)
                        .providerId(userInfo.providerId())
                        .email(userInfo.email())
                        .nickname(userInfo.nickname())
                        .profileUrl(userInfo.profileUrl())
                        .role(Role.ROLE_USER)
                        .build());

        userRepository.save(user);

        return new CustomUserDetails(user, oAuth2User.getAttributes());
    }

    // 플랫폼별 데이터 파싱 (단순화를 위해 Record 사용)
    private record OAuth2UserInfo(String providerId, String email, String nickname, String profileUrl) {}

    private OAuth2UserInfo extractUserInfo(String registrationId, Map<String, Object> attributes) {
        if ("google".equals(registrationId)) {
            return new OAuth2UserInfo(
                    String.valueOf(attributes.get("sub")),
                    String.valueOf(attributes.get("email")),
                    String.valueOf(attributes.get("name")),
                    String.valueOf(attributes.get("picture"))
            );
        } else if ("kakao".equals(registrationId)) {
            Map<String, Object> kakaoAccount = (Map<String, Object>) attributes.get("kakao_account");
            Map<String, Object> profile = kakaoAccount != null ? (Map<String, Object>) kakaoAccount.get("profile") : null;
            return new OAuth2UserInfo(
                    String.valueOf(attributes.get("id")),
                    kakaoAccount != null ? String.valueOf(kakaoAccount.get("email")) : null,
                    profile != null ? String.valueOf(profile.get("nickname")) : null,
                    profile != null ? String.valueOf(profile.get("profile_image_url")) : null
            );
        }
        throw new IllegalArgumentException("지원하지 않는 소셜 로그인입니다: " + registrationId);
    }
}
