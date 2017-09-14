package com.soundcloud.android.lint.rules.checks;

import static com.android.tools.lint.detector.api.Scope.JAVA_FILE;
import static com.android.tools.lint.detector.api.Scope.TEST_SOURCES;

import com.android.annotations.Nullable;
import com.android.tools.lint.detector.api.Category;
import com.android.tools.lint.detector.api.Detector;
import com.android.tools.lint.detector.api.Implementation;
import com.android.tools.lint.detector.api.Issue;
import com.android.tools.lint.detector.api.JavaContext;
import com.android.tools.lint.detector.api.Severity;
import com.intellij.psi.JavaElementVisitor;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiField;
import com.intellij.psi.PsiIdentifier;
import com.intellij.psi.PsiImportStatement;
import com.intellij.psi.PsiLocalVariable;
import com.intellij.psi.PsiMethod;
import com.intellij.psi.PsiMethodCallExpression;
import com.soundcloud.android.memento.annotation.LintDetector;

import java.util.Arrays;
import java.util.EnumSet;
import java.util.List;

@LintDetector
public final class RxJava1Detector extends Detector implements Detector.JavaPsiScanner {

    public static final Issue ISSUE_RXJAVA_1_USAGE = Issue.create("sc.RxJava1Usage",
                                                                  "Usages of RxJava v1",
                                                                  "Migrate all usages of RxJava v1 to v2 as we want to move away from the old v1.",
                                                                  Category.CORRECTNESS,
                                                                  8,
                                                                  Severity.WARNING,
                                                                  new Implementation(RxJava1Detector.class, EnumSet.of(JAVA_FILE, TEST_SOURCES)));

    @Override
    public List<Class<? extends PsiElement>> getApplicablePsiTypes() {
        return Arrays.asList(
                PsiClass.class,
                PsiImportStatement.class,
                PsiMethodCallExpression.class,
                PsiIdentifier.class,
                PsiMethod.class,
                PsiField.class,
                PsiLocalVariable.class
        );
    }

    @Override
    public JavaElementVisitor createPsiVisitor(JavaContext context) {
        return new RxJava1UsagesVisitor(context);
    }

    private static boolean isRxJava1Class(@Nullable String clazz) {
        return clazz != null && clazz.startsWith("rx.");
    }

    private static void reportRxJava1Issue(JavaContext context, PsiElement element) {
        context.report(ISSUE_RXJAVA_1_USAGE,
                       element,
                       context.getLocation(element),
                       "Migrate to RxJava2. RxJava1 should no longer be used.");
    }


    static class RxJava1UsagesVisitor extends JavaElementVisitor {
        private final JavaContext context;

        RxJava1UsagesVisitor(JavaContext context) {
            this.context = context;
        }

        @Override
        public void visitClass(PsiClass aClass) {
            if (aClass.getSuperClass() != null && isRxJava1Class(aClass.getSuperClass().getQualifiedName())) {
                reportRxJava1Issue(context, aClass);
            }
        }

        @Override
        public void visitField(PsiField field) {
            if (isRxJava1Class(field.getType().getCanonicalText())) {
                reportRxJava1Issue(context, field.getTypeElement());
            }
        }

        @Override
        public void visitMethod(PsiMethod method) {
            if (method.getReturnType() != null && isRxJava1Class(method.getReturnType().getCanonicalText())) {
                reportRxJava1Issue(context, method.getReturnTypeElement());
            }
        }

        @Override
        public void visitImportStatement(PsiImportStatement statement) {
            if (isRxJava1Class(statement.getQualifiedName())) {
                reportRxJava1Issue(context, statement);
            }
        }

        @Override
        public void visitLocalVariable(PsiLocalVariable variable) {
            if (variable != null && variable.getType() != null && isRxJava1Class(variable.getType().getCanonicalText())) {
                reportRxJava1Issue(context, variable.getTypeElement());
            }
        }

        @Override
        public void visitMethodCallExpression(PsiMethodCallExpression expression) {
            if (expression.getMethodExpression().getQualifier() != null && isRxJava1Class(expression.getMethodExpression().getQualifier().getText())) {
                reportRxJava1Issue(context, expression.getMethodExpression().getReferenceNameElement());
            }
        }
    }
}
