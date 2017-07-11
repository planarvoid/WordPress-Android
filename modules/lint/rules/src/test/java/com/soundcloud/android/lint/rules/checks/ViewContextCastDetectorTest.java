package com.soundcloud.android.lint.rules.checks;

import com.android.tools.lint.checks.infrastructure.LintDetectorTest;
import com.android.tools.lint.detector.api.Detector;
import com.android.tools.lint.detector.api.Issue;
import org.intellij.lang.annotations.Language;

import java.util.Collections;
import java.util.List;

public class ViewContextCastDetectorTest extends LintDetectorTest {

    @Override
    protected Detector getDetector() {
        return new ViewContextCastDetector();
    }

    @Override
    protected List<Issue> getIssues() {
        return Collections.singletonList(ViewContextCastDetector.ISSUE_VIEW_CONTEXT_CAST);
    }

    public void testReadContextWithoutCastShouldNotReportAnIssue() throws Exception {
        @Language("JAVA") String source = "package foo;\n" +
                "\n" +
                "import android.app.Activity;\n" +
                "import android.content.Context;\n" +
                "import android.widget.TextView;\n" +
                "import android.view.View;\n" +
                "\n" +
                "public class MyActivity extends Activity {\n" +
                "  public void start() {\n" +
                "    View view = new TextView(this);\n" +
                "    Context context = view.getContext();\n" +
                "  }\n" +
                "}";

        lint().files(java(source))
              .run()
              .expectClean();
    }

    public void testPassContextWithoutCastShouldNotReportAnIssue() throws Exception {
        @Language("JAVA") String source = "package foo;\n" +
                "\n" +
                "import android.app.Activity;\n" +
                "import android.content.Context;\n" +
                "import android.widget.TextView;\n" +
                "import android.view.View;\n" +
                "\n" +
                "public class MyActivity extends Activity {\n" +
                "  public void start() {\n" +
                "    View view = new TextView(this);\n" +
                "    foo(view.getContext());\n" +
                "  }\n\n" +
                "  public void foo(Context c) {}\n" +
                "}";

        lint().files(java(source))
              .run()
              .expectClean();
    }

    public void testCastContextToActivityShouldReportAnIssue() throws Exception {
        @Language("JAVA") String source = "package foo;\n" +
                "\n" +
                "import android.app.Activity;\n" +
                "import android.content.Context;\n" +
                "import android.widget.TextView;\n" +
                "import android.view.View;\n" +
                "\n" +
                "public class MyActivity extends Activity {\n" +
                "  public void start() {\n" +
                "    TextView view = new TextView(this);\n" +
                "    Activity activity = (Activity) view.getContext();\n" +
                "  }\n" +
                "}";

        lint().files(java(source))
              .run()
              .expect("src/foo/MyActivity.java:11: " +
                              "Warning: Unsafe cast from View's context to Activity. [sc.ViewContextCast]\n" +
                              "    Activity activity = (Activity) view.getContext();\n" +
                              "                                   ~~~~~~~~~~~~~~~\n" +
                              "0 errors, 1 warnings\n");
    }

    public void testCastContextToActivityInMethodCallShouldReportAnIssue() throws Exception {
        @Language("JAVA") String source = "package foo;\n" +
                "\n" +
                "import android.app.Activity;\n" +
                "import android.content.Context;\n" +
                "import android.widget.TextView;\n" +
                "import android.view.View;\n" +
                "\n" +
                "public class MyActivity extends Activity {\n" +
                "  public void start() {\n" +
                "    View view = new TextView(this);\n" +
                "    foo((Activity) view.getContext());\n" +
                "  }\n\n" +
                "  public void foo(Activity a) {}\n" +
                "}";

        lint().files(java(source))
              .run()
              .expect("src/foo/MyActivity.java:11: " +
                              "Warning: Unsafe cast from View's context to Activity. [sc.ViewContextCast]\n" +
                              "    foo((Activity) view.getContext());\n" +
                              "                   ~~~~~~~~~~~~~~~\n" +
                              "0 errors, 1 warnings\n");
    }

    public void testCastContextToActivitySubclassShouldReportAnIssue() throws Exception {

        @Language("JAVA") String source = "package foo;\n" +
                "\n" +
                "import android.app.Activity;\n" +
                "import android.content.Context;\n" +
                "import android.widget.TextView;\n" +
                "import android.view.View;\n" +
                "\n" +
                "public class MyActivity extends Activity {\n" +
                "  public void start() {\n" +
                "    TextView view = new TextView(this);\n" +
                "    AnotherActivity activity = (AnotherActivity) view.getContext();\n" +
                "  }\n" +
                "}";

        lint().files(anotherActivity(), java(source))
              .run()
              .expect("src/foo/MyActivity.java:11: " +
                              "Warning: Unsafe cast from View's context to Activity. [sc.ViewContextCast]\n" +
                              "    AnotherActivity activity = (AnotherActivity) view.getContext();\n" +
                              "                                                 ~~~~~~~~~~~~~~~\n" +
                              "0 errors, 1 warnings\n");
    }

    public void testCastContextToActivitySubClassInMethodCallShouldReportAnIssue() throws Exception {

        @Language("JAVA") String source = "package foo;\n" +
                "\n" +
                "import android.app.Activity;\n" +
                "import android.content.Context;\n" +
                "import android.widget.TextView;\n" +
                "import android.view.View;\n" +
                "\n" +
                "public class MyActivity extends Activity {\n" +
                "  public void start() {\n" +
                "    View view = new TextView(this);\n" +
                "    foo((AnotherActivity) view.getContext());\n" +
                "  }\n\n" +
                "  public void foo(AnotherActivity a) {}\n" +
                "}";

        lint().files(anotherActivity(), java(source))
              .run()
              .expect("src/foo/MyActivity.java:11: " +
                              "Warning: Unsafe cast from View's context to Activity. [sc.ViewContextCast]\n" +
                              "    foo((AnotherActivity) view.getContext());\n" +
                              "                          ~~~~~~~~~~~~~~~\n" +
                              "0 errors, 1 warnings\n");
    }

    private TestFile anotherActivity() {
        @Language("JAVA") String source = "package foo;\n" +
                "\n" +
                "import android.app.Activity;\n" +
                "\n" +
                "public class AnotherActivity extends Activity {" +
                "}";
        return java(source);
    }

}
