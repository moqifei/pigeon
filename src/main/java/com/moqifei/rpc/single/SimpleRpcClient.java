package com.moqifei.rpc.single;

import java.lang.reflect.Proxy;

public class SimpleRpcClient {

    /**
     * 泛型处理方法
     * @param interfaceClass  远程调用接口
     * @param host            远程地址
     * @param port            远程端口
     * @param <T>
     * @return
     * @throws Exception
     */
    public <T> T call(final Class<T> interfaceClass, final String host, final int port) throws Exception {
        if (interfaceClass == null) {
            throw new IllegalArgumentException("Interface class == null");
        }
        if (! interfaceClass.isInterface()) {
            throw new IllegalArgumentException("The " + interfaceClass.getName() + " must be interface class!");
        }
        if (host == null || host.length() == 0) {
            throw new IllegalArgumentException("Host == null!");
        }
        if (port <= 0 || port > 65535) {
            throw new IllegalArgumentException("Invalid port " + port);
        }
        //动态代理，生成代理类，调用远程接口方法，并增加RPC网络通讯细节
        return (T) Proxy.newProxyInstance(interfaceClass.getClassLoader(), new Class<?>[]{interfaceClass}, new SimpleRpcClientInvocationHandler(host, port));
    }
}
