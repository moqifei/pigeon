package com.moqifei.rpc.spi.main;

import com.moqifei.rpc.core.reference.PigeonRpcReference;
import com.moqifei.rpc.exchange.ExchangeClient;
import com.moqifei.rpc.exchange.enums.CallTypeEnum;
import com.moqifei.rpc.exchange.handler.ExchangeClientHandler;
import com.moqifei.rpc.exchange.invoker.PigeonRpcInvoker;
import com.moqifei.rpc.exchange.params.PigeonRpcFutureReponse;
import com.moqifei.rpc.exchange.params.PigeonRpcInvokeFutureContext;
import com.moqifei.rpc.exchange.support.PigeonExchanger;
import com.moqifei.rpc.net.enums.NetEnum;
import com.moqifei.rpc.net.exception.RemotingException;
import com.moqifei.rpc.net.params.PigeonRpcRequest;
import com.moqifei.rpc.net.params.PigeonRpcResponse;
import com.moqifei.rpc.net.url.PigeonURL;
import com.moqifei.rpc.registry.enums.RegistryTypeEnum;
import com.moqifei.rpc.route.enums.RouteTypeEnum;
import com.moqifei.rpc.serialize.enums.SerializerEnum;
import com.moqifei.rpc.spi.HelloWorld;
import com.moqifei.rpc.spi.HelloWorldResponse;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

public class NettyRpcClient {
    public static void main(String[] args) throws RemotingException, InterruptedException, ExecutionException, TimeoutException {

//        PigeonExchanger exchanger = new PigeonExchanger("netty","hessian2");
//        PigeonURL url = new PigeonURL("rpc", "127.0.0.1", 8091, null);
//        ExchangeClientHandler handler = new ExchangeClientHandler();
//        ExchangeClient client = exchanger.connect(url, handler);
//        PigeonRpcInvoker invoker = PigeonRpcInvoker.getPigeonRpcInvoker();
//        PigeonRpcRequest request = new PigeonRpcRequest();
//        request.setClassName(HelloWorld.class.getName());
//        request.setRequestId("123");
//        request.setMethodName("sayHello");
//
//        PigeonRpcFutureReponse futureResponse = new PigeonRpcFutureReponse(3000, invoker, request);
//        PigeonRpcInvokeFutureContext context = new PigeonRpcInvokeFutureContext(futureResponse);
//        PigeonRpcInvokeFutureContext.setContext(context);
//
//        client.request(request);
//
//        Future<PigeonRpcResponse> responseFuture = PigeonRpcInvokeFutureContext.getContext(PigeonRpcResponse.class);
//
//        System.out.println("client get response: "+responseFuture.get().toString());


        //futureResponse.removeInvokeFuture();

        PigeonRpcReference referencer = new PigeonRpcReference(HelloWorld.class, null, 3000, "127.0.0.1:8091", NetEnum.NETTY
        , SerializerEnum.HESSIAN2, RegistryTypeEnum.ZK, RouteTypeEnum.DEFAULT, CallTypeEnum.SYNC);
        HelloWorld helloWorld = (HelloWorld) referencer.getReference();
        HelloWorldResponse response = helloWorld.sayHello();
        System.out.println("client get response: "+response);

    }
}
