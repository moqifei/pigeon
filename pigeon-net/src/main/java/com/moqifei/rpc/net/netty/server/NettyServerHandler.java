package com.moqifei.rpc.net.netty.server;

import com.moqifei.rpc.net.Channel;
import com.moqifei.rpc.net.ChannelHandler;
import com.moqifei.rpc.net.exception.RemotingException;
import com.moqifei.rpc.net.netty.NettyChannel;
import com.moqifei.rpc.net.params.PigeonRpcRequest;
import com.moqifei.rpc.net.params.PigeonRpcResponse;
import com.moqifei.rpc.net.url.PigeonURL;
import io.netty.channel.ChannelDuplexHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelPromise;
import io.netty.channel.ChannelHandler.Sharable;

import java.net.InetSocketAddress;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Sharable
public class NettyServerHandler extends ChannelDuplexHandler {
    private final Map<String, Channel> channels = new ConcurrentHashMap();
    private final PigeonURL url;
    private final ChannelHandler handler;

    public NettyServerHandler(PigeonURL url, ChannelHandler handler) {
        if (url == null) {
            throw new IllegalArgumentException("url == null");
        } else if (handler == null) {
            throw new IllegalArgumentException("handler == null");
        } else {
            this.url = url;
            this.handler = handler;
        }
    }

    public Map<String, Channel> getChannels() {
        return this.channels;
    }

    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception {
        System.out.println("server active");
        ctx.fireChannelActive();
        NettyChannel channel = NettyChannel.getOrAddChannel(ctx.channel(), this.url, this.handler);

        try {
            if (channel != null) {
                this.channels.put(toAddressString((InetSocketAddress) ctx.channel().remoteAddress()), channel);
            }

            this.handler.connected(channel);
        } finally {
            NettyChannel.removeChannelIfDisconnected(ctx.channel());
        }

    }

    private String toAddressString(InetSocketAddress remoteAddress) {
        return remoteAddress.getAddress().getHostAddress() + ":" + remoteAddress.getPort();
    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) throws Exception {
        NettyChannel channel = NettyChannel.getOrAddChannel(ctx.channel(), this.url, this.handler);

        try {
            this.channels.remove(toAddressString((InetSocketAddress) ctx.channel().remoteAddress()));
            this.handler.disconnected(channel);
        } finally {
            NettyChannel.removeChannelIfDisconnected(ctx.channel());
        }

    }


    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        NettyChannel channel = NettyChannel.getOrAddChannel(ctx.channel(), this.url, this.handler);

        try {
            this.handler.received(channel, msg);
        } finally {
            NettyChannel.removeChannelIfDisconnected(ctx.channel());
        }

    }



    @Override
    public void write(ChannelHandlerContext ctx, Object msg, ChannelPromise promise) throws Exception {
        super.write(ctx, msg, promise);
        NettyChannel channel = NettyChannel.getOrAddChannel(ctx.channel(), this.url, this.handler);

        try {
            this.handler.sent(channel, msg);
        } finally {
            NettyChannel.removeChannelIfDisconnected(ctx.channel());
        }

    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        NettyChannel channel = NettyChannel.getOrAddChannel(ctx.channel(), this.url, this.handler);

        try {
            this.handler.caught(channel, cause);
        } finally {
            NettyChannel.removeChannelIfDisconnected(ctx.channel());
        }

    }
}