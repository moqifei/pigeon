package com.moqifei.rpc.route;

import java.util.TreeSet;

/**
 * 路由策略
 * @Author moqifei
 * @date 20210319
 */
public interface RouteStrategy {
    String route(String serviceName, TreeSet<String> address);
}
