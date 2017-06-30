package com.soundcloud.android.lint.rules.checks;

import com.android.annotations.Nullable;
import com.android.tools.lint.detector.api.Category;
import com.android.tools.lint.detector.api.Detector;
import com.android.tools.lint.detector.api.Implementation;
import com.android.tools.lint.detector.api.Issue;
import com.android.tools.lint.detector.api.JavaContext;
import com.android.tools.lint.detector.api.LintUtils;
import com.android.tools.lint.detector.api.Scope;
import com.android.tools.lint.detector.api.Severity;
import com.intellij.psi.JavaElementVisitor;
import com.intellij.psi.PsiCallExpression;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiExpression;
import com.intellij.psi.PsiMethod;
import com.intellij.psi.PsiMethodCallExpression;
import com.intellij.psi.PsiParameter;
import com.intellij.psi.PsiParameterList;
import com.intellij.psi.PsiReferenceExpression;
import com.intellij.psi.util.PsiTreeUtil;
import com.soundcloud.android.memento.annotation.LintDetector;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

@LintDetector
public class EnumDetector extends Detector implements Detector.JavaPsiScanner {

    public static final Issue ISSUE_ENUM_USAGE = Issue.create("sc.EnumUsage",
                                                              "Unsafe Enum marshalling",
                                                              "Enums containing its own label, should implement custom serialization and deserialization " +
                                                                      "methods instead of using `name()` and `valueOf(String)` calls.",
                                                              Category.CORRECTNESS,
                                                              10,
                                                              Severity.ERROR,
                                                              new Implementation(EnumDetector.class, Scope.JAVA_FILE_SCOPE));

    @Override
    public List<Class<? extends PsiElement>> getApplicablePsiTypes() {
        return Collections.singletonList(PsiCallExpression.class);
    }

    @Override
    public JavaElementVisitor createPsiVisitor(final JavaContext context) {
        return new Visitor(context);
    }

    private static final class Visitor extends JavaElementVisitor {

        private final JavaContext context;

        public Visitor(JavaContext context) {
            this.context = context;
        }

        @Override
        public void visitCallExpression(PsiCallExpression expression) {
            if (!(expression instanceof PsiMethodCallExpression)) {
                return;
            }
            final PsiMethodCallExpression methodCallExpression = (PsiMethodCallExpression) expression;
            final String methodName = JavaContext.getMethodName(methodCallExpression);
            if (!isEnumMarshallingMethod(methodName)) {
                return;
            }
            final PsiClass containingClass = getContainingClass(methodCallExpression);
            // Ignore non Enum calls
            if (containingClass == null || !containingClass.isEnum()) {
                return;
            }
            if (!hasConstructorWithString(containingClass)) {
                return;
            }
            final PsiReferenceExpression methodExpression = methodCallExpression.getMethodExpression();
            // Ignore if enum itself is suppressed
            if (!context.getDriver().isSuppressed(context, ISSUE_ENUM_USAGE, containingClass)) {
                context.report(ISSUE_ENUM_USAGE,
                               methodExpression,
                               context.getLocation(methodExpression.getReferenceNameElement()),
                               String.format("Unsafe `%s()` call to enum with label.", methodExpression.getReferenceName()));
            }
        }

        private static boolean isEnumMarshallingMethod(String methodName) {
            return Arrays.asList("name", "valueOf").contains(methodName);
        }

        @Nullable
        private static PsiClass getContainingClass(PsiMethodCallExpression call) {
            final PsiExpression qualifierExpression = call.getMethodExpression().getQualifierExpression();
            if (qualifierExpression instanceof PsiReferenceExpression) {
                return (PsiClass) PsiTreeUtil.findFirstParent(((PsiReferenceExpression) qualifierExpression).resolve(), psiElement -> psiElement instanceof PsiClass);
            }
            return null;
        }

        private static boolean hasConstructorWithString(PsiClass psiClass) {
            for (PsiMethod psiMethod : psiClass.getConstructors()) {
                if (hasStringParameter(psiMethod.getParameterList())) {
                    return true;
                }
            }
            return false;
        }

        private static boolean hasStringParameter(PsiParameterList typeParameters) {
            for (PsiParameter parameter : typeParameters.getParameters()) {
                if (LintUtils.isString(parameter.getType())) {
                    return true;
                }
            }
            return false;
        }
    }
}
