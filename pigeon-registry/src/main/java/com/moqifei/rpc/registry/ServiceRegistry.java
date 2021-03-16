package com.moqifei.rpc.registry;

import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

/**
 * service registry
 * /pigeon/rpc/
 *            -key01(service01)
 *                 value01(ip:port01)
 *                 value02(ip:port02)
 */
public interface ServiceRegistry {
    /**
     * start
     */
    void init(Map<String, String> param);

    /**
     * stop
     */
    void destroy();

    /**
     * registry service, for mult
     * @param keys    service key
     * @param value   ip:port
     * @return
     */
    boolean registry(Set<String> keys, String value);

    /**
     * remove service, for mult
     * @param keys
     * @param value
     * @return
     */
    boolean remove(Set<String> keys, String value);

    /**
     * discovery services, for mult
     * @param keys
     * @return
     */
    Map<String, TreeSet<String>> discovery(Set<String> keys);

    /**
     * discovery service, for one
     * @param keys
     * @return
     */
    TreeSet<String> discovery(String keys);
}
