package com.moqifei.rpc.exchange.invoker;

import com.moqifei.rpc.exchange.params.PigeonRpcFutureReponse;
import com.moqifei.rpc.net.params.PigeonRpcResponse;

import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

public class PigeonRpcInvoker {
    private PigeonRpcInvoker(){}
    private static volatile PigeonRpcInvoker INSTANCE= new PigeonRpcInvoker();
    public static PigeonRpcInvoker getPigeonRpcInvoker(){
        return INSTANCE;
    }
    private ConcurrentHashMap<String, PigeonRpcFutureReponse> futureResponseMap = new ConcurrentHashMap<>();

    public void setInvokerFuture(String requestId, PigeonRpcFutureReponse futureResponse){
        futureResponseMap.put(requestId, futureResponse);
    }

    public void removeInvokerFuture(String requestId){
        futureResponseMap.remove(requestId);
    }

    public PigeonRpcFutureReponse getInvokerFuture(String requestId){
        return futureResponseMap.get(requestId);
    }

    public void notifyInvokerFuture(String requestId, PigeonRpcResponse rpcResponse){
        final PigeonRpcFutureReponse futureReponse = futureResponseMap.get(requestId);
        if(Objects.isNull(futureReponse)){
            return;
        }
        futureReponse.setRpcResponse(rpcResponse);
        futureResponseMap.remove(requestId);
    }

}
