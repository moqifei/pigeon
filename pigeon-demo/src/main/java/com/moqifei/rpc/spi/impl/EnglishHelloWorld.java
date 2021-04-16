package com.moqifei.rpc.spi.impl;

import com.moqifei.rpc.spi.HelloWorld;
import com.moqifei.rpc.spi.HelloWorldResponse;

public class EnglishHelloWorld implements HelloWorld {
    @Override
    public HelloWorldResponse sayHello() {
        HelloWorldResponse response = new HelloWorldResponse();
        response.setResult("你好");
        return response;
    }
}
