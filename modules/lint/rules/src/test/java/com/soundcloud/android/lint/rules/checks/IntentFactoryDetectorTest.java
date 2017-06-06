package com.soundcloud.android.lint.rules.checks;

import static org.assertj.core.api.Assertions.assertThat;

import com.android.tools.lint.checks.infrastructure.LintDetectorTest;
import com.android.tools.lint.detector.api.Detector;
import com.android.tools.lint.detector.api.Issue;
import org.intellij.lang.annotations.Language;

import java.util.Collections;
import java.util.List;

public class IntentFactoryDetectorTest extends LintDetectorTest {

    // Constructor

    public void test_Constructor_InIntentFactory() throws Exception {
        @Language("JAVA") final String source = "package com.soundcloud.android.navigation;\n" +
                "\n" +
                "import android.content.Intent;\n" +
                "import foo.BarActivity;\n" +
                "import android.content.Context;\n" +
                "\n" +
                "public class IntentFactory {\n" +
                "    public static Intent create(Context context) {\n" +
                "        return new Intent(context, BarActivity.class);\n" +
                "    }\n" +
                "}";
        assertThat(lintProject(java(source), java(otherSample()))).isEqualTo("No warnings.");
    }

    public void test_Constructor_InPendingIntentFactory() throws Exception {
        @Language("JAVA") final String source = "package com.soundcloud.android.navigation;\n" +
                "\n" +
                "import android.content.Intent;\n" +
                "import foo.BarActivity;\n" +
                "import android.content.Context;\n" +
                "\n" +
                "public class PendingIntentFactory {\n" +
                "    public static Intent create(Context context) {\n" +
                "        return new Intent(context, BarActivity.class);\n" +
                "    }\n" +
                "}";
        assertThat(lintProject(java(source), java(otherSample()))).isEqualTo("No warnings.");
    }

    public void test_Constructor_StaticFactoryMethodInClass() throws Exception {
        @Language("JAVA") final String source = "package foo;\n" +
                "\n" +
                "import android.content.Intent;\n" +
                "import android.app.Activity;\n" +
                "import android.content.Context;\n" +
                "\n" +
                "public class SampleActivity extends Activity {\n" +
                "    public static Intent create(Context context) {\n" +
                "        return new Intent(context, SampleActivity.class);\n" +
                "    }\n" +
                "}";
        assertThat(lintProject(java(source))).isEqualTo("No warnings.");
    }

    public void test_Constructor_StaticFactoryMethodInDifferentClass() throws Exception {
        @Language("JAVA") final String source = "package foo;\n" +
                "\n" +
                "import android.content.Intent;\n" +
                "import android.app.Activity;\n" +
                "import foo.BarActivity;\n" +
                "import android.content.Context;\n" +
                "\n" +
                "public class SampleActivity extends Activity {\n" +
                "    public static Intent create(Context context) {\n" +
                "        return new Intent(context, BarActivity.class);\n" +
                "    }\n" +
                "}";
        assertThat(lintProject(java(source), java(otherSample()))).isEqualTo("src/foo/SampleActivity.java:10: "
                                                                                     + "Warning: Intent's should be created in the (Pending)IntentFactory. [sc.CreateIntent]\n"
                                                                                     + "        return new Intent(context, BarActivity.class);\n"
                                                                                     + "               ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~\n"
                                                                                     + "0 errors, 1 warnings\n");
    }

    public void test_Constructor_StaticFactoryMethodInDifferentClassWithActivity() throws Exception {
        @Language("JAVA") final String source = "package foo;\n" +
                "\n" +
                "import android.content.Intent;\n" +
                "import android.app.Activity;\n" +
                "import foo.BarActivity;\n" +
                "\n" +
                "public class SampleActivity extends Activity {\n" +
                "    public static Intent create(Activity activity) {\n" +
                "        return new Intent(activity, BarActivity.class);\n" +
                "    }\n" +
                "}";
        assertThat(lintProject(java(source), java(otherSample()))).isEqualTo("src/foo/SampleActivity.java:9: "
                                                                                     + "Warning: Intent's should be created in the (Pending)IntentFactory. [sc.CreateIntent]\n"
                                                                                     + "        return new Intent(activity, BarActivity.class);\n"
                                                                                     + "               ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~\n"
                                                                                     + "0 errors, 1 warnings\n");
    }

