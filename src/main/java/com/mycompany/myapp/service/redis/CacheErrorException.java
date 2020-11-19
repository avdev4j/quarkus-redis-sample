package com.mycompany.myapp.service.redis;

public class CacheErrorException extends RuntimeException {

    public CacheErrorException(Throwable cause) {
        super(cause);
    }
}
