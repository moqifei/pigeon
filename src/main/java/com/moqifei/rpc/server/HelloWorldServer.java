package com.moqifei.rpc.server;

import com.moqifei.rpc.single.SimpleRpcServer;

import java.io.IOException;

public class HelloWorldServer {
    public static void main(String[] args) throws IOException {
        HelloWorldService helloWorldService = new HelloWorldServiceImpl();
        SimpleRpcServer rpcServer = new SimpleRpcServer();
        rpcServer.reply(helloWorldService, 8080);
    }
}
