package com.moqifei.rpc.exchange.support;

import com.moqifei.rpc.exchange.ExchangeClient;
import com.moqifei.rpc.exchange.ExchangeServer;
import com.moqifei.rpc.exchange.Exchanger;
import com.moqifei.rpc.net.ChannelHandler;
import com.moqifei.rpc.net.exception.RemotingException;
import com.moqifei.rpc.net.transport.NetWorkTransporter;
import com.moqifei.rpc.net.url.PigeonURL;
import com.moqifei.rpc.spi.load.ExtensionLoaderFactory;

public class PigeonExchanger implements Exchanger {

    private final String netType;
    private final String serializerType;
    private NetWorkTransporter netWorkTransporter;

    public PigeonExchanger(String netType, String serializerType) {
        this.netType = netType;
        this.serializerType = serializerType;
        netWorkTransporter = ExtensionLoaderFactory.load(NetWorkTransporter.class, netType);
    }


    @Override
    public ExchangeServer bind(PigeonURL url, ChannelHandler var2) throws RemotingException {
        return new ExchangeServer(netWorkTransporter.bind(url, var2, serializerType));
    }

    @Override
    public ExchangeClient connect(PigeonURL url, ChannelHandler var2) throws RemotingException {
        return new ExchangeClient(netWorkTransporter.connect(url, var2, serializerType));
    }
}
