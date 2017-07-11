package com.soundcloud.android.lint.rules.checks;

import com.android.tools.lint.client.api.JavaEvaluator;
import com.android.tools.lint.detector.api.Category;
import com.android.tools.lint.detector.api.Detector;
import com.android.tools.lint.detector.api.Detector.JavaPsiScanner;
import com.android.tools.lint.detector.api.Implementation;
import com.android.tools.lint.detector.api.Issue;
import com.android.tools.lint.detector.api.JavaContext;
import com.android.tools.lint.detector.api.Scope;
import com.android.tools.lint.detector.api.Severity;
import com.intellij.psi.JavaElementVisitor;
import com.intellij.psi.PsiClassType;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiMethod;
import com.intellij.psi.PsiMethodCallExpression;
import com.intellij.psi.PsiTypeCastExpression;
import com.intellij.psi.PsiTypeElement;
import com.soundcloud.android.memento.annotation.LintDetector;

import java.util.Collections;
import java.util.List;

@LintDetector
public class ViewContextCastDetector extends Detector implements JavaPsiScanner {

    private static final Implementation IMPLEMENTATION = new Implementation(ViewContextCastDetector.class, Scope.JAVA_FILE_SCOPE);

    public static final Issue ISSUE_VIEW_CONTEXT_CAST = Issue.create("sc.ViewContextCast",
                                                                     "You should not cast View's Context to Activity.",
                                                                     "Casting the View context to Activity might end up in a ClassCastException." +
                                                                             "Use ViewUtils.getFragmentActivity() to obtain the Activity the View is attached to.",
                                                                     Category.CORRECTNESS,
                                                                     6,
                                                                     Severity.WARNING,
                                                                     IMPLEMENTATION);

    private static final String CLASS_ANDROID_VIEW = "android.view.View";
    private static final String CLASS_ANDROID_ACTIVITY = "android.app.Activity";

    @Override
    public List<String> getApplicableMethodNames() {
        return Collections.singletonList("getContext");
    }

    @Override
    public void visitMethod(JavaContext context, JavaElementVisitor visitor, PsiMethodCallExpression call, PsiMethod method) {
        final JavaEvaluator evaluator = context.getEvaluator();
        if (isInViewClass(evaluator, method) && isCastingToActivity(evaluator, call)) {
            final PsiElement parentExpression = call.getMethodExpression();
            context.report(ISSUE_VIEW_CONTEXT_CAST,
                           parentExpression,
                           context.getLocation(parentExpression),
                           "Unsafe cast from View's context to Activity.");
        }
    }

    private static boolean isInViewClass(JavaEvaluator evaluator, PsiMethod method) {
        return evaluator.isMemberInSubClassOf(method, CLASS_ANDROID_VIEW, false);
    }

    private static boolean isCastingToActivity(JavaEvaluator evaluator, PsiMethodCallExpression call) {
        PsiTypeCastExpression typeCastExpression = findTypeCastExpression(call);

        if (typeCastExpression != null) {
            final PsiTypeElement castType = typeCastExpression.getCastType();
            if (castType != null) {
                final PsiClassType classType = (PsiClassType) castType.getType();
                if (evaluator.extendsClass(classType.resolve(), CLASS_ANDROID_ACTIVITY, false)) {
                    return true;
                }
            }
        }

        return false;
    }

    private static PsiTypeCastExpression findTypeCastExpression(PsiMethodCallExpression call) {
        PsiElement parent = call;

        while ((parent = parent.getParent()) != null) {
            if (parent instanceof PsiTypeCastExpression) {
                return (PsiTypeCastExpression) parent;
            }
        }

        return null;
    }
}
