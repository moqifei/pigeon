package com.moqifei.rpc.serialize.exception;

public class SerializeException extends RuntimeException{
    public SerializeException(String msg) {
        super(msg);
    }

    public SerializeException(String msg, Throwable cause) {
        super(msg, cause);
    }

    public SerializeException(Throwable cause) {
        super(cause);
    }
}
