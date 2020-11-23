package com.mycompany.myapp.cache.redis;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.mycompany.myapp.domain.User;
import io.quarkus.test.junit.QuarkusTest;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import redis.embedded.RedisServer;

import javax.inject.Inject;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.AssertionsForClassTypes.catchThrowable;

@QuarkusTest
class RedisCacheTest {
    @Inject
    FooRedisCache fooRedisCache;
    @Inject
    UserRedisCache userRedisCache;

    private final String FOO_PREFIX = "Foo:";
    static RedisServer server;

    @BeforeAll
    public static void setup() throws IOException {
        server = new RedisServer(6379);
        server.start();
    }

    @AfterAll
    public static void tearDown() {
        server.stop();
    }

    @AfterEach
    public void clearCache() {
        fooRedisCache.clear();
    }

    private Foo foo() {
        return foo(null);
    }

    private Foo foo(String name){
        Foo foo = new Foo();
        foo.name = name == null ? "bar" : name;
        foo.age = 42;

        return foo;
    }

    @Test
    void should_FooPrefixToBeTheExpectedOne() {
        assertThat(fooRedisCache.prefix).isEqualTo(FOO_PREFIX);
    }

    @Test
    void should_GenerateKeyForFooToReturnPrefixAndName() {
        String fooName = "bar";
        assertThat(fooRedisCache.generateKey(fooName)).isEqualTo(FOO_PREFIX + fooName);
    }

    @Test
    void should_StoreAFooShouldContainsTheObjectInCache() {
        Foo foo = foo();

        fooRedisCache.set(foo.name, foo);
        Optional<Foo> fooInCache = fooRedisCache.get(foo.name, null);

        assertThat(fooInCache).containsInstanceOf(Foo.class);
        assertThat(fooInCache.get().name).isEqualTo(foo.name);
        assertThat(fooInCache.get().age).isEqualTo(foo.age);
    }

    @Test
    void should_StoreAFooAndEvictItLetTheCacheEmpty() {
        Foo foo = foo();
        String fooKey = fooRedisCache.generateKey(foo.name);

        fooRedisCache.set(foo.name, foo);
        List<String> keysAfterSet = fooRedisCache.keys();
        assertThat(keysAfterSet).contains(fooKey);

        fooRedisCache.evict(foo.name);
        Optional<Foo> fooInCache = fooRedisCache.get(foo.name, null);
        List<String> keysAfterEvict = fooRedisCache.keys();

        assertThat(fooInCache).isEmpty();
        assertThat(keysAfterEvict).isEmpty();
    }

    @Test
    void should_InsertTwiceAnFooShouldContainOnlyOneKey() {
        Foo foo = foo();
        String fooKey = fooRedisCache.generateKey(foo.name);

        fooRedisCache.set(foo.name, foo);
        fooRedisCache.set(foo.name, foo);
        List<String> keysAfterSet = fooRedisCache.keys();

        assertThat(keysAfterSet).hasSize(1).contains(fooKey);
    }

    @Test
    void should_InsertTwiceTwoFoosShouldContainTwoDifferentKeys() {
        Foo foo1 = foo();
        Foo foo2 = foo("otherBar");
        String foo1Key = fooRedisCache.generateKey(foo1.name);
        String foo2Key = fooRedisCache.generateKey(foo2.name);

        fooRedisCache.set(foo1.name, foo1);
        fooRedisCache.set(foo2.name, foo2);
        List<String> keysAfterSet = fooRedisCache.keys();

        assertThat(keysAfterSet).hasSize(2).containsAll(Arrays.asList(foo1Key, foo2Key));
    }

    @Test
    void should_ClearCacheEvictAllFooKeys() {
        Foo foo1 = foo();
        Foo foo2 = foo("otherBar");

        fooRedisCache.set(foo1.name, foo1);
        fooRedisCache.set(foo2.name, foo2);
        List<String> keysAfterSet = fooRedisCache.keys();

        assertThat(keysAfterSet).hasSize(2);

        fooRedisCache.clear();
        List<String> keysAfterClear = fooRedisCache.keys();

        assertThat(keysAfterClear).isEmpty();
    }

    @Test
    void should_NullPointerExceptionIsThrownWhenSetIsCalledAnNullIdentifier() {
        Throwable thrown = catchThrowable(() -> {
            fooRedisCache.set(null, new Foo());
        });

        assertThat(thrown)
            .isInstanceOf(NullPointerException.class)
            .hasMessageContaining("Null keys are not supported");
    }

    @Test
    void should_NullPointerExceptionIsThrownWhenGetIsCalledAnNullIdentifier() {
        Throwable thrown = catchThrowable(() -> {
            fooRedisCache.get(null, null);
        });

        assertThat(thrown)
            .isInstanceOf(NullPointerException.class)
            .hasMessageContaining("Null keys are not supported");
    }

    @Test
    void should_NullPointerExceptionIsThrownWhenEvictIsCalledAnNullIdentifier() {
        Throwable thrown = catchThrowable(() -> {
            Object testedObject = null;
            fooRedisCache.evict(testedObject);
        });

        assertThat(thrown)
            .isInstanceOf(NullPointerException.class)
            .hasMessageContaining("Null keys are not supported");
    }

    @Test
    void should_NullPointerExceptionIsThrownWhenEvictIsCalledAnNullCollectionOfIdentifier() {
        Throwable thrown = catchThrowable(() -> {
            fooRedisCache.evict(null);
        });

        assertThat(thrown)
            .isInstanceOf(NullPointerException.class)
            .hasMessageContaining("Null keys are not supported");
    }

    @Test
    void should_NotExecuteSetIfValueIsNull() {
        fooRedisCache.set("fakeIdentifier", null);

        List<String> keysAfterSet = fooRedisCache.keys();

        assertThat(keysAfterSet).isEmpty();
    }

    @Test
    void should_ReturnValueFromValueLoaderIfElementIsNotInTheCacheAndGoSet() {
        Foo notFromCache = foo("notFromCache");
       Optional<Foo> result = fooRedisCache.get("fakeIdentifier", () -> notFromCache);

        assertThat(result).isNotEmpty().contains(notFromCache);
    }

    @Test
    void should_ReturnNullIfSerializeNull() throws JsonProcessingException {
        assertThat(fooRedisCache.serialize(null)).isNull();
    }

    @Test
    void should_StoreTwoDifferentReturnOneByCacheType() {
        Foo foo = foo();
        User user = new User();
        user.login = "johnDoe";

        fooRedisCache.set(foo.name, foo);
        userRedisCache.set(user.login, user);
        List<String> fooKeysAfterSet = fooRedisCache.keys();
        List<String> userKeysAfterSet = userRedisCache.keys();

        assertThat(fooKeysAfterSet).hasSize(1).contains(fooRedisCache.generateKey(foo.name));
        assertThat(userKeysAfterSet).hasSize(1).contains(userRedisCache.generateKey(user.login));
    }
}
