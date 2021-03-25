package com.moqifei.rpc.registry.impl;

import com.alibaba.nacos.api.exception.NacosException;
import com.alibaba.nacos.api.naming.NamingFactory;
import com.alibaba.nacos.api.naming.NamingService;
import com.alibaba.nacos.api.naming.listener.NamingEvent;
import com.alibaba.nacos.api.naming.pojo.Instance;
import com.google.common.base.Strings;
import com.moqifei.rpc.registry.ServiceRegistry;
import com.moqifei.rpc.spi.PigeonSPI;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

/**
 * service registry for "nacos"
 *
 * @author moqifei 20210318
 */

@PigeonSPI("nacosRegistry")
public class NacosServiceRegistry implements ServiceRegistry {

    private static Logger logger = LoggerFactory.getLogger(NacosServiceRegistry.class);

    private String ip;
    private String port;
    private NamingService namingService = null;
    private Thread refreshThread;

    private volatile boolean refreshThreadStop = false;

    //注册服务缓存
    private volatile ConcurrentHashMap<String, TreeSet<String>> registryData = new ConcurrentHashMap<>();
    //发现服务缓存
    private volatile ConcurrentHashMap<String, TreeSet<String>> discoveryData = new ConcurrentHashMap<>();

    /**
     * start
     *
     * @param param
     */
    @Override
    public void init(Map<String, String> param) {
        ip = param.get("ip");
        port = param.get("port");

        if (Strings.isNullOrEmpty(ip)) {
            throw new RuntimeException("pigeon-rpc nacos address can not be empty");
        }
        if (Strings.isNullOrEmpty(port)) {
            throw new RuntimeException("pigeon-rpc nacos port can not be empty");
        }

        String serverAddr = new StringBuffer().append(ip).append(":").append(port).toString();
        try {
            namingService = NamingFactory.createNamingService(serverAddr);
        } catch (NacosException e) {
            logger.error(e.getMessage(), e);
        }

        refreshThread = new Thread(new Runnable() {
            @Override
            public void run() {

                while (!refreshThreadStop) {
                    try {
                        TimeUnit.SECONDS.sleep(60);

                        // refreshDiscoveryData (all)：cycle check
                        refreshDiscoveryData(null);

                        // refresh RegistryData
                        // refreshRegistryData();
                    } catch (Exception e) {
                        if (!refreshThreadStop) {
                            logger.error(">>>>>>>>>>> pigeon-rpc, refresh thread error.", e);
                        }
                    }
                }
                logger.info(">>>>>>>>>>> pigeon-rpc, refresh thread stoped.");
            }
        });

        refreshThread.setName("pigeon-rpc, NacosServiceRegistry refresh thread.");
        refreshThread.setDaemon(true);
        refreshThread.start();

    }

    /**
     * stop
     */
    @Override
    public void destroy() {
        try {
            namingService.shutDown();
        } catch (NacosException e) {
            logger.error(e.getMessage(), e);
        }

        if (refreshThread != null) {
            refreshThreadStop = true;
            refreshThread.interrupt();
        }
    }

    /**
     * registry service, for mult
     *
     * @param keys  service key
     * @param value ip:port
     * @return
     */
    @Override
    public boolean registry(Set<String> keys, String value) {

        for (String key : keys) {
            // local cache
            TreeSet<String> values = registryData.get(key);
            if (values == null) {
                values = new TreeSet<>();
                registryData.put(key, values);
            }
            values.add(value);

            registryNacosInstance(key, value);
        }
        logger.info(">>>>>>>>>>> pigeon-rpc, nacos registry success, keys = {}, value = {}", keys, value);
        return true;

    }

    private void registryNacosInstance(String serviceName, String value) {

        String[] address = value.split(":");
        Instance instance = new Instance();
        instance.setIp(address[0]);//IP
        instance.setPort(Integer.valueOf(address[1]));//端口
        instance.setServiceName(serviceName);//服务名
        instance.setEnabled(true);//true: 上线 false: 下线
        instance.setHealthy(true);//健康状态
        instance.setWeight(1.0);//权重
        instance.addMetadata("nacos-sdk-java-discovery", "true");//元数据
        try {
            namingService.registerInstance(serviceName, instance);
        } catch (NacosException e) {
            logger.error(e.getMessage(), e);
        }
    }


