package com.moqifei.rpc.route.impl;

import com.moqifei.rpc.route.RouteStrategy;
import com.moqifei.rpc.spi.PigeonSPI;

import java.util.Objects;
import java.util.TreeSet;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

@PigeonSPI("round")
public class RoundRouteStrategy implements RouteStrategy {
    private ConcurrentHashMap<String, AtomicInteger> roundCounter = new ConcurrentHashMap<>();

    /**
     * 通过二进制位操作将originValue转化为非负数:
     *
     * @param originValue
     * @return
     */
    private int getNonNegative(int originValue){
        return 0x7fffffff & originValue;
    }

    private int count(String serviceName){
        AtomicInteger atomicInteger = roundCounter.get(serviceName);
        if(Objects.isNull(atomicInteger)){
            atomicInteger = new AtomicInteger(0);
            roundCounter.put(serviceName,atomicInteger);
        }
        return getNonNegative(atomicInteger.incrementAndGet());
    }

    @Override
    public String route(String serviceName, TreeSet<String> address) {
        String[] addressArray = address.toArray(new String[address.size()]);
        int index = count(serviceName);
        return addressArray[index % address.size()];
    }
}
