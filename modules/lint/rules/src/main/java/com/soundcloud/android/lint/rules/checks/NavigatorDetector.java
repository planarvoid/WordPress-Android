package com.soundcloud.android.lint.rules.checks;

import com.android.tools.lint.client.api.JavaEvaluator;
import com.android.tools.lint.detector.api.Category;
import com.android.tools.lint.detector.api.Detector;
import com.android.tools.lint.detector.api.Implementation;
import com.android.tools.lint.detector.api.Issue;
import com.android.tools.lint.detector.api.JavaContext;
import com.android.tools.lint.detector.api.Scope;
import com.android.tools.lint.detector.api.Severity;
import com.intellij.psi.JavaElementVisitor;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiMethod;
import com.intellij.psi.PsiMethodCallExpression;
import com.intellij.psi.PsiReferenceExpression;

import java.util.Arrays;
import java.util.List;

public final class NavigatorDetector extends Detector implements Detector.JavaPsiScanner {

    public static final Issue ISSUE_START_INTENT = Issue.create("sc.StartIntent",
                                                                "Navigation should be performed through `Navigator`.",
                                                                "To ensure a single point to maintain navigation, and simplify " +
                                                                        "enabling deeplinking, all navigation should be done through `Navigator`. " +
                                                                        "Adding a new navigation target is simple and an example can be found in PR #7918.",
                                                                Category.CORRECTNESS,
                                                                8,
                                                                Severity.WARNING,
                                                                new Implementation(NavigatorDetector.class, Scope.JAVA_FILE_SCOPE));
    private static final String PKG_NAVIGATION = "com.soundcloud.android.navigation";
    private static final String CLASS_CONTEXT = "android.content.Context";
    private static final String CLASS_V4_FRAGMENT = "android.support.v4.app.Fragment";
    private static final String CLASS_FRAGMENT = "android.app.Fragment";

    @Override
    public List<String> getApplicableMethodNames() {
        return Arrays.asList("startActivity", "startActivityForResult");
    }

    @Override
    public void visitMethod(JavaContext context, JavaElementVisitor visitor, PsiMethodCallExpression call, PsiMethod method) {
        final JavaEvaluator evaluator = context.getEvaluator();
        if (!fromContextClass(method, evaluator)) {
            return;
        }
        if (inNavigationPackage(call.getContext(), evaluator)) {
            return;
        }
        final PsiReferenceExpression methodExpression = call.getMethodExpression();
        context.report(ISSUE_START_INTENT,
                       methodExpression,
                       context.getLocation(methodExpression),
                       "Direct navigation should be replaced with `Navigator` call.");
    }

    private static boolean inNavigationPackage(PsiElement file, JavaEvaluator evaluator) {
        return evaluator.getPackage(file).getQualifiedName().startsWith(PKG_NAVIGATION);
    }

    private static boolean fromContextClass(PsiMethod method, JavaEvaluator evaluator) {
        return evaluator.extendsClass(method.getContainingClass(), CLASS_CONTEXT, false) ||
                evaluator.extendsClass(method.getContainingClass(), CLASS_V4_FRAGMENT, false) ||
                evaluator.extendsClass(method.getContainingClass(), CLASS_FRAGMENT, false);
    }
}
