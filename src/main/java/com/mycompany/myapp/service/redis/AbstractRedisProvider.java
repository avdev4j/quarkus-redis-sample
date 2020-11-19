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
import java.util.function.Function;
import java.util.stream.Collectors;
import javax.inject.Inject;
import javax.inject.Singleton;

@Singleton
public abstract class AbstractRedisProvider<T> {
    @Inject
    RedisClient redis;

    @Inject
    ReactiveRedisClient reactiveRedis;

    @Inject
    ObjectMapper objectMapper;

    public static final String NULL_KEYS_NOT_SUPPORTED_MSG = "Null keys are not supported";
    private final Class<T> type;

    public AbstractRedisProvider() {
        this.type = (Class<T>) ((ParameterizedType) getClass().getGenericSuperclass()).getActualTypeArguments()[0];
    }

    public Class<T> getMyType() {
        return this.type;
    }

    public Optional<T> get(Object key, Function<String, T> valueLoader) {
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
            result = valueLoader.apply(keyAsString);
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

    public Uni<Void> delete(Object key) {
        if (key == null) {
            throw new NullPointerException(NULL_KEYS_NOT_SUPPORTED_MSG);
        }

        return this.deleteAll(Collections.singletonList(key));
    }

    public Uni<Void> deleteAll(List<Object> keys) {
        List<String> keysAsString = keys.stream().map(Object::toString).collect(Collectors.toList());

        return reactiveRedis.del(keysAsString).map(response -> null);
    }

    protected T deserialize(Response response) throws JsonProcessingException {
        if (response == null) {
            return null;
        }

        return objectMapper.readValue(response.toString(), getMyType());
    }

    protected String serialize(T value) throws JsonProcessingException {
        if (value == null) {
            return null;
        }

        return objectMapper.writeValueAsString(value);
    }
}
