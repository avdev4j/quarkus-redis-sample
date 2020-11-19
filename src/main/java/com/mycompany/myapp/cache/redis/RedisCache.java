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

public abstract class RedisCache<T> {
    public static final String NULL_KEYS_NOT_SUPPORTED_MSG = "Null keys are not supported";

    @Inject
    RedisClient redis;

    @Inject
    ObjectMapper objectMapper;

    final Class<T> type;
    final String prefix;

    public RedisCache(String prefix) {
        this.type = (Class<T>) ((ParameterizedType) getClass().getGenericSuperclass()).getActualTypeArguments()[0];
        this.prefix = prefix;
    }

    abstract String generateKey(Object identifier);

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

        if (result == null) {
            result = valueLoader.get();
            this.set(identifier, result);
        }

        return Optional.ofNullable(result);
    }

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

    public void clear() {
        evict(keys());
    }

    public void evict(Object identifier) {
        if (identifier == null) {
            throw new NullPointerException(NULL_KEYS_NOT_SUPPORTED_MSG);
        }

        this.evict(Collections.singletonList(identifier));
    }

    public void evict(List<Object> identifiers) {
        if (identifiers == null) {
            throw new NullPointerException(NULL_KEYS_NOT_SUPPORTED_MSG);
        }
        List<String> finalKeys = identifiers.stream().filter(Objects::nonNull).map(this::generateKey).collect(Collectors.toList());

        redis.del(finalKeys);
    }

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
