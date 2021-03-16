package com.moqifei.rpc.client;

import com.moqifei.rpc.registry.ServiceRegistry;
import com.moqifei.rpc.registry.impl.ZkServiceRegistry;
import com.moqifei.rpc.single.SimpleRpcClient;
import com.moqifei.rpc.spi.load.ExtensionLoaderFactory;

import java.util.HashMap;
import java.util.Map;
import java.util.TreeSet;

public class HelloWorldClient {
    public static void main(String[] args) throws Exception {
        SimpleRpcClient rpcClient = new SimpleRpcClient();

        //通过SPI获取服务发现实例，zkRegistry
        ServiceRegistry serviceRegistry = ExtensionLoaderFactory.load(ServiceRegistry.class, "zkRegistry");

        Map<String, String> param = new HashMap<>();
        param.put(ZkServiceRegistry.ZK_ADDRESS, "ip:8090");
        param.put(ZkServiceRegistry.ZK_DIGEST, "");
        param.put(ZkServiceRegistry.ENV, "test");
        //初始化服务链接，目前先写死 TODO 从配置文件读取
        serviceRegistry.init(param);

        TreeSet<String> addressSet = serviceRegistry.discovery(HelloWorldService.class.getSimpleName());
        String findAddress = addressSet.first();

        String[] address = findAddress.split(":");
        HelloWorldService helloWorldService = rpcClient.call(HelloWorldService.class, address[0], Integer.valueOf(address[1]));
        String result = helloWorldService.sayHello("Hello World!");
        System.out.println(result);
    }
}
