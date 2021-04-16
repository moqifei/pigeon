package com.moqifei.rpc.net;

import com.moqifei.rpc.net.exception.RemotingException;

public interface Server {
    public void init() throws RemotingException;
    public void bound() throws InterruptedException;
    public void close();
}
