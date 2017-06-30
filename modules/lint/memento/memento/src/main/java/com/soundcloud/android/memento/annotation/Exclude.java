package com.soundcloud.android.memento.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Target;

/**
 * Can be added to Lint Issue's in case they should not be added to the IssueRegistry.
 */
@Target(ElementType.FIELD)
public @interface Exclude {
}
