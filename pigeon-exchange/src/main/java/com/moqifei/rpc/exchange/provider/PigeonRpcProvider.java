package com.moqifei.rpc.exchange.provider;

import com.moqifei.rpc.exchange.invoker.PigeonRpcInvoker;
import com.moqifei.rpc.net.params.PigeonRpcRequest;
import com.moqifei.rpc.net.params.PigeonRpcResponse;

import java.lang.reflect.Method;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class PigeonRpcProvider {
    private PigeonRpcProvider(){}
    private static volatile PigeonRpcProvider INSTANCE= new PigeonRpcProvider();
    public static PigeonRpcProvider getInstance(){return INSTANCE;}

    private Map<String, Object> serviceData = new ConcurrentHashMap<>();

    public Map<String, Object> getServiceData() {
        return serviceData;
    }

    public void addService(String iface, String version, Object serviceBean){
        String serviceKey = makeServiceKey(iface, version);
        serviceData.put(serviceKey, serviceBean);
    }


    public PigeonRpcResponse call(PigeonRpcRequest pigeonRpcRequest){
        PigeonRpcResponse pigeonRpcResponse = new PigeonRpcResponse();
        pigeonRpcResponse.setRequestId(pigeonRpcRequest.getRequestId());

        // match service bean
        String serviceKey = makeServiceKey(pigeonRpcRequest.getClassName(), pigeonRpcRequest.getVersion());
        Object serviceBean = serviceData.get(serviceKey);

        // valid
        if (serviceBean == null) {
            pigeonRpcResponse.setErrorMsg("The serviceKey["+ serviceKey +"] not found.");
            return pigeonRpcResponse;
        }

        try{
            // invoke
            Class<?> serviceClass = serviceBean.getClass();
            String methodName = pigeonRpcRequest.getMethodName();
            Class<?>[] parameterTypes = pigeonRpcRequest.getParameterTypes();
            Object[] parameters = pigeonRpcRequest.getParameters();

            Method method = serviceClass.getMethod(methodName, parameterTypes);
            method.setAccessible(true);
            Object result = method.invoke(serviceBean, parameters);

            pigeonRpcResponse.setResult(result);
        }catch (Exception e){
            pigeonRpcResponse.setErrorMsg(e.toString());
        }
        return pigeonRpcResponse;
    }

    public static String makeServiceKey(String iface, String version){
        String serviceKey = iface;
        if (version!=null && version.trim().length()>0) {
            serviceKey += "#".concat(version);
        }
        return serviceKey;
    }

}
