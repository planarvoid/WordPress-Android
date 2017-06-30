package com.soundcloud.android.memento.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Target;

/**
 * Added to a class extending Lint's `Detector`, it will indicate that all containing Issue constants will
 * be added to the to be generated IssueRegistry.
 *
 * @see Exclude to ignore a certain Issue to be added
 */
@Target(ElementType.TYPE)
public @interface LintDetector {
}
