package com.moqifei.rpc.net.exception;

import java.net.InetSocketAddress;
import java.net.SocketAddress;

public class RemotingException extends Exception {
    private static final long serialVersionUID = -3160452149606778709L;
    private InetSocketAddress localAddress;
    private InetSocketAddress remoteAddress;



    public RemotingException(InetSocketAddress localAddress, InetSocketAddress remoteAddress, String message) {
        super(message);
        this.localAddress = localAddress;
        this.remoteAddress = remoteAddress;
    }


    public RemotingException(InetSocketAddress localAddress, InetSocketAddress remoteAddress, Throwable cause) {
        super(cause);
        this.localAddress = localAddress;
        this.remoteAddress = remoteAddress;
    }


    public RemotingException(InetSocketAddress localAddress, InetSocketAddress remoteAddress, String message, Throwable cause) {
        super(message, cause);
        this.localAddress = localAddress;
        this.remoteAddress = remoteAddress;
    }

    public RemotingException(SocketAddress remoteAddress, String message, Throwable cause) {
        super(message, cause);
    }

    public InetSocketAddress getLocalAddress() {
        return this.localAddress;
    }

    public InetSocketAddress getRemoteAddress() {
        return this.remoteAddress;
    }
}
