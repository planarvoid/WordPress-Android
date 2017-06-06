package com.soundcloud.android.lint.rules.checks;

import static org.assertj.core.api.Assertions.assertThat;

import com.android.tools.lint.checks.infrastructure.LintDetectorTest;
import com.android.tools.lint.detector.api.Detector;
import com.android.tools.lint.detector.api.Issue;
import org.intellij.lang.annotations.Language;

import java.util.Collections;
import java.util.List;

public class NavigatorDetectorTest extends LintDetectorTest {

    private static final String CLASS_SAMPLE = "foo.SampleContext";
    private static final String CLASS_ACTIVITY = "android.app.Activity";
    private static final String CLASS_V4_ACTIVITY = "android.support.v4.app.FragmentActivity";
    private static final String CLASS_FRAGMENT = "android.app.Fragment";
    private static final String CLASS_V4_FRAGMENT = "android.support.v4.app.Fragment";

    @Override
    protected Detector getDetector() {
        return new NavigatorDetector();
    }

    @Override
    protected List<Issue> getIssues() {
        return Collections.singletonList(NavigatorDetector.ISSUE_START_INTENT);
    }

    // Activity

    public void test_Activity_startActivity() throws Exception {
        @Language("JAVA") String source = exampleStartActivity(CLASS_ACTIVITY);
        assertThat(lintProject(java(source))).isEqualTo("src/foo/Example.java:8: "
                                                                + "Warning: Direct navigation should be replaced with Navigator call. [sc.StartIntent]\n"
                                                                + "    context.startActivity(new Intent());\n"
                                                                + "    ~~~~~~~~~~~~~~~~~~~~~\n"
                                                                + "0 errors, 1 warnings\n");
    }

    public void test_Activity_startActivityForResult() throws Exception {
        @Language("JAVA") String source = exampleStartActivityForResult(CLASS_ACTIVITY);
        assertThat(lintProject(java(source))).isEqualTo("src/foo/Example.java:8: "
                                                                + "Warning: Direct navigation should be replaced with Navigator call. [sc.StartIntent]\n"
                                                                + "    context.startActivityForResult(new Intent(), 0);\n"
                                                                + "    ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~\n"
                                                                + "0 errors, 1 warnings\n");
    }

    // FragmentActivity

    public void test_FragmentActivity_startActivity() throws Exception {
        @Language("JAVA") String source = exampleStartActivity(CLASS_V4_ACTIVITY);
        assertThat(lintProject(java(source), java(fakeSupportFragmentActivity()))).isEqualTo("src/foo/Example.java:8: "
                                                                + "Warning: Direct navigation should be replaced with Navigator call. [sc.StartIntent]\n"
                                                                + "    context.startActivity(new Intent());\n"
                                                                + "    ~~~~~~~~~~~~~~~~~~~~~\n"
                                                                + "0 errors, 1 warnings\n");
    }

    public void test_FragmentActivity_startActivityForResult() throws Exception {
        @Language("JAVA") String source = exampleStartActivityForResult(CLASS_V4_ACTIVITY);
        assertThat(lintProject(java(source), java(fakeSupportFragmentActivity()))).isEqualTo("src/foo/Example.java:8: "
                                                                + "Warning: Direct navigation should be replaced with Navigator call. [sc.StartIntent]\n"
                                                                + "    context.startActivityForResult(new Intent(), 0);\n"
                                                                + "    ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~\n"
                                                                + "0 errors, 1 warnings\n");
    }

    // Fragment

    public void test_Fragment_startActivity() throws Exception {
        @Language("JAVA") String source = exampleStartActivity(CLASS_FRAGMENT);
        assertThat(lintProject(java(source))).isEqualTo("src/foo/Example.java:8: "
                                                                + "Warning: Direct navigation should be replaced with Navigator call. [sc.StartIntent]\n"
                                                                + "    context.startActivity(new Intent());\n"
                                                                + "    ~~~~~~~~~~~~~~~~~~~~~\n"
                                                                + "0 errors, 1 warnings\n");
    }

