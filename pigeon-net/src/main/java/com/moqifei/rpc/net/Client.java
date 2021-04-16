package com.moqifei.rpc.net;

import com.moqifei.rpc.net.exception.RemotingException;

import java.net.URL;

public interface Client {
    public void init() throws RemotingException;
    public void connect() throws InterruptedException;
    public void sent(Object msg, boolean sent) throws RemotingException;
    public boolean isValidate();
    public void close();
}
