package com.moqifei.rpc.net.netty.server;

import com.moqifei.rpc.net.ChannelHandler;
import com.moqifei.rpc.net.Server;
import com.moqifei.rpc.net.exception.RemotingException;
import com.moqifei.rpc.net.netty.NettyChannel;
import com.moqifei.rpc.net.netty.codec.NettyDecoder;
import com.moqifei.rpc.net.netty.codec.NettyEncoder;
import com.moqifei.rpc.net.params.PigeonRpcRequest;
import com.moqifei.rpc.net.params.PigeonRpcResponse;
import com.moqifei.rpc.net.url.PigeonURL;
import com.moqifei.rpc.serialize.Serializer;
import com.moqifei.rpc.spi.load.ExtensionLoaderFactory;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.util.concurrent.DefaultThreadFactory;

import java.net.InetSocketAddress;
import java.net.URL;

public class NettyServer implements Server {
    private final ChannelHandler handler;
    private volatile PigeonURL url;
    private final String serializer;

    private Thread thread;

    private volatile Channel channel;
    private ServerBootstrap bootstrap;
    private EventLoopGroup bossGroup;
    private EventLoopGroup workerGroup;

    public NettyServer(PigeonURL url, ChannelHandler handler, String serializer) throws RemotingException {
        this.handler = handler;
        this.url = url;
        this.serializer = serializer;
        thread = new Thread(()->{
            init();
            try {
                bound();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }finally {
                bossGroup.shutdownGracefully();
                workerGroup.shutdownGracefully();
            }
        });

        thread.setDaemon(true);
        thread.start();
    }


    @Override
    public void init() {
        this.bootstrap = new ServerBootstrap();
        this.bossGroup = new NioEventLoopGroup(1, new DefaultThreadFactory("NettyServerBoss", true));
        this.workerGroup = new NioEventLoopGroup();
        final NettyServerHandler nettyServerHandler = new NettyServerHandler(this.getUrl(), this.handler);

        Serializer serializer =  ExtensionLoaderFactory.load(Serializer.class, this.serializer);


        bootstrap.group(bossGroup, workerGroup)
                .channel(NioServerSocketChannel.class)
                .childHandler(new ChannelInitializer<SocketChannel>() {
                    @Override
                    public void initChannel(SocketChannel channel) throws Exception {
                        channel.pipeline()
                                .addLast(new NettyDecoder(PigeonRpcRequest.class, serializer))
                                .addLast(new NettyEncoder(PigeonRpcResponse.class, serializer))
                                .addLast(nettyServerHandler);
                    }
                })
                .childOption(ChannelOption.TCP_NODELAY, true)
                .childOption(ChannelOption.SO_KEEPALIVE, true);
        System.out.println("server init");

    }

    private PigeonURL getUrl() {
        return this.url;
    }


    @Override
    public void bound() throws InterruptedException {
        //String bindIp = this.getUrl().getHost();
        int bindPort = this.getUrl().getPort();

       // InetSocketAddress bindAddress = new InetSocketAddress(bindIp, bindPort);
        ChannelFuture channelFuture = this.bootstrap.bind(bindPort).sync();
        this.channel = channelFuture.channel();
        System.out.println("server bind");
        //添加到NettyChannel
        NettyChannel.getOrAddChannel(this.channel, this.getUrl(), this.handler);
        // wait util stop
        channelFuture.channel().closeFuture().sync();
    }

    @Override
    public void close() {
        // destroy server thread
        if (thread != null && thread.isAlive()) {
            thread.interrupt();
        }
    }
}
