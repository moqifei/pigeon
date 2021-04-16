package com.moqifei.rpc.registry.enums;

public enum RegistryTypeEnum {
    DEFAULT("DEFAULT","zk"),
    ZK("ZK","zk"),
    NACOS("NACOS","nacos"),
    LOCAL("LOCAL","local");


    private String typeName;
    private String typeCode;

    public String getTypeName() {
        return typeName;
    }

    public String getTypeCode() {
        return typeCode;
    }

    RegistryTypeEnum(String typeName, String typeCode){
        this.typeName = typeName;
        this.typeCode = typeCode;
    }

    public static RegistryTypeEnum fromName(String typeName){
        for (RegistryTypeEnum rgistryType : RegistryTypeEnum.values()) {
            if (rgistryType.getTypeName().equals(typeName)){
                return rgistryType;
            }
        }
        return DEFAULT;
    }
}
