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
import com.intellij.psi.PsiMethod;
import com.intellij.psi.PsiMethodCallExpression;
import com.intellij.psi.util.PsiTreeUtil;
import com.soundcloud.android.memento.annotation.LintDetector;

import java.util.Arrays;
import java.util.List;

@LintDetector
public final class LogDetector extends Detector implements Detector.JavaPsiScanner {

    public static final Issue ISSUE_ANDROID_LOG = Issue.create("sc.AndroidLog",
                                                                "Logging should be performed through `com.soundcloud.android.utils.Log`.",
                                                                "To ensure a unified logging experience the Android logger should not be used.",
                                                                Category.CORRECTNESS,
                                                                5,
                                                                Severity.WARNING,
                                                                new Implementation(LogDetector.class, Scope.JAVA_FILE_SCOPE));
    private static final String CLASS_ANDROID_LOG = "android.util.Log";
    private static final String CLASS_SOUNDCLOUD_LOG = "com.soundcloud.android.utils.Log";

    @Override
    public List<String> getApplicableMethodNames() {
        return Arrays.asList(
                "d",
                "e",
                "i",
                "v",
                "w",
                "wtf");
    }
    @Override
    public void visitMethod(JavaContext context, JavaElementVisitor visitor, PsiMethodCallExpression call, PsiMethod method) {
        JavaEvaluator evaluator = context.getEvaluator();
        if (!evaluator.isMemberInClass(method, CLASS_ANDROID_LOG)) {
            return;
        }

        PsiMethod parentMethod = PsiTreeUtil.getParentOfType(call, PsiMethod.class);
        if (evaluator.isMemberInClass(parentMethod, CLASS_SOUNDCLOUD_LOG)) {
            return;
        }

        context.report(ISSUE_ANDROID_LOG,
                       call,
                       context.getLocation(call),
                       "Android Log usage detected.");
    }
}
