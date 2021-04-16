package com.moqifei.rpc.net.netty;

import com.moqifei.rpc.net.ChannelHandler;
import com.moqifei.rpc.net.exception.RemotingException;
import com.moqifei.rpc.net.url.PigeonURL;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;

import java.net.URL;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * 记录netty Channel 与 pigeon Channel之间的映射关系
 */
public class NettyChannel implements com.moqifei.rpc.net.Channel {
    private static final ConcurrentMap<Channel, NettyChannel> channelMap = new ConcurrentHashMap();
    private final Channel channel;
    private volatile PigeonURL url;
    private final ChannelHandler channelHandler;

    public NettyChannel(Channel channel, PigeonURL url, ChannelHandler channelHandler) {
        this.channel = channel;
        this.channelHandler = channelHandler;
        this.url = url;
    }

    /**
     * 获取或添加Channel之间的映射关系
     * @param ch
     * @param url
     * @param handler
     * @return
     */
    public static NettyChannel getOrAddChannel(Channel ch, PigeonURL url, ChannelHandler handler) {
        if (ch == null) {
            return null;
        } else {
            NettyChannel ret = (NettyChannel)channelMap.get(ch);
            if (ret == null) {
                NettyChannel nettyChannel = new NettyChannel(ch, url, handler);
                if (ch.isActive()) {
                    ret = (NettyChannel)channelMap.putIfAbsent(ch, nettyChannel);
                }

                if (ret == null) {
                    ret = nettyChannel;
                }
            }

            return ret;
        }
    }

    @Override
    public void send(Object message, boolean sent) throws RemotingException {

        System.out.println("Channel send "+ message);

        try {
            ChannelFuture future = this.channel.writeAndFlush(message).sync();

            Throwable cause = future.cause();
            if (cause != null) {
                throw cause;
            }
        } catch (Throwable var7) {
            throw new RemotingException(this.channel.remoteAddress(), "Failed to send message " + message + " to remoteAddress, cause: " + var7.getMessage(), var7);
        }
    }

    public static void removeChannelIfDisconnected(Channel ch) {
        if (ch != null && !ch.isActive()) {
            channelMap.remove(ch);
        }

    }

}
