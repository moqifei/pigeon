package com.moqifei.rpc.route.impl;

import com.moqifei.rpc.route.RouteStrategy;
import com.moqifei.rpc.spi.PigeonSPI;

import java.util.TreeSet;
import java.util.concurrent.ThreadLocalRandom;

@PigeonSPI("random")
public class RandomRouteStrategy implements RouteStrategy {
    @Override
    public String route(String serviceName, TreeSet<String> address) {
        String[] addressArray = address.toArray(new String[address.size()]);
        String getAddress = addressArray[ThreadLocalRandom.current().nextInt(address.size())];
        return getAddress;
    }
}
