package com.soundcloud.android.lint.rules.checks;

import static com.android.tools.lint.detector.api.Scope.JAVA_FILE;
import static com.android.tools.lint.detector.api.Scope.JAVA_FILE_SCOPE;
import static com.android.tools.lint.detector.api.Scope.TEST_SOURCES;

import com.android.annotations.NonNull;
import com.android.annotations.Nullable;
import com.android.tools.lint.client.api.JavaEvaluator;
import com.android.tools.lint.detector.api.Category;
import com.android.tools.lint.detector.api.Detector;
import com.android.tools.lint.detector.api.Implementation;
import com.android.tools.lint.detector.api.Issue;
import com.android.tools.lint.detector.api.JavaContext;
import com.android.tools.lint.detector.api.LintUtils;
import com.android.tools.lint.detector.api.Severity;
import com.intellij.psi.JavaElementVisitor;
import com.intellij.psi.PsiCallExpression;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiCodeBlock;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiExpression;
import com.intellij.psi.PsiExpressionStatement;
import com.intellij.psi.PsiField;
import com.intellij.psi.PsiMethod;
import com.intellij.psi.PsiMethodCallExpression;
import com.intellij.psi.PsiReferenceExpression;
import com.intellij.psi.PsiStatement;
import com.intellij.psi.PsiType;

import java.util.Arrays;
import java.util.Collections;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;

public class RxJava2Detector extends Detector implements Detector.JavaPsiScanner {

    public static final Issue ISSUE_METHOD_MISSING_CHECK_RESULT =
            Issue.create("sc.CheckResult",
                         "Ignoring results",
                         "Methods returning Rx2 types might have no side effect, and calling them without handling the result is suspicious.",
                         Category.CORRECTNESS,
                         6,
                         Severity.WARNING,
                         new Implementation(RxJava2Detector.class, EnumSet.of(JAVA_FILE, TEST_SOURCES)));
    public static final Issue ISSUE_MISSING_COMPOSITE_DISPOSABLE_RECYCLED =
            Issue.create("sc.MissingCompositeDisposableRecycle", "Not recycling CompositeDisposable",
                         "A class is using CompositeDisposable and not recycling the List through `clear()` or `dispose()`.",
                         Category.CORRECTNESS,
                         10,
                         Severity.ERROR,
                         new Implementation(RxJava2Detector.class, JAVA_FILE_SCOPE));
    public static final Issue ISSUE_DISPOSE_COMPOSITE_DISPOSABLE =
            Issue.create("sc.DisposeCompositeDisposable", "Using dispose() instead of clear()",
                         "Instead of using dispose(), clear() should be used.",
                         Category.CORRECTNESS,
                         8,
                         Severity.WARNING,
                         new Implementation(RxJava2Detector.class, JAVA_FILE_SCOPE));

    private static final String CLASS_COMPOSITE_DISPOSABLE = "io.reactivex.disposables.CompositeDisposable";
    private static final String CLASS_DISPOSABLE = "io.reactivex.disposables.Disposable";
    private static final String CLASS_TEST_OBSERVER = "io.reactivex.observers.TestObserver";
    private static final String CLASS_TEST_SUBSCRIBER = "io.reactivex.subscribers.TestSubscriber";
    private static final String METHOD_CLEAR = "clear";
    private static final String METHOD_DISPOSE = "dispose";
    private static final String POSTFIX_METHOD_SUPPRESSION = "AndForget";
    private static final Pattern PATTERN_PACKAGE_RX2 = Pattern.compile("io\\.reactivex\\.[\\w]+");

    @Override
    public List<Class<? extends PsiElement>> getApplicablePsiTypes() {
        return Arrays.asList(PsiCallExpression.class, PsiClass.class);
    }

    @Override
    public JavaElementVisitor createPsiVisitor(@NonNull final JavaContext context) {
        return new Visitor(context);
    }

    @Override
    public List<String> getApplicableMethodNames() {
        return Collections.singletonList(METHOD_DISPOSE);
    }

    @Override
    public void visitMethod(JavaContext context, JavaElementVisitor visitor, PsiMethodCallExpression call, PsiMethod method) {
        final JavaEvaluator evaluator = context.getEvaluator();
        if (evaluator.isMemberInClass(method, CLASS_COMPOSITE_DISPOSABLE)) {
            context.report(ISSUE_DISPOSE_COMPOSITE_DISPOSABLE,
                           call,
                           context.getNameLocation(call),
                           "Usage of `clear()` instead of `dispose()`.");
        }
    }

