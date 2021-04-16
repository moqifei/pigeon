package com.moqifei.rpc.exchange.enums;

import com.moqifei.rpc.serialize.enums.SerializerEnum;

public enum CallTypeEnum {
    DEFAULT("DEFAULT","sync"),
    SYNC("SYNC","sync"),
    ASYNC("ASYNC","async"),
    CALLBACK("CALLBACK","callback"),
    ONEWAY("ONEWAY","onway");


    private String typeName;
    private String typeCode;

    public String getTypeName() {
        return typeName;
    }

    public String getTypeCode() {
        return typeCode;
    }

    CallTypeEnum(String typeName, String typeCode){
        this.typeName = typeName;
        this.typeCode = typeCode;
    }

    public static CallTypeEnum fromName(String typeName){
        for (CallTypeEnum callTypeEnum : CallTypeEnum.values()) {
            if (callTypeEnum.getTypeName().equals(typeName)){
                return callTypeEnum;
            }
        }
        return DEFAULT;
    }
}
