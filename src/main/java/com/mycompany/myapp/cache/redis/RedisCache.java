package com.mycompany.myapp.cache.redis;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mycompany.myapp.cache.CacheErrorException;
import io.quarkus.redis.client.RedisClient;
import io.vertx.redis.client.Response;
import java.lang.reflect.ParameterizedType;
import java.util.*;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import javax.inject.Inject;

/**
 * This class represent a cache manager for the given object T.
 * Each object managed by the Redis cache should have a proper implementation
 * and define a prefix through the constructor.
 *
 * @param <T> The object to handle in the cache
 */
public abstract class RedisCache<T> {
    public static final String NULL_KEYS_NOT_SUPPORTED_MSG = "Null keys are not supported";

    @Inject
    RedisClient redis;

    @Inject
    ObjectMapper objectMapper;

    final Class<T> type;
    /**
     * The prefix is used to define the key for storing purpose.
     * It should have a delimiter, like : / - ... to make the research easier.
     * e.g. "USER:"
     *
     * The value is define in the constructor and it's used in the generateKey() method
     * or to retrieve all current keys in the cache through keys() method.
     */
    final String prefix;

    public RedisCache(String prefix) {
        this.type = (Class<T>) ((ParameterizedType) getClass().getGenericSuperclass()).getActualTypeArguments()[0];
        this.prefix = prefix;
    }

    /**
     * This method generate the key used to store an element in the cache.
     * The identifier has to be unique to form an unique key when it combines with the prefix.
     *
     * @param identifier an unique identifier for a given object to store.
     * @return The key used to store the element in the store
     */
    final String generateKey(Object identifier) {
        return prefix + identifier;
    }

    /**
     * Return the value in the cache or launch the Supplier lambda if no value is present in the cache.
     *
     * @param identifier the unique object's identifier to store
     * @param valueLoader a lambda used to load the value in case cache is empty
     * @return The value found in the cache or retrieved by the supplier
     */
    public Optional<T> get(Object identifier, Supplier<T> valueLoader) {
        if (identifier == null) {
            throw new NullPointerException(NULL_KEYS_NOT_SUPPORTED_MSG);
        }
        T result;

        try {
            String key = generateKey(identifier);
            result = deserialize(redis.get(key));
        } catch (JsonProcessingException e) {
            throw new CacheErrorException(e);
        }

        if (result == null && valueLoader != null) {
            result = valueLoader.get();
            this.set(identifier, result);
        }

        return Optional.ofNullable(result);
    }

    /**
     * Store an object in the cache.
     *
     * @param identifier the unique object's identifier to store (could not be null)
     * @param value the value to store
     */
    public void set(Object identifier, T value) {
        if (identifier == null) {
            throw new NullPointerException(NULL_KEYS_NOT_SUPPORTED_MSG);
        }
        if (value == null) {
            return;
        }
        String key = generateKey(identifier);

        try {
            redis.set(Arrays.asList(key, serialize(value)));
        } catch (JsonProcessingException e) {
            throw new CacheErrorException(e);
        }
    }

    /**
     * Remove all entries in the cache according to keys returned by the keys() method.
     */
    public void clear() {
        evict(keys());
    }

    /**
     * Remove an element from the cache according to the given identifier
     * @param identifier the unique object's identifier to remove
     */
    public void evict(Object identifier) {
        if (identifier == null) {
            throw new NullPointerException(NULL_KEYS_NOT_SUPPORTED_MSG);
        }

        this.evict(Collections.singletonList(identifier));
    }

    /**
     * Remove elements from the cache according to the given identifiers
     * @param identifiers a list of identifier to remove
     */
    public void evict(List<Object> identifiers) {
        if (identifiers == null) {
            throw new NullPointerException(NULL_KEYS_NOT_SUPPORTED_MSG);
        }
        List<String> finalKeys = identifiers.stream().filter(Objects::nonNull).map(this::generateKey).collect(Collectors.toList());

        redis.del(finalKeys);
    }

    /**
     * Retrieve all keys from the cache according the prefix value.
     * e.g. for User entity the searched term would be "USER:*"
     *
     * @return A List of keys from the cache
     */
    public List<String> keys() {
        return redis.keys(prefix + "*").stream().map(Object::toString).collect(Collectors.toList());
    }

    protected T deserialize(Response response) throws JsonProcessingException {
        if (response == null) {
            return null;
        }

        return objectMapper.readValue(response.toString(), this.type);
    }

    protected String serialize(T value) throws JsonProcessingException {
        if (value == null) {
            return null;
        }

        return objectMapper.writeValueAsString(value);
    }
}
