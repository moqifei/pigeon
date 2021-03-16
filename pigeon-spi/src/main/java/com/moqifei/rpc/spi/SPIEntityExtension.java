package com.moqifei.rpc.spi;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode
@AllArgsConstructor
public class SPIEntityExtension {
    private String name;

    private Class<?> serviceClass;

    private Integer order;

    private ScopeType scopeType;
}