    public void test_Fragment_startActivityForResult() throws Exception {
        @Language("JAVA") String source = exampleStartActivityForResult(CLASS_FRAGMENT);
        assertThat(lintProject(java(source))).isEqualTo("src/foo/Example.java:8: "
                                                                + "Warning: Direct navigation should be replaced with Navigator call. [sc.StartIntent]\n"
                                                                + "    context.startActivityForResult(new Intent(), 0);\n"
                                                                + "    ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~\n"
                                                                + "0 errors, 1 warnings\n");
    }

    // Support Fragment

    public void test_SupportFragment_startActivity() throws Exception {
        @Language("JAVA") String source = exampleStartActivity(CLASS_V4_FRAGMENT);
        assertThat(lintProject(java(source), java(fakeSupportFragment()))).isEqualTo("src/foo/Example.java:8: "
                                                                + "Warning: Direct navigation should be replaced with Navigator call. [sc.StartIntent]\n"
                                                                + "    context.startActivity(new Intent());\n"
                                                                + "    ~~~~~~~~~~~~~~~~~~~~~\n"
                                                                + "0 errors, 1 warnings\n");
    }

    public void test_SupportFragment_startActivityForResult() throws Exception {
        @Language("JAVA") String source = exampleStartActivityForResult(CLASS_V4_FRAGMENT);
        assertThat(lintProject(java(source), java(fakeSupportFragment()))).isEqualTo("src/foo/Example.java:8: "
                                                                + "Warning: Direct navigation should be replaced with Navigator call. [sc.StartIntent]\n"
                                                                + "    context.startActivityForResult(new Intent(), 0);\n"
                                                                + "    ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~\n"
                                                                + "0 errors, 1 warnings\n");
    }

    // Sample Context

    public void test_SampleContext_startActivity() throws Exception {
        @Language("JAVA") String source = exampleStartActivity(CLASS_SAMPLE);
        assertThat(lintProject(java(source), java(sampleContext()))).isEqualTo("No warnings.");
    }

    public void test_SampleContext_startActivityForResult() throws Exception {
        @Language("JAVA") String source = exampleStartActivityForResult(CLASS_SAMPLE);
        assertThat(lintProject(java(source), java(sampleContext()))).isEqualTo("No warnings.");
    }

    // Navigator

    public void test_Navigator_Activty_startActivity() throws Exception {
        @Language("JAVA") String source = navigatorStartActivity(CLASS_ACTIVITY);
        assertThat(lintProject(java(source))).isEqualTo("No warnings.");
    }

    public void test_Navigator_Activty_startActivityForResult() throws Exception {
        @Language("JAVA") String source = navigatorStartActivityForResult(CLASS_ACTIVITY);
        assertThat(lintProject(java(source))).isEqualTo("No warnings.");
    }

    public void test_Navigator_FragmentActivity_startActivity() throws Exception {
        @Language("JAVA") String source = navigatorStartActivity(CLASS_V4_ACTIVITY);
        assertThat(lintProject(java(source), java(fakeSupportFragmentActivity()))).isEqualTo("No warnings.");
    }

    public void test_Navigator_FragmentActivity_startActivityForResult() throws Exception {
        @Language("JAVA") String source = navigatorStartActivityForResult(CLASS_V4_ACTIVITY);
        assertThat(lintProject(java(source), java(fakeSupportFragmentActivity()))).isEqualTo("No warnings.");
    }

    public void test_Navigator_Fragment_startActivity() throws Exception {
        @Language("JAVA") String source = navigatorStartActivity(CLASS_FRAGMENT);
        assertThat(lintProject(java(source))).isEqualTo("No warnings.");
    }

    public void test_Navigator_Fragment_startActivityForResult() throws Exception {
        @Language("JAVA") String source = navigatorStartActivityForResult(CLASS_FRAGMENT);
        assertThat(lintProject(java(source))).isEqualTo("No warnings.");
    }

