package com.moqifei.rpc.net;

import com.moqifei.rpc.net.exception.RemotingException;

import java.net.URL;

/**
 * 通道接口，通道是通讯的载体。channel可以读写，channel是client和server的传输桥梁。
 * channel和client是一一对应的，也就是一个client对应一个channel，但是channel和server是多对一对关系，也就是一个server可以对应多个channel。
 */
public interface Channel {
    public void send(Object message, boolean sent) throws RemotingException;
}
