package com.moqifei.rpc.net.netty.client;

import com.moqifei.rpc.net.ChannelHandler;
import com.moqifei.rpc.net.netty.NettyChannel;
import com.moqifei.rpc.net.url.PigeonURL;
import io.netty.channel.ChannelDuplexHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelPromise;
import io.netty.channel.ChannelHandler.Sharable;

import java.net.URL;

@Sharable
public class NettyClientHandler extends ChannelDuplexHandler {

    private final PigeonURL url;
    private final ChannelHandler handler;

    public NettyClientHandler(PigeonURL url, ChannelHandler handler) {
        if (url == null) {
            throw new IllegalArgumentException("url == null");
        } else if (handler == null) {
            throw new IllegalArgumentException("handler == null");
        } else {
            this.url = url;
            this.handler = handler;
        }
    }

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {

        System.out.println("NettyClientHandler#channelRead " + msg);

        NettyChannel channel = NettyChannel.getOrAddChannel(ctx.channel(), this.url, this.handler);

        try {
            this.handler.received(channel, msg);
        } finally {
            NettyChannel.removeChannelIfDisconnected(ctx.channel());
        }

    }

    @Override
    public void channelReadComplete(ChannelHandlerContext ctx) throws Exception {
        ctx.fireChannelReadComplete();
        System.out.println("NettyClientHandler#channelReadComplete ");
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
}
