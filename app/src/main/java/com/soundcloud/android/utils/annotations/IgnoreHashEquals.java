package com.soundcloud.android.utils.annotations;

import static java.lang.annotation.ElementType.FIELD;
import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.ElementType.PARAMETER;
import static java.lang.annotation.RetentionPolicy.SOURCE;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;

@Retention(SOURCE)
@Target({METHOD, PARAMETER, FIELD})
public @interface IgnoreHashEquals {
    // for use with https://github.com/REggar/auto-value-ignore-hash-equals
}