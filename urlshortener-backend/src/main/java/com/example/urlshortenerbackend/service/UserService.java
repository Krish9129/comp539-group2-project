package com.example.urlshortenerbackend.service;

import com.example.urlshortenerbackend.model.UserEntity;
import com.example.urlshortenerbackend.repository.BigtableRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
public class UserService {

    @Autowired
    private BigtableRepository bigtableRepository;

    public Optional<UserEntity> getUserByProviderAndId(String provider, String providerId) {
        return bigtableRepository.getUserByProviderAndId(provider, providerId);
    }

    public void saveUser(UserEntity userEntity) {
        bigtableRepository.saveUser(userEntity);
    }

}