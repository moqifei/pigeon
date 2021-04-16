package com.moqifei.rpc.spi.impl;

import com.moqifei.rpc.spi.HelloWorld;
import com.moqifei.rpc.spi.HelloWorldResponse;
import com.moqifei.rpc.spi.PigeonSPI;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@PigeonSPI("chinese")
public class ChineseHelloWorld implements HelloWorld {
    @Override
    public HelloWorldResponse sayHello() {
        HelloWorldResponse response = new HelloWorldResponse();
        response.setResult("你好");
        return response;
    }
}
