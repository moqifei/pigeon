package com.moqifei.rpc.net.url;

import java.io.Serializable;
import java.util.Map;

public class PigeonURL implements Serializable {
    private final String protocol;
    private final String host;
    private final int port;
    private final Map<String, String> parameters;

    public PigeonURL(String protocol, String host, int port, Map<String, String> parameters) {
        this.protocol = protocol;
        this.host = host;
        this.port = port;
        this.parameters = parameters;
    }

    public String getProtocol() {
        return protocol;
    }

    public String getHost() {
        return host;
    }

    public int getPort() {
        return port;
    }

    public Map<String, String> getParameters() {
        return parameters;
    }
}
