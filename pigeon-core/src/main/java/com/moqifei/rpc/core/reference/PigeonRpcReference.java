package com.moqifei.rpc.core.reference;

import com.moqifei.rpc.core.exception.PigeonRpcException;
import com.moqifei.rpc.exchange.ExchangeClient;
import com.moqifei.rpc.exchange.enums.CallTypeEnum;
import com.moqifei.rpc.exchange.handler.ExchangeClientHandler;
import com.moqifei.rpc.exchange.invoker.PigeonRpcInvoker;
import com.moqifei.rpc.exchange.params.PigeonRpcFutureReponse;
import com.moqifei.rpc.exchange.params.PigeonRpcInvokeFutureContext;
import com.moqifei.rpc.exchange.support.PigeonExchanger;
import com.moqifei.rpc.net.enums.NetEnum;
import com.moqifei.rpc.net.params.PigeonRpcRequest;
import com.moqifei.rpc.net.params.PigeonRpcResponse;
import com.moqifei.rpc.net.url.PigeonURL;
import com.moqifei.rpc.registry.ServiceRegistry;
import com.moqifei.rpc.registry.enums.RegistryTypeEnum;
import com.moqifei.rpc.registry.impl.ZkServiceRegistry;
import com.moqifei.rpc.route.RouteStrategy;
import com.moqifei.rpc.route.enums.RouteTypeEnum;
import com.moqifei.rpc.serialize.enums.SerializerEnum;
import com.moqifei.rpc.spi.load.ExtensionLoaderFactory;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.*;
import java.util.concurrent.TimeUnit;

public class PigeonRpcReference {

    private final Class<?> iface;
    private final String version;
    private final long timeout;
    private final String address;

    private final NetEnum net;
    private final SerializerEnum serializer;
    private final RegistryTypeEnum registryType;
    private final RouteTypeEnum routeType;
    private final CallTypeEnum callType;

    private PigeonRpcInvoker pigeonRpcInvoker;
    private ServiceRegistry serviceRegistry;
    private RouteStrategy routeStrategy;


    public PigeonRpcReference(Class<?> iface, String version, long timeout, String address, NetEnum net, SerializerEnum serializer, RegistryTypeEnum registryType, RouteTypeEnum routeType, CallTypeEnum callType) {
        this.iface = iface;
        this.version = version;
        this.timeout = timeout;
        this.address = address;
        this.net = net;
        this.serializer = serializer;
        this.registryType = registryType;
        this.routeType = routeType;
        this.callType = callType;
        init();
    }

    private void init() {
        this.pigeonRpcInvoker = PigeonRpcInvoker.getPigeonRpcInvoker();
        this.serviceRegistry = ExtensionLoaderFactory.load(ServiceRegistry.class, this.registryType.getTypeCode());
        Map<String, String> param = new HashMap<>();
        param.put(ZkServiceRegistry.ZK_ADDRESS, "ip:8090");
        param.put(ZkServiceRegistry.ZK_DIGEST, "");
        param.put(ZkServiceRegistry.ENV, "test");

        serviceRegistry.init(param);
        this.routeStrategy = ExtensionLoaderFactory.load(RouteStrategy.class, this.routeType.getTypeCode());
    }

    public Object getReference() {
        return Proxy.newProxyInstance(Thread.currentThread().getContextClassLoader(), new Class[]{iface}, new InvocationHandler() {
            @Override
            public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
                // method param
                String className = method.getDeclaringClass().getName();    // iface.getName()
                String varsion_ = version;
                String methodName = method.getName();
                Class<?>[] parameterTypes = method.getParameterTypes();
                Object[] parameters = args;

                //find service
                String connectAddress = address;
                if (Objects.isNull(connectAddress)) {
                    if (!Objects.isNull(serviceRegistry)) {
                        String serviceKey = iface.getName();
                        TreeSet<String> addressSet = serviceRegistry.discovery(serviceKey);
                        if (Objects.isNull(addressSet) || addressSet.size() == 0) {
                            //donothing;
                        } else if (addressSet.size() == 1) {
                            connectAddress = addressSet.first();
                        } else {
                            if (!Objects.isNull(routeStrategy)) {
                                connectAddress = routeStrategy.route(serviceKey, addressSet);
                            }
                        }
                    }
                }
                if (Objects.isNull(connectAddress)) {
                    throw new PigeonRpcException("xxl-rpc reference bean[" + className + "] address empty");
                }

                //init client  TODO 待优化复用连接
                String ip = connectAddress.split(":")[0];
                int port = Integer.valueOf(connectAddress.split(":")[1]);
                PigeonExchanger exchanger = new PigeonExchanger(net.getTypeCode(), serializer.getTypeCode());
                PigeonURL url = new PigeonURL("rpc", ip, port, null);
                ExchangeClientHandler handler = new ExchangeClientHandler();
                ExchangeClient client = exchanger.connect(url, handler);

                // request
                PigeonRpcRequest pigeonRpcRequest = new PigeonRpcRequest();
                pigeonRpcRequest.setRequestId(UUID.randomUUID().toString());
                pigeonRpcRequest.setCreateMillisTime(System.currentTimeMillis());
                pigeonRpcRequest.setClassName(className);
                pigeonRpcRequest.setMethodName(methodName);
                pigeonRpcRequest.setParameterTypes(parameterTypes);
                pigeonRpcRequest.setParameters(parameters);

                //call
                if (CallTypeEnum.SYNC == callType) {
                    PigeonRpcFutureReponse futureResponse = new PigeonRpcFutureReponse(timeout, pigeonRpcInvoker, pigeonRpcRequest);

                    try {
                        client.request(pigeonRpcRequest);
                        PigeonRpcResponse pigeonRpcResponse = futureResponse.get(timeout, TimeUnit.MILLISECONDS);
                        if (!Objects.isNull(pigeonRpcResponse.getErrorMsg())) {
                            throw new PigeonRpcException(pigeonRpcResponse.getErrorMsg());
                        }
                        return pigeonRpcResponse.getResult();
                    } catch (Exception e) {
                        throw (e instanceof PigeonRpcException) ? e : new PigeonRpcException(e);
                    } finally {
                        futureResponse.removeInvokeFuture();
                    }
                } else if (CallTypeEnum.ASYNC == callType) {
                    PigeonRpcFutureReponse futureResponse = new PigeonRpcFutureReponse(timeout, pigeonRpcInvoker, pigeonRpcRequest);
                    try {
                        PigeonRpcInvokeFutureContext context = new PigeonRpcInvokeFutureContext(futureResponse);
                        PigeonRpcInvokeFutureContext.setContext(context);

                        client.request(pigeonRpcRequest);
                    } catch (Exception e) {
                        // future-response remove
                        futureResponse.removeInvokeFuture();

                        throw (e instanceof PigeonRpcException) ? e : new PigeonRpcException(e);
                    }


                    return null;
                } else if (CallTypeEnum.ONEWAY == callType) {
                    try {
                        client.request(pigeonRpcRequest);
                    } catch (Exception e) {
                        throw (e instanceof PigeonRpcException) ? e : new PigeonRpcException(e);
                    }
                    return null;
                } else {
                    throw new PigeonRpcException("xxl-rpc callType[" + callType + "] invalid");
                }

            }
        });
    }

}
