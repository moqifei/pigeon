package com.moqifei.rpc.server;

public class HelloWorldServiceImpl implements  HelloWorldService{
    @Override
    public String sayHello(String words) {
        return words;
    }
}
