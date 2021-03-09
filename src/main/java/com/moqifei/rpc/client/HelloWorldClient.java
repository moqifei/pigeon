package com.moqifei.rpc.client;

import com.moqifei.rpc.single.SimpleRpcClient;

public class HelloWorldClient {
    public static void main(String[] args) throws Exception {
        SimpleRpcClient rpcClient = new SimpleRpcClient();
        HelloWorldService helloWorldService = rpcClient.call(HelloWorldService.class, "127.0.0.1", 8080);
        String result = helloWorldService.sayHello("Hello World!");
        System.out.println(result);
    }
}
