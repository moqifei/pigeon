package com.moqifei.rpc.route.impl;

import com.moqifei.rpc.route.RouteStrategy;
import com.moqifei.rpc.spi.PigeonSPI;

import java.util.SortedMap;
import java.util.TreeMap;
import java.util.TreeSet;

@PigeonSPI("chash")
public class ConsistentHashRouteStrategy implements RouteStrategy {

    private static final long FNV_32_INIT = 2166136261L;
    private static final long FNV_32_PRIME = 16777619;
    private static final int VIRTUAL_NODE_NUM = 5;

    /**
     * FNV1_32_HASH
     *
     * @param key
     * @return
     */
    private long hash(String key) {
        long rv = FNV_32_INIT;
        int len = key.length();
        for (int i = 0; i < len; i++) {
            rv *= FNV_32_PRIME;
            rv ^= key.charAt(i);
        }
        return rv & rv & 0xffffffffL; /* Truncate to 32-bits */
    }


    @Override
    public String route(String serviceName, TreeSet<String> address) {
        TreeMap<Long, String> addressRing = new TreeMap<>();
        //增加虚拟节点
        for (String addr : address) {
            for (int i = 0; i < VIRTUAL_NODE_NUM; i++) {
                long addressHash = hash("Virutal "+ addr + " Node "+i);
                addressRing.put(addressHash, addr);
            }
        }

        long serviceHash = hash(serviceName);

        //获取离serviceHash最近的节点
        SortedMap<Long, String> ringItems = addressRing.tailMap(serviceHash);
        if(!ringItems.isEmpty()){
            return ringItems.get(ringItems.firstKey());
        }

        return addressRing.firstEntry().getValue();
    }
}
