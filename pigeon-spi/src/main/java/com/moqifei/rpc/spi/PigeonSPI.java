package com.moqifei.rpc.spi;

import java.lang.annotation.*;

@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface PigeonSPI {
    /**
     * Value string.
     *
     * @return the string
     */
    String value();

    /**
     * Order int.
     *
     * @return the int
     */
    int order() default 0;

    /**
     * Scope type scope type.
     *
     * @return the scope type
     */
    ScopeType scopeType() default ScopeType.SINGLETON;
}
