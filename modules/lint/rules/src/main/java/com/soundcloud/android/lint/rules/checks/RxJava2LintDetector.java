package com.soundcloud.android.lint.rules.checks;

import static com.android.tools.lint.detector.api.Scope.JAVA_FILE;
import static com.android.tools.lint.detector.api.Scope.TEST_SOURCES;

import com.android.annotations.NonNull;
import com.android.tools.lint.detector.api.Category;
import com.android.tools.lint.detector.api.Detector;
import com.android.tools.lint.detector.api.Implementation;
import com.android.tools.lint.detector.api.Issue;
import com.android.tools.lint.detector.api.JavaContext;
import com.android.tools.lint.detector.api.LintUtils;
import com.android.tools.lint.detector.api.Severity;
import com.intellij.psi.JavaElementVisitor;
import com.intellij.psi.PsiCallExpression;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiExpressionStatement;
import com.intellij.psi.PsiMethod;
import com.intellij.psi.PsiType;

import java.util.Collections;
import java.util.EnumSet;
import java.util.List;
import java.util.regex.Pattern;

public class RxJava2LintDetector extends Detector implements Detector.JavaPsiScanner {

    public static final Issue ISSUE_METHOD_MISSING_CHECK_RESULT =
            Issue.create("sc.CheckResult",
                         "Ignoring results",
                         "Methods returning Rx2 types might have no side effect, and calling them without handling the result is suspicious.",
                         Category.CORRECTNESS,
                         6,
                         Severity.WARNING,
                         new Implementation(RxJava2LintDetector.class, EnumSet.of(JAVA_FILE, TEST_SOURCES)));

    private static final String CLASS_DISPOSABLE = "io.reactivex.disposables.Disposable";
    private static final String CLASS_TEST_OBSERVER = "io.reactivex.observers.TestObserver";
    private static final String CLASS_TEST_SUBSCRIBER = "io.reactivex.subscribers.TestSubscriber";
    private static final Pattern PATTERN_PACKAGE_RX2 = Pattern.compile("io\\.reactivex\\.[\\w]+");
    private static final String POSTFIX_METHOD_SUPPRESSION = "AndForget";

    @Override
    public List<Class<? extends PsiElement>> getApplicablePsiTypes() {
        return Collections.singletonList(PsiCallExpression.class);
    }

    @Override
    public JavaElementVisitor createPsiVisitor(@NonNull final JavaContext context) {
        return new Visitor(context);
    }

    private static class Visitor extends JavaElementVisitor {
        private final JavaContext context;

        Visitor(final JavaContext context) {
            this.context = context;
        }

        @Override
        public void visitCallExpression(PsiCallExpression call) {
            final PsiMethod method = call.resolveMethod();
            if (method == null) {
                return;
            }
            if (context.getDriver().isSuppressed(context, ISSUE_METHOD_MISSING_CHECK_RESULT, method)) {
                return;
            }
            checkResultUsed(context, method, call);
        }

        private void checkResultUsed(JavaContext context, PsiMethod method, PsiCallExpression call) {
            if (!isRxJava2Type(method.getReturnType())) {
                return;
            }
            if (LintUtils.skipParentheses(call.getParent()) instanceof PsiExpressionStatement) {
                String methodName = JavaContext.getMethodName(call);
                String message = String.format("The result of `%1$s` is not used.", methodName);
                if (methodName.endsWith(POSTFIX_METHOD_SUPPRESSION)) {
                    return;
                }
                context.report(ISSUE_METHOD_MISSING_CHECK_RESULT, call, context.getNameLocation(call), message);
            }
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
