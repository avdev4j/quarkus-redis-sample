package com.mycompany.myapp.service.redis;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.quarkus.redis.client.RedisClient;
import io.vertx.redis.client.Response;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.ParameterizedType;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import javax.inject.Inject;

public abstract class AbstractRedisProvider<T> {
    private final Logger log = LoggerFactory.getLogger(AbstractRedisProvider.class);

    @Inject
  RedisClient redisClient;

  @Inject
  ObjectMapper objectMapper;

  private final Class<T> type;

  public AbstractRedisProvider() {
    this.type = (Class<T>) ((ParameterizedType) getClass().getGenericSuperclass()).getActualTypeArguments()[0];
  }

  public Class<T> getMyType() {
    return this.type;
  }

  public Optional<T> retrieve(String key) {
    T result = null;

    try {
      Response response = redisClient.get(key);
      if (response != null) {
        result = objectMapper.readValue(response.toString(), getMyType());
      }
    } catch (JsonProcessingException e) {
        log.error("error");
    }

    return Optional.ofNullable(result);
  }

  public void set(String key, T value) {
    try {
      redisClient.set(Arrays.asList(key, objectMapper.writeValueAsString(value)));
    } catch (JsonProcessingException e) {
      e.printStackTrace();
    }
  }

    public String delete(String key) {
        return this.deleteAll(Collections.singletonList(key));
    }

    public String deleteAll(List<String> keys) {
        return redisClient.del(keys).toString();
    }

    public boolean exists(String key) {
        return redisClient.exists(Collections.singletonList(key)).toBoolean();
    }

    public List<String> retrieveKeys() {
        return redisClient.keys("*").stream().map(Response::toString).collect(Collectors.toList());
    }

}
