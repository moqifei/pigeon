package com.moqifei.rpc.route.enums;

public enum RouteTypeEnum {
    DEFAULT("DEFAULT","chash"),
    SYNC("CHASH","chash"),
    LFU("LFU","lfu"),
    LRU("LRU","lru"),
    RANDOM("RANDOM","random"),
    ROUND("ROUND","round");


    private String typeName;
    private String typeCode;

    public String getTypeName() {
        return typeName;
    }

    public String getTypeCode() {
        return typeCode;
    }

    RouteTypeEnum(String typeName, String typeCode){
        this.typeName = typeName;
        this.typeCode = typeCode;
    }

    public static RouteTypeEnum fromName(String typeName){
        for (RouteTypeEnum routeTypeEnum : RouteTypeEnum.values()) {
            if (routeTypeEnum.getTypeName().equals(typeName)){
                return routeTypeEnum;
            }
        }
        return DEFAULT;
    }
}