    /**
     * remove service, for mult
     *
     * @param keys
     * @param value
     * @return
     */
    @Override
    public boolean remove(Set<String> keys, String value) {
        for (String key : keys) {
            TreeSet<String> values = discoveryData.get(key);
            if (values != null) {
                values.remove(value);
            }
            deregisterInstance(key, value);
        }
        logger.info(">>>>>>>>>>> pigeon-rpc, remove success, keys = {}, value = {}", keys, value);
        return true;
    }

    private void deregisterInstance(String serviceName, String value) {
        String[] address = value.split(":");
        Instance instance = new Instance();
        instance.setIp(address[0]);//IP
        instance.setPort(Integer.valueOf(address[1]));//端口
        instance.setServiceName(serviceName);//服务名
        instance.setEnabled(true);//true: 上线 false: 下线
        instance.setHealthy(true);//健康状态
        instance.setWeight(1.0);//权重
        instance.setClusterName("rpc-demo");
        instance.addMetadata("nacos-sdk-java-discovery", "true");//元数据
        try {
            namingService.deregisterInstance(serviceName, instance);
        } catch (NacosException e) {
            logger.error(e.getMessage(), e);
        }
    }

    /**
     * discovery services, for mult
     *
     * @param keys
     * @return
     */
    @Override
    public Map<String, TreeSet<String>> discovery(Set<String> keys) {
        if (keys == null || keys.size() == 0) {
            return null;
        }
        Map<String, TreeSet<String>> registryDataTmp = new HashMap<String, TreeSet<String>>();
        for (String key : keys) {
            TreeSet<String> valueSetTmp = discovery(key);
            if (valueSetTmp != null) {
                registryDataTmp.put(key, valueSetTmp);
            }
        }
        return registryDataTmp;
    }

    /**
     * discovery service, for one
     *
     * @param key
     * @return
     */
    @Override
    public TreeSet<String> discovery(String key) {
        // local cache
        TreeSet<String> values = discoveryData.get(key);
        if (values == null) {

            // refreshDiscoveryData (one)：first use
            refreshDiscoveryData(key);
            //listening
            addListener(key);

            values = discoveryData.get(key);
        }

        return values;
    }

    private void addListener(String key) {
        try {
            namingService.subscribe(key, event -> {
                if(event instanceof NamingEvent){
                    NamingEvent namingEvent = (NamingEvent) event;
                    if(namingEvent.getInstances().get(0).isEnabled()){

                    }
                    //TODO 检测服务变化，更新DiscoveryData
                    System.out.println("当前线程：" + Thread.currentThread().getName() + " ,监听到实例内容：" + namingEvent.getInstances());
                }

            });
        } catch (NacosException e) {
            logger.error(e.getMessage(), e);
        }
    }

    private void refreshDiscoveryData(String key) {
        Set<String> keys = new HashSet<String>();
        if (key != null && key.trim().length() > 0) {
            keys.add(key);
        } else {
            if (discoveryData.size() > 0) {
                keys.addAll(discoveryData.keySet());
            }
        }

        if (keys.size() > 0) {
            for (String keyItem : keys) {
                List<Instance> healthyInstancesAfterDeregister = null;
                try {
                    healthyInstancesAfterDeregister = namingService.selectInstances(keyItem, true);
                } catch (NacosException e) {
                    logger.error(e.getMessage(), e);
                }

                // exist-values
                TreeSet<String> existValues = discoveryData.get(keyItem);
                if (existValues == null) {
                    existValues = new TreeSet<String>();
                    discoveryData.put(keyItem, existValues);
                }

                if (healthyInstancesAfterDeregister.size() > 0) {
                    existValues.clear();
                    Set<String> registerInfo = new TreeSet<>();
                    for(Instance instance : healthyInstancesAfterDeregister){
                        String address = instance.getIp()+":"+instance.getPort();
                        registerInfo.add(address);
                    }

                    existValues.addAll(registerInfo);
                }
            }
            logger.info(">>>>>>>>>>> pigeon-rpc, refresh discovery data success, discoveryData = {}", discoveryData);
           System.out.println(">>>>>>>>>>> pigeon-rpc, refresh discovery data success, discoveryData = {}"+discoveryData);
        }
    }
}