    public void test_Constructor_StaticFactoryMethodInDifferentClassIntegerClass() throws Exception {
        @Language("JAVA") final String source = "package foo;\n" +
                "\n" +
                "import android.content.Intent;\n" +
                "import android.app.Activity;\n" +
                "\n" +
                "public class SampleActivity extends Activity {\n" +
                "    public static Intent create(Activity activity) {\n" +
                "        return new Intent(activity, Integer.class);\n" +
                "    }\n" +
                "}";
        assertThat(lintProject(java(source), java(otherSample()))).isEqualTo("No warnings.");
    }

    public void test_Constructor_StartInClass() throws Exception {
        @Language("JAVA") final String source = "package foo;\n" +
                "\n" +
                "import android.app.Activity;\n" +
                "import android.content.Context;\n" +
                "import android.content.Intent;\n" +
                "\n" +
                "public class SampleActivity extends Activity {\n" +
                "    public static void start(Context context) {\n" +
                "        context.startActivity(new Intent(context, SampleActivity.class));\n" +
                "    }\n" +
                "}";
        assertThat(lintProject(java(source), java(otherSample()))).isEqualTo("src/foo/SampleActivity.java:9: "
                                                                                     + "Warning: Intent's should be created in the (Pending)IntentFactory. [sc.CreateIntent]\n"
                                                                                     + "        context.startActivity(new Intent(context, SampleActivity.class));\n"
                                                                                     + "                              ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~\n"
                                                                                     + "0 errors, 1 warnings\n");
    }

    public void test_Constructor_StaticFactoryInvalid() throws Exception {
        @Language("JAVA") final String source = "package foo;\n" +
                "\n" +
                "import android.content.Intent;\n" +
                "import foo.BarActivity;\n" +
                "import android.content.Context;\n" +
                "\n" +
                "public class InvalidFactory {\n" +
                "    public static Intent create(Context context) {\n" +
                "        return new Intent(context, BarActivity.class);\n" +
                "    }\n" +
                "}";
        assertThat(lintProject(java(source), java(otherSample()))).isEqualTo("src/foo/InvalidFactory.java:9: "
                                                                                     + "Warning: Intent's should be created in the (Pending)IntentFactory. [sc.CreateIntent]\n"
                                                                                     + "        return new Intent(context, BarActivity.class);\n"
                                                                                     + "               ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~\n"
                                                                                     + "0 errors, 1 warnings\n");
    }

    public void test_Constructor_InvalidCreation() throws Exception {
        @Language("JAVA") final String source = "package foo;\n" +
                "\n" +
                "import android.content.Intent;\n" +
                "import foo.BarActivity;\n" +
                "import android.content.Context;\n" +
                "\n" +
                "public class Example {\n" +
                "    public Intent create(Context context) {\n" +
                "        return new Intent(context, BarActivity.class);\n" +
                "    }\n" +
                "}";
        assertThat(lintProject(java(source), java(otherSample()))).isEqualTo("src/foo/Example.java:9: "
                                                                                     + "Warning: Intent's should be created in the (Pending)IntentFactory. [sc.CreateIntent]\n"
                                                                                     + "        return new Intent(context, BarActivity.class);\n"
                                                                                     + "               ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~\n"
                                                                                     + "0 errors, 1 warnings\n");
    }

