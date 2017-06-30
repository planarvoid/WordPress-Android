package com.soundcloud.android.lint.rules.checks;

import com.android.annotations.Nullable;
import com.android.tools.lint.client.api.JavaEvaluator;
import com.android.tools.lint.detector.api.Category;
import com.android.tools.lint.detector.api.Detector;
import com.android.tools.lint.detector.api.Implementation;
import com.android.tools.lint.detector.api.Issue;
import com.android.tools.lint.detector.api.JavaContext;
import com.android.tools.lint.detector.api.Scope;
import com.android.tools.lint.detector.api.Severity;
import com.google.common.collect.Lists;
import com.intellij.psi.JavaElementVisitor;
import com.intellij.psi.PsiCall;
import com.intellij.psi.PsiClassType;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiExpressionList;
import com.intellij.psi.PsiMethod;
import com.intellij.psi.PsiMethodCallExpression;
import com.intellij.psi.PsiNewExpression;
import com.intellij.psi.PsiType;
import com.soundcloud.android.memento.annotation.LintDetector;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

@LintDetector
public class IntentFactoryDetector extends Detector implements Detector.JavaPsiScanner {
    public static final Issue ISSUE_CREATE_OUTSIDE = Issue.create("sc.CreateIntent",
                                                                  "Intents should not be created directly.",
                                                                  "Intents should be created either through a factory method in the target class or" +
                                                                          "through the `(Pending)IntentFactory`.",
                                                                  Category.CORRECTNESS,
                                                                  2,
                                                                  Severity.WARNING,
                                                                  new Implementation(IntentFactoryDetector.class, Scope.JAVA_FILE_SCOPE));

    private static final String CLASS_CONTEXT = "android.content.Context";
    private static final String CLASS_ACTIVITY = "android.app.Activity";
    private static final String CLASS_CLASS = "java.lang.Class";
    private static final String CLASS_INTENT = "android.content.Intent";
    private static final String CLASS_INTENT_FACTORY = "com.soundcloud.android.navigation.IntentFactory";
    private static final String CLASS_PENDING_INTENT_FACTORY = "com.soundcloud.android.navigation.PendingIntentFactory";

    @Override
    public List<String> getApplicableConstructorTypes() {
        return Collections.singletonList(CLASS_INTENT);
    }

    @Override
    public void visitConstructor(JavaContext context, JavaElementVisitor visitor, PsiNewExpression node, PsiMethod constructor) {
        final JavaEvaluator evaluator = context.getEvaluator();
        final List<PsiClassType> parameterClasses = getParameterClassNames(node);
        if (hasContextParameter(evaluator, parameterClasses) && hasActivityClassParameter(evaluator, parameterClasses)) {
            final PsiMethod containingMethod = findContainingMethod(node);
            if (isInFactoryMethod(evaluator, containingMethod) && isInTargetClass(evaluator, containingMethod, parameterClasses)) {
                return;
            }
            if (isInValidFactory(evaluator, containingMethod)) {
                return;
            }
            context.report(ISSUE_CREATE_OUTSIDE, node, context.getLocation(node), "Intent's should be created in the `(Pending)IntentFactory`.");
        }
    }

    @Override
    public List<String> getApplicableMethodNames() {
        return Arrays.asList("setClass", "setClassName", "setComponent");
    }

    @Override
    public void visitMethod(JavaContext context, JavaElementVisitor visitor, PsiMethodCallExpression node, PsiMethod method) {
        final JavaEvaluator evaluator = context.getEvaluator();
        if (isFromIntent(evaluator, method)) {
            final PsiMethod containingMethod = findContainingMethod(node);
            final List<PsiClassType> parameterClasses = getParameterClassNames(node);
            if ("setClass".equals(method.getName())) {
                if (!hasContextParameter(evaluator, parameterClasses) || !hasActivityClassParameter(evaluator, parameterClasses)) {
                    return;
                }
                if (isInFactoryMethod(evaluator, containingMethod) && isInTargetClass(evaluator, containingMethod, parameterClasses)) {
                    return;
                }
            } else {
                if (isInFactoryMethod(evaluator, containingMethod)) {
                    return;
                }
            }
            if (isInValidFactory(evaluator, containingMethod)) {
                return;
            }
            context.report(ISSUE_CREATE_OUTSIDE, node, context.getLocation(node), "Intent's should be created in the `(Pending)IntentFactory`.");
        }
    }

    private static boolean isFromIntent(JavaEvaluator evaluator, PsiMethod method) {
        return evaluator.isMemberInClass(method, CLASS_INTENT);
    }

    private static boolean isInFactoryMethod(JavaEvaluator evaluator, PsiMethod method) {
        return evaluator.isStatic(method) && evaluator.typeMatches(method.getReturnType(), CLASS_INTENT);
    }

    private static boolean isInValidFactory(JavaEvaluator evaluator, PsiMethod method) {
        return evaluator.isMemberInSubClassOf(method, CLASS_INTENT_FACTORY, false) || evaluator.isMemberInSubClassOf(method, CLASS_PENDING_INTENT_FACTORY, false);
    }

    private static boolean isInTargetClass(JavaEvaluator evaluator, PsiMethod method, List<PsiClassType> parameterClasses) {
        final PsiClassType classParameterType = getClassParameterType(evaluator, parameterClasses);
        return classParameterType != null && evaluator.isMemberInClass(method, classParameterType.getCanonicalText());
    }

    private static boolean hasActivityClassParameter(JavaEvaluator evaluator, List<PsiClassType> parameterClasses) {
        final PsiClassType classParameterType = getClassParameterType(evaluator, parameterClasses);
        return classParameterType != null && evaluator.extendsClass(classParameterType.resolve(), CLASS_ACTIVITY, true);
    }

    @Nullable
    private static PsiClassType getClassParameterType(JavaEvaluator evaluator, List<PsiClassType> parameterClasses) {
        for (PsiClassType parameterClass : parameterClasses) {
            if (!evaluator.extendsClass(parameterClass.resolve(), CLASS_CLASS, false)) {
                continue;
            }
            if (parameterClass.getParameterCount() != 1) {
                continue;
            }
            final PsiType typeParameter = parameterClass.getParameters()[0];
            if (!(typeParameter instanceof PsiClassType)) {
                continue;
            }
            return (PsiClassType) typeParameter;
        }
        return null;
    }

    private static PsiMethod findContainingMethod(PsiElement node) {
        PsiElement parent = node;
        while ((parent = parent.getParent()) != null) {
            if (parent instanceof PsiMethod) {
                return (PsiMethod) parent;
            }
        }
        return null;
    }

    private static boolean hasContextParameter(JavaEvaluator evaluator, List<PsiClassType> parameterClasses) {
        for (PsiClassType parameterClass : parameterClasses) {
            if (evaluator.extendsClass(parameterClass.resolve(), CLASS_CONTEXT, false)) {
                return true;
            }
        }
        return false;
    }

    private static List<PsiClassType> getParameterClassNames(PsiCall method) {
        final List<PsiClassType> parameters = Lists.newArrayList();
        final PsiExpressionList parameterList = method.getArgumentList();
        if (parameterList != null) {
            for (PsiType type : parameterList.getExpressionTypes()) {
                parameters.add((PsiClassType) type);
            }
        }
        return parameters;
    }
}
