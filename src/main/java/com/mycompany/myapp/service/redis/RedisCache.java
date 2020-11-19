package com.mycompany.myapp.service.redis;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.quarkus.redis.client.RedisClient;
import io.quarkus.redis.client.reactive.ReactiveRedisClient;
import io.smallrye.mutiny.Uni;
import io.vertx.redis.client.Response;
import java.lang.reflect.ParameterizedType;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import javax.inject.Inject;

public abstract class RedisCache<T> {
    @Inject
    RedisClient redis;

    @Inject
    ReactiveRedisClient reactiveRedis;

    @Inject
    ObjectMapper objectMapper;

    public static final String NULL_KEYS_NOT_SUPPORTED_MSG = "Null keys are not supported";
    final Class<T> type;

    public RedisCache() {
        this.type = (Class<T>) ((ParameterizedType) getClass().getGenericSuperclass()).getActualTypeArguments()[0];
    }

    public Optional<T> get(Object key, Supplier<T> valueLoader) {
        if (key == null) {
            throw new NullPointerException(NULL_KEYS_NOT_SUPPORTED_MSG);
        }
        T result;
        String keyAsString = key.toString();

        try {
            result = deserialize(redis.get(keyAsString));
        } catch (JsonProcessingException e) {
            throw new CacheErrorException(e);
        }

        if (result == null) {
            result = valueLoader.get();
            this.set(keyAsString, result);
        }

        return Optional.ofNullable(result);
    }

    public void set(String key, T value) {
        if (value == null) {
            return;
        }

        try {
            redis.set(Arrays.asList(key, serialize(value)));
        } catch (JsonProcessingException e) {
            throw new CacheErrorException(e);
        }
    }

    public void delete(Object key) {
        if (key == null) {
            throw new NullPointerException(NULL_KEYS_NOT_SUPPORTED_MSG);
        }

        this.deleteAll(Collections.singletonList(key));
    }

    public void deleteAll(List<Object> keys) {
        List<String> keysAsString = keys.stream().map(Object::toString).collect(Collectors.toList());

        redis.del(keysAsString);
    }

    public List<String> keys() {
        return keys("*");
    }

    public List<String> keys(String filter) {
        return redis.keys(filter + "*").stream().map(Object::toString).collect(Collectors.toList());
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