    public void test_Navigator_SupportFragment_startActivity() throws Exception {
        @Language("JAVA") String source = navigatorStartActivity(CLASS_V4_FRAGMENT);
        assertThat(lintProject(java(source), java(fakeSupportFragment()))).isEqualTo("No warnings.");
    }

    public void test_Navigator_SupportFragment_startActivityForResult() throws Exception {
        @Language("JAVA") String source = navigatorStartActivityForResult(CLASS_V4_FRAGMENT);
        assertThat(lintProject(java(source), java(fakeSupportFragment()))).isEqualTo("No warnings.");
    }

    // Utils

    @Language("JAVA")
    private static String exampleStartActivity(String clazz) {
        return "package foo;\n" +
                "\n" +
                "import android.content.Intent;\n" +
                "\n" +
                "public class Example {\n" +
                "  public void start() {\n" +
                "    " + clazz + " context = new " + clazz + "();\n" +
                "    context.startActivity(new Intent());\n" +
                "  }\n" +
                "}";
    }

    @Language("JAVA")
    private static String exampleStartActivityForResult(String clazz) {
        return "package foo;\n" +
                "\n" +
                "import android.content.Intent;\n" +
                "\n" +
                "public class Example {\n" +
                "  public void start() {\n" +
                "    " + clazz + " context = new " + clazz + "();\n" +
                "    context.startActivityForResult(new Intent(), 0);\n" +
                "  }\n" +
                "}";
    }

    @Language("JAVA")
    private static String navigatorStartActivity(String clazz) {
        return "package com.soundcloud.android.navigation;\n" +
                "\n" +
                "import android.content.Intent;\n" +
                "\n" +
                "public class Example {\n" +
                "  public void start() {\n" +
                "    " + clazz + " context = new " + clazz + "();\n" +
                "    context.startActivity(new Intent());\n" +
                "  }\n" +
                "}";
    }

    @Language("JAVA")
    private static String navigatorStartActivityForResult(String clazz) {
        return "package com.soundcloud.android.navigation;\n" +
                "\n" +
                "import android.content.Intent;\n" +
                "\n" +
                "public class Example {\n" +
                "  public void start() {\n" +
                "    " + clazz + " context = new " + clazz + "();\n" +
                "    context.startActivityForResult(new Intent(), 0);\n" +
                "  }\n" +
                "}";
    }

    @Language("JAVA")
    private static String sampleContext() {
        return "package foo;\n" +
                "\n" +
                "import android.content.Intent;\n" +
                "import android.os.Bundle;\n" +
                "\n" +
                "public class SampleContext {\n" +
                "    public void startActivity(Intent intent) {}\n" +
                "    public void startActivity(Intent intent, Bundle options) {}\n" +
                "    public void startActivityForResult(Intent intent, int requestCode) {}\n" +
                "    public void startActivityForResult(Intent intent, int requestCode, Bundle options) {}    \n" +
                "}";
    }

    @Language("JAVA")
    private static String fakeSupportFragment() {
        return "package android.support.v4.app;\n" +
                "\n" +
                "import android.content.Intent;\n" +
                "import android.os.Bundle;\n" +
                "\n" +
                "public class Fragment {\n" +
                "    public void startActivity(Intent intent) {}\n" +
                "    public void startActivity(Intent intent, Bundle options) {}\n" +
                "    public void startActivityForResult(Intent intent, int requestCode) {}\n" +
                "    public void startActivityForResult(Intent intent, int requestCode, Bundle options) {}    \n" +
                "}";
    }

    @Language("JAVA")
    private static String fakeSupportFragmentActivity() {
        return "package android.support.v4.app;\n" +
                "\n" +
                "import android.app.Activity;\n" +
                "\n" +
                "public class FragmentActivity extends Activity {\n" +
                "}";
    }
}
