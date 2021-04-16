package com.moqifei.rpc.serialize.enums;

public enum  SerializerEnum {
    DEFAULT("DEFAULT","hessian2"),
    HESSIAN2("HESSIAN2","hessian2");

    private String typeName;
    private String typeCode;

    public String getTypeName() {
        return typeName;
    }

    public String getTypeCode() {
        return typeCode;
    }

    SerializerEnum(String typeName, String typeCode){
        this.typeName = typeName;
        this.typeCode = typeCode;
    }

    public static SerializerEnum fromName(String typeName){
        for (SerializerEnum serializerEnum : SerializerEnum.values()) {
            if (serializerEnum.getTypeName().equals(typeName)){
                return serializerEnum;
            }
        }
        return DEFAULT;
    }
}
