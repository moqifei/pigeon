package com.moqifei.rpc.core.export;

import com.moqifei.rpc.exchange.ExchangeClient;
import com.moqifei.rpc.exchange.handler.ExchangeClientHandler;
import com.moqifei.rpc.exchange.handler.ExchangeServerHandler;
import com.moqifei.rpc.exchange.provider.PigeonRpcProvider;
import com.moqifei.rpc.exchange.support.PigeonExchanger;
import com.moqifei.rpc.net.Server;
import com.moqifei.rpc.net.enums.NetEnum;
import com.moqifei.rpc.net.exception.RemotingException;
import com.moqifei.rpc.net.params.PigeonRpcRequest;
import com.moqifei.rpc.net.params.PigeonRpcResponse;
import com.moqifei.rpc.net.url.PigeonURL;
import com.moqifei.rpc.registry.ServiceRegistry;
import com.moqifei.rpc.registry.enums.RegistryTypeEnum;
import com.moqifei.rpc.registry.impl.ZkServiceRegistry;
import com.moqifei.rpc.serialize.enums.SerializerEnum;
import com.moqifei.rpc.spi.load.ExtensionLoaderFactory;

import java.util.HashMap;
import java.util.Map;

public class PigeonRpcExport {

    private final String ip;
    private final int port;

    private final NetEnum net;
    private final SerializerEnum serializer;
    private final RegistryTypeEnum registryType;

    private PigeonRpcProvider pigeonRpcProvider;
    private ServiceRegistry serviceRegistry;
    private String serviceAddress;


    public PigeonRpcExport(String ip, int port, NetEnum net, SerializerEnum serializer, RegistryTypeEnum registryType) {
        this.ip = ip;
        this.port = port;
        this.net = net;
        this.serializer = serializer;
        this.registryType = registryType;
        init();
    }

    private void init() {
        this.pigeonRpcProvider = PigeonRpcProvider.getInstance();
        this.serviceRegistry = ExtensionLoaderFactory.load(ServiceRegistry.class, this.registryType.getTypeCode());
        Map<String, String> param = new HashMap<>();
        param.put(ZkServiceRegistry.ZK_ADDRESS, "ip:8090");
        param.put(ZkServiceRegistry.ZK_DIGEST, "");
        param.put(ZkServiceRegistry.ENV, "test");

        serviceRegistry.init(param);
    }

    public void start() throws RemotingException {
        //服务注册
        serviceAddress = ip.concat(":").concat(String.valueOf(port));
        serviceRegistry.registry(pigeonRpcProvider.getServiceData().keySet(), serviceAddress);

        //服务启动
        PigeonExchanger exchanger = new PigeonExchanger(net.getTypeCode(), serializer.getTypeCode());
        PigeonURL url = new PigeonURL("rpc", ip, port, null);
        ExchangeServerHandler handler = new ExchangeServerHandler(pigeonRpcProvider);
        exchanger.bind(url, handler);

    }

    public void addService(String iface, String version, Object serviceBean){
       pigeonRpcProvider.addService(iface, version, serviceBean);
    }
}
