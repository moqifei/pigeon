package com.moqifei.rpc.core.exception;

public class PigeonRpcException extends RuntimeException {
    private static final long serialVersionUID = 42L;

    public PigeonRpcException(String msg) {
        super(msg);
    }

    public PigeonRpcException(String msg, Throwable cause) {
        super(msg, cause);
    }

    public PigeonRpcException(Throwable cause) {
        super(cause);
    }

}