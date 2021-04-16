package com.moqifei.rpc.spi.main;

import com.moqifei.rpc.core.export.PigeonRpcExport;
import com.moqifei.rpc.exchange.handler.ExchangeServerHandler;
import com.moqifei.rpc.exchange.provider.PigeonRpcProvider;
import com.moqifei.rpc.net.ChannelHandler;
import com.moqifei.rpc.net.enums.NetEnum;
import com.moqifei.rpc.net.exception.RemotingException;
import com.moqifei.rpc.net.netty.server.NettyServer;
import com.moqifei.rpc.net.transport.NetWorkTransporter;
import com.moqifei.rpc.net.url.PigeonURL;
import com.moqifei.rpc.registry.enums.RegistryTypeEnum;
import com.moqifei.rpc.serialize.enums.SerializerEnum;
import com.moqifei.rpc.spi.HelloWorld;
import com.moqifei.rpc.spi.impl.ChineseHelloWorld;
import com.moqifei.rpc.spi.load.ExtensionLoaderFactory;

import java.net.MalformedURLException;
import java.util.concurrent.TimeUnit;

public class NettyRpcServer {
    public static void main(String[] args) throws RemotingException, MalformedURLException, InterruptedException {

        PigeonRpcExport exporter = new PigeonRpcExport("127.0.0.1",8091, NetEnum.NETTY, SerializerEnum.HESSIAN2, RegistryTypeEnum.ZK);
        exporter.addService(HelloWorld.class.getName(),null, new ChineseHelloWorld());

        exporter.start();


        while (!Thread.currentThread().isInterrupted()) {
            TimeUnit.HOURS.sleep(1);
        }

    }
}
