package com.mycompany.myapp.cache.redis;

import java.io.Serializable;
import java.util.Objects;

public class Foo implements Serializable {

    public String name;
    public Integer age;

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Foo foo = (Foo) o;
        return Objects.equals(name, foo.name) &&
                Objects.equals(age, foo.age);
    }

    @Override
    public int hashCode() {
        return Objects.hash(name, age);
    }

    @Override
    public String toString() {
        return "Foo{" +
            "name='" + name + '\'' +
            ", age=" + age +
            '}';
    }
}
