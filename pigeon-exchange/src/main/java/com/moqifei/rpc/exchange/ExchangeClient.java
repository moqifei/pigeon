package com.moqifei.rpc.exchange;

import com.moqifei.rpc.exchange.invoker.PigeonRpcInvoker;
import com.moqifei.rpc.net.Client;
import com.moqifei.rpc.net.exception.RemotingException;
import com.moqifei.rpc.net.params.PigeonRpcRequest;

public class ExchangeClient {
    private final Client client;
    private PigeonRpcInvoker invoker;
    public ExchangeClient(Client client) {
        this.client = client;
        invoker = PigeonRpcInvoker.getPigeonRpcInvoker();
    }

    public void request(PigeonRpcRequest rpcRequest) throws RemotingException {
       client.sent(rpcRequest, true);
    }
}
