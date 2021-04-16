package com.moqifei.rpc.exchange.handler;

import com.moqifei.rpc.exchange.provider.PigeonRpcProvider;
import com.moqifei.rpc.net.Channel;
import com.moqifei.rpc.net.exception.RemotingException;
import com.moqifei.rpc.net.params.PigeonRpcRequest;
import com.moqifei.rpc.net.params.PigeonRpcResponse;

public class ExchangeServerHandler extends ChannelHandlerAdapter{
    private PigeonRpcProvider pigeonRpcProvider;

    public ExchangeServerHandler(PigeonRpcProvider pigeonRpcProvider) {
        this.pigeonRpcProvider = pigeonRpcProvider;
    }


    @Override
    public void received(Channel channel, Object var2) throws RemotingException {
        PigeonRpcRequest rpcRequest = (PigeonRpcRequest) var2;

        System.out.println("Server receive rpcRequest" +  rpcRequest);

        PigeonRpcResponse response = pigeonRpcProvider.call(rpcRequest);

        channel.send(response, true);
        System.out.println("Server reply back" +  response);
    }
}
