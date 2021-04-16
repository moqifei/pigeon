package com.moqifei.rpc.exchange.handler;

import com.moqifei.rpc.exchange.invoker.PigeonRpcInvoker;
import com.moqifei.rpc.net.Channel;
import com.moqifei.rpc.net.exception.RemotingException;
import com.moqifei.rpc.net.params.PigeonRpcResponse;

public class ExchangeClientHandler extends ChannelHandlerAdapter{

    private PigeonRpcInvoker rpcInvoker;

    public ExchangeClientHandler(){
        this.rpcInvoker = PigeonRpcInvoker.getPigeonRpcInvoker();
    }

    @Override
    public void received(Channel var1, Object var2) throws RemotingException {
        System.out.println("ExchangeClientHandler#received "+var2);
        PigeonRpcResponse rpcResponse = (PigeonRpcResponse) var2;
        rpcInvoker.notifyInvokerFuture(rpcResponse.getRequestId(), rpcResponse);
    }
}
