package com.mycompany.myapp.service.redis;

import com.mycompany.myapp.domain.User;

import javax.inject.Singleton;

@Singleton
public class UserRedisCache extends RedisCache<User> {
}
