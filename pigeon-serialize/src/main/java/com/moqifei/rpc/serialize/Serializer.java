package com.moqifei.rpc.serialize;

public interface Serializer {
    public <T> byte[] serialize(T obj);
    public <T> Object deserialize(byte[] bytes, Class<T> clazz);
}
