package com.moqifei.rpc.net.enums;

import sun.nio.ch.Net;

public enum  NetEnum {
    DEFAULT("DEFAULT","netty"),
    NETTY("NETTY","netty");

    private String typeName;
    private String typeCode;

    public String getTypeName() {
        return typeName;
    }

    public String getTypeCode() {
        return typeCode;
    }

    NetEnum(String typeName, String typeCode){
       this.typeName = typeName;
       this.typeCode = typeCode;
    }

    public static NetEnum fromName(String typeName){
        for (NetEnum netEnum : NetEnum.values()) {
            if (netEnum.getTypeName().equals(typeName)){
                return netEnum;
            }
        }
        return DEFAULT;
    }
}
