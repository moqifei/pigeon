package com.moqifei.rpc.spi.main;

import com.moqifei.rpc.registry.ServiceRegistry;
import com.moqifei.rpc.registry.impl.ZkServiceRegistry;
import com.moqifei.rpc.route.RouteStrategy;
import com.moqifei.rpc.server.HelloWorldService;
import com.moqifei.rpc.spi.HelloWorld;
import com.moqifei.rpc.spi.load.ExtensionLoaderFactory;

import java.io.UnsupportedEncodingException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.*;
import java.util.concurrent.TimeUnit;

public class HelloWorldMain {
    public static void main(String[] args) {
//        ServiceLoader<HelloWorld> helloWorlds = ServiceLoader.load(HelloWorld.class);
//        Iterator<HelloWorld> iterator = helloWorlds.iterator();
//        while(iterator.hasNext()){
//            HelloWorld helloWorld = iterator.next();
//            helloWorld.sayHello();
//        }
//
//        HelloWorld helloWorld = ExtensionLoaderFactory.load(HelloWorld.class, "chinese");
//        helloWorld.sayHello();

//        ServiceRegistry serviceRegistry = ExtensionLoaderFactory.load(ServiceRegistry.class, "zkRegistry");
//        Map<String, String> param = new HashMap<>();
//        param.put(ZkServiceRegistry.ZK_ADDRESS, "ip:8090");
//        param.put(ZkServiceRegistry.ZK_DIGEST, "");
//        param.put(ZkServiceRegistry.ENV, "test");
//
//        serviceRegistry.init(param);
//
//        String servicename = "demo_service";
//        serviceRegistry.registry(new HashSet<String>(Arrays.asList(HelloWorldService.class.getSimpleName())), "127.0.0.1:8888");
//        try {
//            TimeUnit.MILLISECONDS.sleep(10);
//        } catch (InterruptedException e) {
//            e.printStackTrace();
//        }
//        System.out.println(HelloWorldService.class.getSimpleName());
//        System.out.println(serviceRegistry.discovery(HelloWorldService.class.getSimpleName()));


//        ServiceRegistry serviceRegistry = ExtensionLoaderFactory.load(ServiceRegistry.class, "nacosRegistry");
//        Map<String, String> param = new HashMap<>();
//        param.put("ip", "127.0.0.1");
//        param.put("port", "8848");
//        serviceRegistry.init(param);
//        serviceRegistry.registry(new HashSet<String>(Arrays.asList(HelloWorldService.class.getSimpleName())), "127.0.0.1:8888");

        RouteStrategy routeStrategy = ExtensionLoaderFactory.load(RouteStrategy.class, "lfu");
        TreeSet<String> address = new TreeSet<>();
        address.add("127.0.0.1:8080");
        address.add("127.0.0.2:8090");
        address.add("127.0.0.3:8010");
        for(int i = 0; i< 10; i++){
            System.out.println(routeStrategy.route("test", address));
        }



    }
}
