package com.moqifei.rpc.net.transport;

import com.moqifei.rpc.net.ChannelHandler;
import com.moqifei.rpc.net.Client;
import com.moqifei.rpc.net.Server;
import com.moqifei.rpc.net.exception.RemotingException;
import com.moqifei.rpc.net.url.PigeonURL;

import java.net.URL;

public interface NetWorkTransporter {
    //绑定获取网络传输服务端
    Server bind(PigeonURL var1, ChannelHandler handler, String serializer) throws RemotingException;

    //连接获取网络传输客户端
    Client connect(PigeonURL var1, ChannelHandler handler,  String serializer) throws RemotingException;
}
