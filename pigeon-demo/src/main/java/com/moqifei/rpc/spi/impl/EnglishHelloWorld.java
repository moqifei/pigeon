package com.moqifei.rpc.spi.impl;

import com.moqifei.rpc.spi.HelloWorld;

public class EnglishHelloWorld implements HelloWorld {
    @Override
    public void sayHello() {
        System.out.println("Hello!");
    }
}
