package com.soundcloud.android.tracking;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
public @interface Tracking {

    Page page()   default Page.UNKNOWN;
    Click click() default Click.UNKNOWN;
}
