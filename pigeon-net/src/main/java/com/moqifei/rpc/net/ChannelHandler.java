package com.moqifei.rpc.net;

import com.moqifei.rpc.net.exception.RemotingException;

public interface ChannelHandler {
    void connected(Channel var1) throws RemotingException;

    void disconnected(Channel var1) throws RemotingException;

    void sent(Channel var1, Object var2) throws RemotingException;

    void received(Channel var1, Object var2) throws RemotingException;

    void caught(Channel var1, Throwable var2) throws RemotingException;
}
