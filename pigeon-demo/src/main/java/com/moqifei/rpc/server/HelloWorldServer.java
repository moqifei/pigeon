package com.moqifei.rpc.server;

import com.moqifei.rpc.registry.ServiceRegistry;
import com.moqifei.rpc.registry.impl.ZkServiceRegistry;
import com.moqifei.rpc.single.SimpleRpcServer;
import com.moqifei.rpc.spi.load.ExtensionLoaderFactory;

import java.io.IOException;
import java.util.*;
import java.util.concurrent.TimeUnit;

public class HelloWorldServer {
    public static void main(String[] args) throws IOException {
        HelloWorldService helloWorldService = new HelloWorldServiceImpl();
        SimpleRpcServer rpcServer = new SimpleRpcServer();

        //通过SPI获取服务发现实例，zkRegistry
        ServiceRegistry serviceRegistry = ExtensionLoaderFactory.load(ServiceRegistry.class, "nacosRegistry");

        Map<String, String> param = new HashMap<>();
//        param.put(ZkServiceRegistry.ZK_ADDRESS, "ip:8090");
//        param.put(ZkServiceRegistry.ZK_DIGEST, "");
//        param.put(ZkServiceRegistry.ENV, "test");
        param.put("ip", "127.0.0.1");
        param.put("port", "8848");
        //初始化服务链接，目前先写死 TODO 从配置文件读取
        serviceRegistry.init(param);

        serviceRegistry.registry(new HashSet<String>(Arrays.asList(HelloWorldService.class.getSimpleName())), "127.0.0.1:8080");

        rpcServer.reply(helloWorldService, 8080);
    }
}
