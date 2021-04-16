package com.moqifei.rpc.net.transport;

import com.moqifei.rpc.net.ChannelHandler;
import com.moqifei.rpc.net.Client;
import com.moqifei.rpc.net.Server;
import com.moqifei.rpc.net.exception.RemotingException;
import com.moqifei.rpc.net.netty.client.NettyClient;
import com.moqifei.rpc.net.netty.server.NettyServer;
import com.moqifei.rpc.net.url.PigeonURL;
import com.moqifei.rpc.spi.PigeonSPI;
import lombok.SneakyThrows;

import java.net.URL;

@PigeonSPI("netty")
public class NettyTransporter implements  NetWorkTransporter{
    @Override
    public Server bind(PigeonURL url, ChannelHandler handler, String serializer) throws RemotingException {
        return new NettyServer(url, handler, serializer);
    }

    @SneakyThrows
    @Override
    public Client connect(PigeonURL url, ChannelHandler handler,  String serializer) throws RemotingException {
        return new NettyClient(url, handler, serializer);
    }
}
