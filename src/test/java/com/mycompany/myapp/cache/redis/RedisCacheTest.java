package com.mycompany.myapp.cache.redis;

import com.mycompany.myapp.domain.User;
import io.quarkus.test.junit.QuarkusTest;
import javax.inject.Inject;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import redis.embedded.RedisServer;

import java.io.IOException;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;

@QuarkusTest
class RedisCacheTest {
    @Inject
    UserRedisCache userRedisCache;

    private final String USER_PREFIX = "USER:";
    static RedisServer server;

    @BeforeAll
    public static void setup() throws IOException {
        server = new RedisServer(6379);
        server.start();
    }

    @AfterAll
    public static void tearDown() throws IOException {
        server.stop();
    }

    @AfterEach
    public void clearCache() throws IOException {
        userRedisCache.clear();
    }

    private User user(){
        User user = new User();
        user.login = "foo";
        user.email = "foo@localhost";

        return user;
    }

    @Test
    void should_UserPrefixToBeTheExpectedOne() {
        assertThat(userRedisCache.prefix).isEqualTo(USER_PREFIX);
    }

    @Test
    void should_GenerateKeyForFooUserToReturnPrefixAndFoo() {
        String userFieldName = "foo";
        assertThat(userRedisCache.generateKey(userFieldName)).isEqualTo(USER_PREFIX + userFieldName);
        userRedisCache.set(userFieldName, new User());
    }


    @Test
    void should_StoreAnUserShouldContainsTheObjectInCache() {
        User user = new User();
        user.login = "foo";
        user.email = "foo@localhost";

        userRedisCache.set(user.login, user);
        Optional<User> userInCache = userRedisCache.get(user.login, null);

        assertThat(userInCache).containsInstanceOf(User.class);
        assertThat(userInCache.get().login).isEqualTo(user.login);
        assertThat(userInCache.get().email).isEqualTo(user.email);
    }

    @Test
    void should_StoreAnUserAndEvictItLetTheCacheEmpty() {
        User user = user();
        String userKey = userRedisCache.generateKey(user.login);

        userRedisCache.set(user.login, user);
        List<String> keysAfterSet = userRedisCache.keys();
        assertThat(keysAfterSet).contains(userKey);

        userRedisCache.evict(user.login);
        Optional<User> userInCache = userRedisCache.get(user.login, null);
        List<String> keysAfterEvict = userRedisCache.keys();

        assertThat(userInCache).isEmpty();
        assertThat(keysAfterEvict).isEmpty();
    }

    @Test
    void should_InsertTwiceAnUserShouldContainOnlyOneKey() {
        User user = user();
        String userKey = userRedisCache.generateKey(user.login);

        userRedisCache.set(user.login, user);
        userRedisCache.set(user.login, user);
        List<String> keysAfterSet = userRedisCache.keys();

        assertThat(keysAfterSet).hasSize(1).contains(userKey);
    }

    @Test
    void should_InsertTwiceTwoUsersShouldContainTwoDifferentKeys() {
        User user1 = user();
        User user2 = user();
        user2.login = "bar";
        String user1Key = userRedisCache.generateKey(user1.login);
        String user2Key = userRedisCache.generateKey(user2.login);

        userRedisCache.set(user1.login, user1);
        userRedisCache.set(user2.login, user2);
        List<String> keysAfterSet = userRedisCache.keys();

        assertThat(keysAfterSet).hasSize(2).contains(user1Key).contains(user2Key);
    }

    @Test
    void should_ClearCacheEvictAllUserKeys() {
        User user1 = user();
        User user2 = user();
        user2.login = "bar";

        userRedisCache.set(user1.login, user1);
        userRedisCache.set(user2.login, user2);
        List<String> keysAfterSet = userRedisCache.keys();

        assertThat(keysAfterSet).hasSize(2);

        userRedisCache.clear();
        List<String> keysAfterClear = userRedisCache.keys();

        assertThat(keysAfterClear).isEmpty();
    }

}