    public void test_Constructor_PureIntent() throws Exception {
        @Language("JAVA") final String source = "package foo;\n" +
                "\n" +
                "import android.content.Intent;\n" +
                "\n" +
                "public class Example {\n" +
                "    public Intent create() {\n" +
                "        return new Intent();\n" +
                "    }\n" +
                "}";
        assertThat(lintProject(java(source))).isEqualTo("No warnings.");
    }

    // Methods

    public void test_Method_IntentSetClass() throws Exception {
        @Language("JAVA") final String source = "package foo;\n" +
                "\n" +
                "import android.content.Intent;\n" +
                "import foo.BarActivity;\n" +
                "import android.content.Context;\n" +
                "\n" +
                "public class Example {\n" +
                "    public Intent create(Context context) {\n" +
                "        return new Intent().setClass(context, BarActivity.class);\n" +
                "    }\n" +
                "}";
        assertThat(lintProject(java(source), java(otherSample()))).isEqualTo("src/foo/Example.java:9: "
                                                                                     + "Warning: Intent's should be created in the (Pending)IntentFactory. [sc.CreateIntent]\n"
                                                                                     + "        return new Intent().setClass(context, BarActivity.class);\n"
                                                                                     + "               ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~\n"
                                                                                     + "0 errors, 1 warnings\n");
    }

    public void test_Method_IntentSetClassNameContext() throws Exception {
        @Language("JAVA") final String source = "package foo;\n" +
                "\n" +
                "import android.content.Intent;\n" +
                "import android.content.Context;\n" +
                "\n" +
                "public class Example {\n" +
                "    public Intent create(Context context) {\n" +
                "        return new Intent().setClassName(context, \"BarActivity\");\n" +
                "    }\n" +
                "}";
        assertThat(lintProject(java(source))).isEqualTo("src/foo/Example.java:8: "
                                                                + "Warning: Intent's should be created in the (Pending)IntentFactory. [sc.CreateIntent]\n"
                                                                + "        return new Intent().setClassName(context, \"BarActivity\");\n"
                                                                + "               ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~\n"
                                                                + "0 errors, 1 warnings\n");
    }

    public void test_Method_OtherSetClass() throws Exception {
        @Language("JAVA") final String source = "package foo;\n" +
                "\n" +
                "import android.content.Intent;\n" +
                "import android.content.Context;\n" +
                "\n" +
                "public class Example {\n" +
                "    public Intent setClass(Context context, Class<?> clazz) {\n" +
                "        return null;\n" +
                "    }\n" +
                "    \n" +
                "    public void sample() {\n" +
                "        setClass(null, null);\n" +
                "    }\n" +
                "}";
        assertThat(lintProject(java(source))).isEqualTo("No warnings.");
    }

    public void test_Method_IntentSetClassNameString() throws Exception {
        @Language("JAVA") final String source = "package foo;\n" +
                "\n" +
                "import android.content.Intent;\n" +
                "\n" +
                "public class Example {\n" +
                "    public Intent create() {\n" +
                "        return new Intent().setClassName(\"foo\", \"BarActivity\");\n" +
                "    }\n" +
                "}";
        assertThat(lintProject(java(source))).isEqualTo("src/foo/Example.java:7: "
                                                                + "Warning: Intent's should be created in the (Pending)IntentFactory. [sc.CreateIntent]\n"
                                                                + "        return new Intent().setClassName(\"foo\", \"BarActivity\");\n"
                                                                + "               ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~\n"
                                                                + "0 errors, 1 warnings\n");
    }

    public void test_Method_IntentSetComponent() throws Exception {
        @Language("JAVA") final String source = "package foo;\n" +
                "\n" +
                "import android.content.Intent;\n" +
                "import android.content.ComponentName;\n" +
                "\n" +
                "public class Example {\n" +
                "    public Intent create(ComponentName component) {\n" +
                "        return new Intent().setComponent(component);\n" +
                "    }\n" +
                "}";
        assertThat(lintProject(java(source))).isEqualTo("src/foo/Example.java:8: "
                                                                + "Warning: Intent's should be created in the (Pending)IntentFactory. [sc.CreateIntent]\n"
                                                                + "        return new Intent().setComponent(component);\n"
                                                                + "               ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~\n"
                                                                + "0 errors, 1 warnings\n");
    }

