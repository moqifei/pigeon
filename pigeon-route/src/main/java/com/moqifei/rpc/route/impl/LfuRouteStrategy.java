package com.moqifei.rpc.route.impl;

import com.moqifei.rpc.route.RouteStrategy;
import com.moqifei.rpc.spi.PigeonSPI;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * LFU(Least Frequently Used) ,最近最不常被使用算法，淘汰一定时期内被访问最少的数据
 */
@PigeonSPI("lfu")
public class LfuRouteStrategy implements RouteStrategy {

    private ConcurrentHashMap<String, HashMap<String, HitRate>> lfuMap = new ConcurrentHashMap<>();

    @Override
    public String route(String serviceName, TreeSet<String> address) {
        //init
        HashMap<String, HitRate> hitRateMap = lfuMap.get(serviceName);
        if (Objects.isNull(hitRateMap)) {
            hitRateMap = new HashMap<>();
            lfuMap.putIfAbsent(serviceName, hitRateMap);
        }

        //put
        for (String addr : address) {
            if (!hitRateMap.containsKey(addr)) {
                HitRate hitRate = new HitRate(addr, 0, System.nanoTime());
                hitRateMap.put(addr, hitRate);
            }
        }

        // remove old
        List<String> delKeys = new ArrayList<>();
        hitRateMap.forEach((k, v) -> {
            if (!address.contains(k)) {
                delKeys.add(k);
            }
        });

        if (delKeys.size() > 0) {
            for (String delKey : delKeys) {
                hitRateMap.remove(delKey);
            }
        }

        HitRate minHit = Collections.min(hitRateMap.values());
        System.out.println(minHit);
        minHit.setHitCount(minHit.getHitCount()+1);

        return minHit.getKey();
    }

    class HitRate implements Comparable<HitRate> {
        private String key;
        private Integer hitCount;
        private Long atime;

        public String getKey() {
            return key;
        }

        public void setKey(String key) {
            this.key = key;
        }

        public Integer getHitCount() {
            return hitCount;
        }

        public void setHitCount(Integer hitCount) {
            this.hitCount = hitCount;
        }

        public Long getAtime() {
            return atime;
        }

        public void setAtime(Long atime) {
            this.atime = atime;
        }

        public HitRate(String key, Integer hitCount, Long atime) {
            this.key = key;
            this.hitCount = hitCount;
            this.atime = atime;
        }

        @Override
        public String toString() {
            return "HitRate{" +
                    "key='" + key + '\'' +
                    ", hitCount=" + hitCount +
                    ", atime=" + atime +
                    '}';
        }

        @Override
        public int compareTo(HitRate o) {
            return hitCount.compareTo(o.hitCount);
        }
    }
}
