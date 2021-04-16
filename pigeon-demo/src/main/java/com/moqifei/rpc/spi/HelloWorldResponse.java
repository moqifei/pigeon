package com.moqifei.rpc.spi;

import java.io.Serializable;

public class HelloWorldResponse implements Serializable {
    public String result;

    public String getResult() {
        return result;
    }

    public void setResult(String result) {
        this.result = result;
    }

    @Override
    public String toString() {
        return "HelloWorldResponse{" +
                "result='" + result + '\'' +
                '}';
    }
}