    public void test_Method_InIntentFactory() throws Exception {
        @Language("JAVA") final String source = "package com.soundcloud.android.navigation;\n" +
                "\n" +
                "import android.content.Intent;\n" +
                "import foo.BarActivity;\n" +
                "import android.content.Context;\n" +
                "\n" +
                "public class IntentFactory {\n" +
                "    public static Intent create(Context context) {\n" +
                "        return new Intent().setClass(context, BarActivity.class);\n" +
                "    }\n" +
                "}";
        assertThat(lintProject(java(source), java(otherSample()))).isEqualTo("No warnings.");
    }

    public void test_Method_InPendingIntentFactory() throws Exception {
        @Language("JAVA") final String source = "package com.soundcloud.android.navigation;\n" +
                "\n" +
                "import android.content.Intent;\n" +
                "import foo.BarActivity;\n" +
                "import android.content.Context;\n" +
                "\n" +
                "public class PendingIntentFactory {\n" +
                "    public static Intent create(Context context) {\n" +
                "        return new Intent().setClass(context, BarActivity.class);\n" +
                "    }\n" +
                "}";
        assertThat(lintProject(java(source), java(otherSample()))).isEqualTo("No warnings.");
    }

    public void test_Method_StaticFactoryMethodInClass() throws Exception {
        @Language("JAVA") final String source = "package foo;\n" +
                "\n" +
                "import android.content.Intent;\n" +
                "import android.app.Activity;\n" +
                "import android.content.Context;\n" +
                "\n" +
                "public class SampleActivity extends Activity {\n" +
                "    public static Intent create(Context context) {\n" +
                "        return new Intent().setClass(context, SampleActivity.class);\n" +
                "    }\n" +
                "}";
        assertThat(lintProject(java(source))).isEqualTo("No warnings.");
    }

    public void test_Method_StaticFactoryMethodInDifferentClass() throws Exception {
        @Language("JAVA") final String source = "package foo;\n" +
                "\n" +
                "import android.content.Intent;\n" +
                "import android.app.Activity;\n" +
                "import android.content.Context;\n" +
                "import foo.BarActivity;\n" +
                "\n" +
                "public class SampleActivity extends Activity {\n" +
                "    public static Intent create(Context context) {\n" +
                "        return new Intent().setClass(context, BarActivity.class);\n" +
                "    }\n" +
                "}";
        assertThat(lintProject(java(source), java(otherSample()))).isEqualTo("src/foo/SampleActivity.java:10: "
                                                                                     + "Warning: Intent's should be created in the (Pending)IntentFactory. [sc.CreateIntent]\n"
                                                                                     + "        return new Intent().setClass(context, BarActivity.class);\n"
                                                                                     + "               ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~\n"
                                                                                     + "0 errors, 1 warnings\n");
    }

    public void test_Method_StaticFactoryMethodInDifferentClassWithActivity() throws Exception {
        @Language("JAVA") final String source = "package foo;\n" +
                "\n" +
                "import android.content.Intent;\n" +
                "import android.app.Activity;\n" +
                "import foo.BarActivity;\n" +
                "\n" +
                "public class SampleActivity extends Activity {\n" +
                "    public static Intent create(Activity activity) {\n" +
                "        return new Intent().setClass(activity, BarActivity.class);\n" +
                "    }\n" +
                "}";
        assertThat(lintProject(java(source), java(otherSample()))).isEqualTo("src/foo/SampleActivity.java:9: "
                                                                                     + "Warning: Intent's should be created in the (Pending)IntentFactory. [sc.CreateIntent]\n"
                                                                                     + "        return new Intent().setClass(activity, BarActivity.class);\n"
                                                                                     + "               ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~\n"
                                                                                     + "0 errors, 1 warnings\n");
    }

