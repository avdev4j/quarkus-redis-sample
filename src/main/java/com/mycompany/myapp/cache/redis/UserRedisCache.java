package com.mycompany.myapp.cache.redis;

import com.mycompany.myapp.domain.User;

import javax.inject.Singleton;

/**
 * This cache manager is used to manage users in the Redis cache.
 *
 * An entry per user and per identifier.
 *
 * e.g. USER:admin or USER:admin@localhost (or anything you want)
 */
@Singleton
public class UserRedisCache extends RedisCache<User> {

    public UserRedisCache() {
        super("USER:");
    }
}
