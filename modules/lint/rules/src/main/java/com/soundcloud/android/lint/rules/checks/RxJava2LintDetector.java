package com.soundcloud.android.lint.rules.checks;

import static com.android.tools.lint.detector.api.Scope.JAVA_FILE;
import static com.android.tools.lint.detector.api.Scope.TEST_SOURCES;

import com.android.annotations.NonNull;
import com.android.tools.lint.detector.api.Category;
import com.android.tools.lint.detector.api.Detector;
import com.android.tools.lint.detector.api.Implementation;
import com.android.tools.lint.detector.api.Issue;
import com.android.tools.lint.detector.api.JavaContext;
import com.android.tools.lint.detector.api.Severity;
import com.intellij.psi.JavaElementVisitor;
import com.intellij.psi.PsiAnnotation;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiMethod;
import com.intellij.psi.PsiType;

import java.util.Collections;
import java.util.EnumSet;
import java.util.List;
import java.util.regex.Pattern;

/**
 * Based on rxjava2-lint-rules from https://github.com/vanniktech/lint-rules.
 */
public class RxJava2LintDetector extends Detector implements Detector.JavaPsiScanner {

    public static final Issue ISSUE_METHOD_MISSING_CHECK_RESULT =
            Issue.create("sc.MissingCheckResult",
                         "Method is missing the `@CheckResult` annotation",
                         "Methods returning RxJava Reactive Types should be annotated with the `@CheckResult` annotation.",
                         Category.MESSAGES,
                         8,
                         Severity.WARNING,
                         new Implementation(RxJava2LintDetector.class, EnumSet.of(JAVA_FILE, TEST_SOURCES)));

    private static final String CLASS_ANNOTATION_CHECK_RESULT = "android.support.annotation.CheckResult";
    private static final String CLASS_DISPOSABLE = "io.reactivex.disposables.Disposable";
    private static final String CLASS_TEST_OBSERVER = "io.reactivex.observers.TestObserver";
    private static final String CLASS_TEST_SUBSCRIBER = "io.reactivex.subscribers.TestSubscriber";
    private static final Pattern PATTERN_PACKAGE_RX2 = Pattern.compile("io\\.reactivex\\.[\\w]+");

    @Override
    public List<Class<? extends PsiElement>> getApplicablePsiTypes() {
        return Collections.singletonList(PsiMethod.class);
    }

    @Override
    public JavaElementVisitor createPsiVisitor(@NonNull final JavaContext context) {
        return new CheckReturnValueVisitor(context);
    }

    private static class CheckReturnValueVisitor extends JavaElementVisitor {
        private final JavaContext context;

        CheckReturnValueVisitor(final JavaContext context) {
            this.context = context;
        }

        @Override
        public void visitMethod(final PsiMethod method) {
            final PsiType returnType = method.getReturnType();
            if (returnType == null || !isRxJava2Type(returnType)) {
                return;
            }
            if (hasCheckResultAnnotation(method)) {
                return;
            }
            context.report(ISSUE_METHOD_MISSING_CHECK_RESULT, method, context.getLocation(method), "Method should have `@CheckResult` annotation");
        }

        private static boolean hasCheckResultAnnotation(PsiMethod method) {
            final PsiAnnotation[] annotations = method.getModifierList().getAnnotations();
            for (final PsiAnnotation annotation : annotations) {
                if (CLASS_ANNOTATION_CHECK_RESULT.equals(annotation.getQualifiedName())) {
                    return true;
                }
            }
            return false;
        }

        static boolean isRxJava2Type(final PsiType psiType) {
            final String canonicalText = psiType.getCanonicalText();
            return PATTERN_PACKAGE_RX2.matcher(canonicalText).matches()
                    || CLASS_DISPOSABLE.equals(canonicalText)
                    || CLASS_TEST_OBSERVER.equals(canonicalText)
                    || CLASS_TEST_SUBSCRIBER.equals(canonicalText);
        }
    }
}