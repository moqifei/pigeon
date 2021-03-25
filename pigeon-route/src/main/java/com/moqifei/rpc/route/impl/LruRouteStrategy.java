package com.moqifei.rpc.route.impl;

import com.moqifei.rpc.route.RouteStrategy;
import com.moqifei.rpc.spi.PigeonSPI;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 *  经典LRU：基于链表
 *  1.新数据插入到链表头部；
 *  2.每当缓存数据命中，则将数据移动到链表头部；
 *  3.当链表满的时候，将链表尾部的数据丢失
 *
 *  服务发现LRU：基于LinkedhashMap(initCapacity, loadFactor, accessOrder)
 *  accessOrder参数: 为true时访问顺序排序（put/get排序，新访问的放到链表的后面）；为false时插入（put）顺序排序，FIFO
 *  removeEldestEntry方法：put时调用，返回true时删除最近最少使用元素，指定LinkedHashMap大小，并重写该方法，超出指定大小时返回true，即实现K-LRU
 *
 *
 */
@PigeonSPI("lru")
public class LruRouteStrategy implements RouteStrategy {
    private ConcurrentHashMap<String, LinkedHashMap<String, String>> routeMap = new ConcurrentHashMap<>();
    private static final int REMOVE_ELDEST_ENTRY_THRESHOLD = 1000;

    @Override
    public String route(String serviceName, TreeSet<String> address) {
        LinkedHashMap<String,String> lruMap = routeMap.get(serviceName);
        if(Objects.isNull(lruMap)){
            lruMap = new LinkedHashMap<String, String>(16,0.75f, true){
                @Override
                protected  boolean removeEldestEntry(Map.Entry<String, String> eldest){
                    if (super.size() > REMOVE_ELDEST_ENTRY_THRESHOLD){
                        return true;
                    }else{
                        return false;
                    }
                }
            };
            routeMap.putIfAbsent(serviceName, lruMap);
        }

        for(String addr : address){
            if(!lruMap.containsValue(addr)){
                lruMap.put(addr, addr);
            }
        }

        // remove old
        List<String> delKeys = new ArrayList<>();
        lruMap.forEach((k,v) -> {
            if(!address.contains(k)){
                delKeys.add(k);
            }
        });

        if (delKeys.size() > 0) {
            for (String delKey: delKeys) {
                lruMap.remove(delKey);
            }
        }

        // load
        String eldestKey = lruMap.entrySet().iterator().next().getKey();
        String eldestValue = lruMap.get(eldestKey);
        return eldestValue;
    }
}
