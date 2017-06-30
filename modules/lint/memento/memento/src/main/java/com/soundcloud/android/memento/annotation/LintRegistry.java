package com.soundcloud.android.memento.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Target;

/**
 * In case the IssueRegistry will require more modifications that implementing the `getIssues()` method,
 * this annotation needs to be added to the custom abstract IssueRegistry.
 *
 * The generated IssueRegistry will be called `Memento_<className>`.
 * If no class is annotated, it will be called `Memento_IssueRegistry`.
 *
 * This class needs to be registered in the MANIFEST.MF file as:
 * `Lint-Registry: <fully qualified class name>`
 *
 * This can be automatically done by applying the `com.soundcloud.memento` plugin to the build gradle script.
 */
@Target(ElementType.TYPE)
public @interface LintRegistry {
}
