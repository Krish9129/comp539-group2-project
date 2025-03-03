package com.example.urlshortenerbackend.security;

import com.example.urlshortenerbackend.model.UserEntity;
import com.example.urlshortenerbackend.repository.BigtableRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.user.DefaultOAuth2User;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

@Service
public class CustomOAuth2UserService extends DefaultOAuth2UserService {

    @Autowired
    private BigtableRepository bigtableRepository;


    @Override
    public OAuth2User loadUser(OAuth2UserRequest userRequest) throws OAuth2AuthenticationException {
        try {
            OAuth2User oauth2User = super.loadUser(userRequest);
            return processOAuth2User(userRequest, oauth2User);
        } catch (Exception ex) {
            // 添加更详细的日志
            ex.printStackTrace();
            System.out.println("OAuth2 authentication error: " + ex.getMessage());
            // 重新抛出异常
            throw new OAuth2AuthenticationException(ex.getMessage());
        }
    }

    private OAuth2User processOAuth2User(OAuth2UserRequest userRequest, OAuth2User oauth2User) {
        String registrationId = userRequest.getClientRegistration().getRegistrationId();
        OAuth2UserInfo oAuth2UserInfo = OAuth2UserInfoFactory.getOAuth2UserInfo(
                registrationId, oauth2User.getAttributes());
        System.out.println(oAuth2UserInfo);

        // 处理用户信息
        Optional<UserEntity> userOptional = bigtableRepository.getUserByProviderAndId(
                registrationId, oAuth2UserInfo.getId());

        UserEntity user;
        if (userOptional.isPresent()) {
            user = userOptional.get();
            user = updateExistingUser(user, oAuth2UserInfo);
        } else {
            user = registerNewUser(registrationId, oAuth2UserInfo);
        }

        // 保存用户
        bigtableRepository.saveUser(user);

        // 创建一个新的可修改的 Map
        Map<String, Object> attributes = new HashMap<>(oauth2User.getAttributes());
        attributes.put("provider", registrationId);
        attributes.put("providerId", oAuth2UserInfo.getId());

        return new DefaultOAuth2User(
                Collections.singleton(new SimpleGrantedAuthority("ROLE_" + user.getRole())),
                attributes,
                "email"
        );
    }

    private UserEntity updateExistingUser(UserEntity user, OAuth2UserInfo oAuth2UserInfo) {
        user.setName(oAuth2UserInfo.getName());
        user.setEmail(oAuth2UserInfo.getEmail());
        user.setPictureUrl(oAuth2UserInfo.getImageUrl());
        user.setLastLogin(Instant.now().toString());
        return user;
    }

    private UserEntity registerNewUser(String registrationId, OAuth2UserInfo oAuth2UserInfo) {
        UserEntity user = new UserEntity();
        user.setProvider(registrationId);
        user.setProviderId(oAuth2UserInfo.getId());
        user.setName(oAuth2UserInfo.getName());
        user.setEmail(oAuth2UserInfo.getEmail());
        user.setPictureUrl(oAuth2UserInfo.getImageUrl());
        user.setRole("USER"); // Default role
        user.setLastLogin(Instant.now().toString());
        return user;
    }
}