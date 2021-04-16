package com.moqifei.rpc.net.netty.client;

import com.moqifei.rpc.net.ChannelHandler;
import com.moqifei.rpc.net.Client;
import com.moqifei.rpc.net.exception.RemotingException;
import com.moqifei.rpc.net.netty.NettyChannel;
import com.moqifei.rpc.net.netty.codec.NettyDecoder;
import com.moqifei.rpc.net.netty.codec.NettyEncoder;
import com.moqifei.rpc.net.params.PigeonRpcRequest;
import com.moqifei.rpc.net.params.PigeonRpcResponse;
import com.moqifei.rpc.net.url.PigeonURL;
import com.moqifei.rpc.serialize.Serializer;
import com.moqifei.rpc.spi.load.ExtensionLoaderFactory;
import io.netty.bootstrap.Bootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;


public class NettyClient implements Client{
    private final ChannelHandler handler;
    private volatile PigeonURL url;
    private final String serializer;

    private volatile Channel channel;
    private EventLoopGroup group;
    private Bootstrap bootstrap;

    public NettyClient(PigeonURL url, ChannelHandler handler,String serializer) throws InterruptedException {
        this.handler = handler;
        this.url = url;
        this.serializer = serializer;
        init();
        connect();
    }

    public PigeonURL getUrl() {
        return this.url;
    }


    @Override
    public void init() {
        //定义NettyClientHandler, 最终委托给ChannelHandler处理读写
        final NettyClientHandler nettyClientHandler = new NettyClientHandler(this.getUrl(), this.handler);
        Serializer serializer =  ExtensionLoaderFactory.load(Serializer.class, this.serializer);

        this.group = new NioEventLoopGroup();
        this.bootstrap = new Bootstrap();
        bootstrap.group(group)
                .channel(NioSocketChannel.class)
                .handler(new ChannelInitializer<SocketChannel>() {
                    @Override
                    public void initChannel(SocketChannel channel) throws Exception {
                        channel.pipeline()
                                .addLast(new NettyEncoder(PigeonRpcRequest.class, serializer))
                                .addLast(new NettyDecoder(PigeonRpcResponse.class, serializer))
                                .addLast("handler", nettyClientHandler);
                    }
                })
                .option(ChannelOption.TCP_NODELAY, true)
                .option(ChannelOption.SO_KEEPALIVE, true)
                .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, 10000);
    }

    @Override
    public void connect() throws InterruptedException {
        this.channel = bootstrap.connect(this.getUrl().getHost(), this.getUrl().getPort()).sync().channel();
        //添加到NettyChannel
        NettyChannel.getOrAddChannel(this.channel, this.getUrl(), this.handler);
    }

    @Override
    public boolean isValidate() {
        if (this.channel != null) {
            return this.channel.isActive();
        }
        return false;
    }

    @Override
    public void close() {
        if (this.channel != null && this.channel.isActive()) {
            this.channel.close();        // if this.channel.isOpen()
        }
        if (this.group != null && !this.group.isShutdown()) {
            this.group.shutdownGracefully();
        }
    }

    @Override
    public void sent(Object message, boolean sent) throws RemotingException {
        asyncSend(message, true);
    }

    private void asyncSend(Object message, boolean sent) throws RemotingException {
        NettyChannel nettyChannel =  NettyChannel.getOrAddChannel(this.channel, this.getUrl(), this.handler);
        nettyChannel.send(message, true);
    }
}
