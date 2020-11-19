package com.mycompany.myapp.cache.redis;

import com.mycompany.myapp.domain.User;

import javax.inject.Singleton;

@Singleton
public class UserRedisCache extends RedisCache<User> {

    public UserRedisCache() {
        super("USER:");
    }

    @Override
    String generateKey(Object identifier) {
        return prefix + identifier;
    }
}
