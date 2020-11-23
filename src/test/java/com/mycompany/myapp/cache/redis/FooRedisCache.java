package com.mycompany.myapp.cache.redis;

import javax.inject.Singleton;

@Singleton
public class FooRedisCache extends RedisCache<Foo> {

    public FooRedisCache() {
        super("Foo:");
    }
}