    private static class Visitor extends JavaElementVisitor {
        private final JavaContext context;

        Visitor(final JavaContext context) {
            this.context = context;
        }

        @Override
        public void visitClass(final PsiClass clazz) {
            final Set<PsiField> compositeDisposables = getCompositeDisposables(clazz);
            final PsiMethod[] allMethods = clazz.getAllMethods();
            for (final PsiMethod allMethod : allMethods) {
                final PsiCodeBlock body = allMethod.getBody();
                if (body == null) {
                    continue;
                }
                removeCompositeDisposableIfClearedInMethod(context, compositeDisposables, body);
            }
            for (final PsiField compositeDisposable : compositeDisposables) {
                context.report(ISSUE_MISSING_COMPOSITE_DISPOSABLE_RECYCLED,
                               compositeDisposable,
                               context.getLocation(compositeDisposable),
                               String.format("`%s` is never recycled.", compositeDisposable.getName()));
            }
        }

        private static void removeCompositeDisposableIfClearedInMethod(JavaContext context, Set<PsiField> compositeDisposables, PsiCodeBlock body) {
            final JavaEvaluator evaluator = context.getEvaluator();
            final PsiStatement[] statements = body.getStatements();
            for (final PsiStatement statement : statements) {
                final PsiReferenceExpression methodExpression = getMethodExpression(statement);
                if (methodExpression == null) {
                    continue;
                }
                final PsiMethod method = (PsiMethod) methodExpression.resolve();
                if (isClearOrDisposeMethod(evaluator, method)) {
                    final PsiExpression qualifierExpression = methodExpression.getQualifierExpression();
                    if (qualifierExpression == null || !(qualifierExpression instanceof PsiReferenceExpression)) {
                        continue;
                    }
                    final PsiElement resolvedElement = ((PsiReferenceExpression) qualifierExpression).resolve();
                    if (resolvedElement != null && resolvedElement instanceof PsiField) {
                        final PsiField qualifierField = (PsiField) resolvedElement;
                        compositeDisposables.remove(qualifierField);
                    }
                }
            }
        }

        @Nullable
        private static PsiReferenceExpression getMethodExpression(PsiStatement statement) {
            if (!(statement instanceof PsiExpressionStatement)) {
                return null;
            }
            final PsiExpressionStatement expressionStatement = (PsiExpressionStatement) statement;
            if (!(expressionStatement.getExpression() instanceof PsiMethodCallExpression)) {
                return null;
            }
            return ((PsiMethodCallExpression) expressionStatement.getExpression()).getMethodExpression();
        }

        private static boolean isClearOrDisposeMethod(JavaEvaluator evaluator, PsiMethod method) {
            return method != null
                    && (METHOD_CLEAR.equals(method.getName()) || METHOD_DISPOSE.equals(method.getName()))
                    && evaluator.isMemberInClass(method, CLASS_COMPOSITE_DISPOSABLE);
        }

        private static Set<PsiField> getCompositeDisposables(PsiClass clazz) {
            final Set<PsiField> compositeDisposables = new HashSet<>();
            final PsiField[] fields = clazz.getFields();
            for (final PsiField field : fields) {
                if (CLASS_COMPOSITE_DISPOSABLE.equals(field.getType().getCanonicalText())) {
                    compositeDisposables.add(field);
                }
            }
            return compositeDisposables;
        }


        @Override
        public void visitCallExpression(PsiCallExpression call) {
            final PsiMethod method = call.resolveMethod();
            if (method == null) {
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
                if (methodName != null && methodName.endsWith(POSTFIX_METHOD_SUPPRESSION)) {
                    return;
                }
                final boolean isReturningMethodSuppressed = context.getDriver().isSuppressed(context, ISSUE_METHOD_MISSING_CHECK_RESULT, method);
                if (!isReturningMethodSuppressed) {
                    context.report(ISSUE_METHOD_MISSING_CHECK_RESULT,
                                   call,
                                   context.getNameLocation(call),
                                   String.format("The result of `%1$s` is not used.", methodName));
                }
            }
        }

        private static boolean isRxJava2Type(final PsiType psiType) {
            if (psiType == null) {
                return false;
            }
            final String canonicalText = psiType.getCanonicalText();
            return PATTERN_PACKAGE_RX2.matcher(canonicalText).matches()
                    || CLASS_DISPOSABLE.equals(canonicalText)
                    || CLASS_TEST_OBSERVER.equals(canonicalText)
                    || CLASS_TEST_SUBSCRIBER.equals(canonicalText);
        }
    }
}