    public void test_Method_StaticFactoryMethodInDifferentClassIntegerClass() throws Exception {
        @Language("JAVA") final String source = "package foo;\n" +
                "\n" +
                "import android.content.Intent;\n" +
                "import android.app.Activity;\n" +
                "\n" +
                "public class SampleActivity extends Activity {\n" +
                "    public static Intent create(Activity activity) {\n" +
                "        return new Intent().setClass(activity, Integer.class);\n" +
                "    }\n" +
                "}";
        assertThat(lintProject(java(source))).isEqualTo("No warnings.");
    }

    public void test_Method_StartInClass() throws Exception {
        @Language("JAVA") final String source = "package foo;\n" +
                "\n" +
                "import android.app.Activity;\n" +
                "import android.content.Context;\n" +
                "import android.content.Intent;\n" +
                "\n" +
                "public class SampleActivity extends Activity {\n" +
                "    public static void start(Context context) {\n" +
                "        context.startActivity(new Intent().setClassName(\"test\", \"SampleActivity\"));\n" +
                "    }\n" +
                "}";
        assertThat(lintProject(java(source))).isEqualTo("src/foo/SampleActivity.java:9: "
                                                                + "Warning: Intent's should be created in the (Pending)IntentFactory. [sc.CreateIntent]\n"
                                                                + "        context.startActivity(new Intent().setClassName(\"test\", \"SampleActivity\"));\n"
                                                                + "                              ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~\n"
                                                                + "0 errors, 1 warnings\n");
    }

    public void test_Method_StaticFactoryInvalid() throws Exception {
        @Language("JAVA") final String source = "package foo;\n" +
                "\n" +
                "import android.content.Intent;\n" +
                "import foo.BarActivity;\n" +
                "import android.content.Context;\n" +
                "\n" +
                "public class InvalidFactory {\n" +
                "    public static Intent create(Context context) {\n" +
                "        return new Intent().setClass(context, BarActivity.class);\n" +
                "    }\n" +
                "}";
        assertThat(lintProject(java(source), java(otherSample()))).isEqualTo("src/foo/InvalidFactory.java:9: "
                                                                                     + "Warning: Intent's should be created in the (Pending)IntentFactory. [sc.CreateIntent]\n"
                                                                                     + "        return new Intent().setClass(context, BarActivity.class);\n"
                                                                                     + "               ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~\n"
                                                                                     + "0 errors, 1 warnings\n");
    }

    public void test_Method_InvalidCreation() throws Exception {
        @Language("JAVA") final String source = "package foo;\n" +
                "\n" +
                "import android.content.Intent;\n" +
                "import android.content.ComponentName;\n" +
                "import android.content.Context;\n" +
                "\n" +
                "public class Example {\n" +
                "    public Intent create(Context context, ComponentName component) {\n" +
                "        return new Intent().setComponent(component);\n" +
                "    }\n" +
                "}";
        assertThat(lintProject(java(source))).isEqualTo("src/foo/Example.java:9: "
                                                                + "Warning: Intent's should be created in the (Pending)IntentFactory. [sc.CreateIntent]\n"
                                                                + "        return new Intent().setComponent(component);\n"
                                                                + "               ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~\n"
                                                                + "0 errors, 1 warnings\n");
    }

    @Override
    protected Detector getDetector() {
        return new IntentFactoryDetector();
    }

    @Override
    protected List<Issue> getIssues() {
        return Collections.singletonList(IntentFactoryDetector.ISSUE_CREATE_OUTSIDE);
    }

    @Language("JAVA")
    private static String otherSample() {
        return "package foo;\n" +
                "\n" +
                "import android.content.Intent;\n" +
                "import android.app.Activity;\n" +
                "\n" +
                "public class BarActivity extends Activity {\n" +
                "}";
    }
}
