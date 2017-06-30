package com.soundcloud.android.memento;

import com.google.testing.compile.JavaFileObjects;
import org.junit.Test;

import javax.tools.JavaFileObject;
import java.util.Arrays;

import static com.google.common.truth.Truth.assertAbout;
import static com.google.testing.compile.JavaSourceSubjectFactory.javaSource;
import static com.google.testing.compile.JavaSourcesSubjectFactory.javaSources;

public class MementoProcessorTest {

    @Test
    public void wrongSuperClass() throws Exception {
        JavaFileObject input = JavaFileObjects.forSourceString("com.test.SampleDetector",
                "package com.test;\n" +
                        "\n" +
                        "import com.soundcloud.android.memento.annotation.LintDetector;\n" +
                        "\n" +
                        "@LintDetector\n" +
                        "public class SampleDetector {\n" +
                        "}"
        );

        assertAbout(javaSource())
                .that(input)
                .withCompilerOptions("-Adebug")
                .processedWith(new MementoProcessor())
                .failsToCompile()
                .withErrorContaining("LintDetector must be on a class extending Detector");
    }

    @Test
    public void noClass() throws Exception {
        JavaFileObject input = JavaFileObjects.forSourceString("com.test.SampleDetector",
                "package com.test;\n" +
                        "\n" +
                        "import com.soundcloud.android.memento.annotation.LintDetector;\n" +
                        "\n" +
                        "@LintDetector\n" +
                        "public interface SampleDetector {\n" +
                        "}"
        );

        assertAbout(javaSource())
                .that(input)
                .withCompilerOptions("-Adebug")
                .processedWith(new MementoProcessor())
                .failsToCompile()
                .withErrorContaining("LintDetector Annotation can only be used on Classes");
    }

    @Test
    public void wrongIssueVisibility() throws Exception {
        JavaFileObject input = JavaFileObjects.forSourceString("com.test.SampleDetector",
                "package com.test;\n" +
                        "\n" +
                        "import com.android.tools.lint.detector.api.*;\n" +
                        "import com.soundcloud.android.memento.annotation.Exclude;\n" +
                        "import com.soundcloud.android.memento.annotation.LintDetector;\n" +
                        "\n" +
                        "@LintDetector\n" +
                        "public class SampleDetector extends Detector implements Detector.JavaPsiScanner {\n" +
                        "    @Exclude\n" +
                        "    private static final Issue ISSUE = Issue.create(\"id\",\n" +
                        "            \"description\",\n" +
                        "            \"explanation\",\n" +
                        "            Category.MESSAGES,\n" +
                        "            1,\n" +
                        "            Severity.ERROR,\n" +
                        "            new Implementation(SampleDetector.class, Scope.JAVA_FILE_SCOPE));\n" +
                        "}"
        );

        assertAbout(javaSource())
                .that(input)
                .withCompilerOptions("-Adebug")
                .processedWith(new MementoProcessor())
                .failsToCompile()
                .withErrorContaining("Each Issue must be at least a package private constant");
    }

    @Test
    public void generatesNoRegistry_NoIssue() throws Exception {
        JavaFileObject input = JavaFileObjects.forSourceString("com.test.SampleDetector",
                "package com.test;\n" +
                        "\n" +
                        "import com.android.tools.lint.detector.api.*;\n" +
                        "import com.soundcloud.android.memento.annotation.LintDetector;\n" +
                        "\n" +
                        "@LintDetector\n" +
                        "public class SampleDetector extends Detector implements Detector.JavaPsiScanner {\n" +
                        "}"
        );

        assertAbout(javaSource())
                .that(input)
                .withCompilerOptions("-Adebug")
                .processedWith(new MementoProcessor())
                .compilesWithoutError()
                .withNoteContaining("Skipped issue registry generation");
    }

    @Test
    public void generatesNoRegistry_ExcludedIssue() throws Exception {
        JavaFileObject input = JavaFileObjects.forSourceString("com.test.SampleDetector",
                "package com.test;\n" +
                        "\n" +
                        "import com.android.tools.lint.detector.api.*;\n" +
                        "import com.soundcloud.android.memento.annotation.Exclude;\n" +
                        "import com.soundcloud.android.memento.annotation.LintDetector;\n" +
                        "\n" +
                        "@LintDetector\n" +
                        "public class SampleDetector extends Detector implements Detector.JavaPsiScanner {\n" +
                        "    @Exclude\n" +
                        "    public static final Issue ISSUE = Issue.create(\"id\",\n" +
                        "            \"description\",\n" +
                        "            \"explanation\",\n" +
                        "            Category.MESSAGES,\n" +
                        "            1,\n" +
                        "            Severity.ERROR,\n" +
                        "            new Implementation(SampleDetector.class, Scope.JAVA_FILE_SCOPE));\n" +
                        "}"
        );

        assertAbout(javaSource())
                .that(input)
                .withCompilerOptions("-Adebug")
                .processedWith(new MementoProcessor())
                .compilesWithoutError()
                .withNoteContaining("Skipped issue registry generation");
    }

    @Test
    public void oneDetectorOneIssue() throws Exception {
        JavaFileObject input = JavaFileObjects.forSourceString("com.test.SampleDetector",
                "package com.test;\n" +
                        "\n" +
                        "import com.android.tools.lint.detector.api.*;\n" +
                        "import com.soundcloud.android.memento.annotation.LintDetector;\n" +
                        "\n" +
                        "@LintDetector\n" +
                        "public class SampleDetector extends Detector implements Detector.JavaPsiScanner {\n" +
                        "    public static final Issue ISSUE = Issue.create(\"id\",\n" +
                        "            \"description\",\n" +
                        "            \"explanation\",\n" +
                        "            Category.MESSAGES,\n" +
                        "            1,\n" +
                        "            Severity.ERROR,\n" +
                        "            new Implementation(SampleDetector.class, Scope.JAVA_FILE_SCOPE));\n" +
                        "}"
        );

        JavaFileObject expected = JavaFileObjects.forSourceString("com.test.Memento_IssueRegistry",
                "package com.test;\n" +
                        "\n" +
                        "import com.android.tools.lint.client.api.IssueRegistry;\n" +
                        "import com.android.tools.lint.detector.api.Issue;\n" +
                        "import java.lang.Override;\n" +
                        "import java.util.ArrayList;\n" +
                        "import java.util.List;\n" +
                        "import javax.annotation.Generated;\n" +
                        "\n" +
                        "@Generated(\"Generated by MementoProcessor\")\n" +
                        "public final class Memento_IssueRegistry extends IssueRegistry {\n" +
                        "    @Override\n" +
                        "    public List<Issue> getIssues() {\n" +
                        "        final List<Issue> issues = new ArrayList<Issue>();\n" +
                        "        issues.add(SampleDetector.ISSUE);\n" +
                        "        return issues;\n" +
                        "    }\n" +
                        "} "
        );

        assertAbout(javaSource())
                .that(input)
                .processedWith(new MementoProcessor())
                .compilesWithoutError()
                .and()
                .generatesSources(expected);
    }

    @Test
    public void oneDetectorTwoIssues() throws Exception {
        JavaFileObject input = JavaFileObjects.forSourceString("com.test.SampleDetector",
                "package com.test;\n" +
                        "\n" +
                        "import com.android.tools.lint.detector.api.*;\n" +
                        "import com.soundcloud.android.memento.annotation.LintDetector;\n" +
                        "\n" +
                        "@LintDetector\n" +
                        "public class SampleDetector extends Detector implements Detector.JavaPsiScanner {\n" +
                        "    public static final Issue ISSUE_1 = Issue.create(\"id\",\n" +
                        "            \"description\",\n" +
                        "            \"explanation\",\n" +
                        "            Category.MESSAGES,\n" +
                        "            1,\n" +
                        "            Severity.ERROR,\n" +
                        "            new Implementation(SampleDetector.class, Scope.JAVA_FILE_SCOPE));\n" +
                        "    public static final Issue ISSUE_2 = Issue.create(\"id\",\n" +
                        "            \"description\",\n" +
                        "            \"explanation\",\n" +
                        "            Category.MESSAGES,\n" +
                        "            1,\n" +
                        "            Severity.ERROR,\n" +
                        "            new Implementation(SampleDetector.class, Scope.JAVA_FILE_SCOPE));\n" +
                        "}"
        );

        JavaFileObject expected = JavaFileObjects.forSourceString("com.test.Memento_IssueRegistry",
                "package com.test;\n" +
                        "\n" +
                        "import com.android.tools.lint.client.api.IssueRegistry;\n" +
                        "import com.android.tools.lint.detector.api.Issue;\n" +
                        "import java.lang.Override;\n" +
                        "import java.util.ArrayList;\n" +
                        "import java.util.List;\n" +
                        "import javax.annotation.Generated;\n" +
                        "\n" +
                        "@Generated(\"Generated by MementoProcessor\")\n" +
                        "public final class Memento_IssueRegistry extends IssueRegistry {\n" +
                        "    @Override\n" +
                        "    public List<Issue> getIssues() {\n" +
                        "        final List<Issue> issues = new ArrayList<Issue>();\n" +
                        "        issues.add(SampleDetector.ISSUE_1);\n" +
                        "        issues.add(SampleDetector.ISSUE_2);\n" +
                        "        return issues;\n" +
                        "    }\n" +
                        "} "
        );

        assertAbout(javaSource())
                .that(input)
                .processedWith(new MementoProcessor())
                .compilesWithoutError()
                .and()
                .generatesSources(expected);
    }

    @Test
    public void oneDetectorTwoIssuesOneExcluded() throws Exception {
        JavaFileObject input = JavaFileObjects.forSourceString("com.test.SampleDetector",
                "package com.test;\n" +
                        "\n" +
                        "import com.android.tools.lint.detector.api.*;\n" +
                        "import com.soundcloud.android.memento.annotation.Exclude;\n" +
                        "import com.soundcloud.android.memento.annotation.LintDetector;\n" +
                        "\n" +
                        "@LintDetector\n" +
                        "public class SampleDetector extends Detector implements Detector.JavaPsiScanner {\n" +
                        "    public static final Issue ISSUE_1 = Issue.create(\"id\",\n" +
                        "            \"description\",\n" +
                        "            \"explanation\",\n" +
                        "            Category.MESSAGES,\n" +
                        "            1,\n" +
                        "            Severity.ERROR,\n" +
                        "            new Implementation(SampleDetector.class, Scope.JAVA_FILE_SCOPE));\n" +
                        "    @Exclude\n" +
                        "    public static final Issue ISSUE_2 = Issue.create(\"id\",\n" +
                        "            \"description\",\n" +
                        "            \"explanation\",\n" +
                        "            Category.MESSAGES,\n" +
                        "            1,\n" +
                        "            Severity.ERROR,\n" +
                        "            new Implementation(SampleDetector.class, Scope.JAVA_FILE_SCOPE));\n" +
                        "}"
        );

        JavaFileObject expected = JavaFileObjects.forSourceString("com.test.Memento_IssueRegistry",
                "package com.test;\n" +
                        "\n" +
                        "import com.android.tools.lint.client.api.IssueRegistry;\n" +
                        "import com.android.tools.lint.detector.api.Issue;\n" +
                        "import java.lang.Override;\n" +
                        "import java.util.ArrayList;\n" +
                        "import java.util.List;\n" +
                        "import javax.annotation.Generated;\n" +
                        "\n" +
                        "@Generated(\"Generated by MementoProcessor\")\n" +
                        "public final class Memento_IssueRegistry extends IssueRegistry {\n" +
                        "    @Override\n" +
                        "    public List<Issue> getIssues() {\n" +
                        "        final List<Issue> issues = new ArrayList<Issue>();\n" +
                        "        issues.add(SampleDetector.ISSUE_1);\n" +
                        "        return issues;\n" +
                        "    }\n" +
                        "} "
        );

        assertAbout(javaSource())
                .that(input)
                .processedWith(new MementoProcessor())
                .compilesWithoutError()
                .and()
                .generatesSources(expected);
    }

    @Test
    public void twoDetectorOneIssueEach() throws Exception {
        JavaFileObject input1 = JavaFileObjects.forSourceString("com.test.SampleDetector1",
                "package com.test;\n" +
                        "\n" +
                        "import com.android.tools.lint.detector.api.*;\n" +
                        "import com.soundcloud.android.memento.annotation.Exclude;\n" +
                        "import com.soundcloud.android.memento.annotation.LintDetector;\n" +
                        "\n" +
                        "@LintDetector\n" +
                        "public class SampleDetector1 extends Detector implements Detector.JavaPsiScanner {\n" +
                        "    public static final Issue ISSUE = Issue.create(\"id\",\n" +
                        "            \"description\",\n" +
                        "            \"explanation\",\n" +
                        "            Category.MESSAGES,\n" +
                        "            1,\n" +
                        "            Severity.ERROR,\n" +
                        "            new Implementation(SampleDetector1.class, Scope.JAVA_FILE_SCOPE));\n" +
                        "}"
        );
        JavaFileObject input2 = JavaFileObjects.forSourceString("com.test.SampleDetector2",
                "package com.test;\n" +
                        "\n" +
                        "import com.android.tools.lint.detector.api.*;\n" +
                        "import com.soundcloud.android.memento.annotation.Exclude;\n" +
                        "import com.soundcloud.android.memento.annotation.LintDetector;\n" +
                        "\n" +
                        "@LintDetector\n" +
                        "public class SampleDetector2 extends Detector implements Detector.JavaPsiScanner {\n" +
                        "    public static final Issue ISSUE = Issue.create(\"id\",\n" +
                        "            \"description\",\n" +
                        "            \"explanation\",\n" +
                        "            Category.MESSAGES,\n" +
                        "            1,\n" +
                        "            Severity.ERROR,\n" +
                        "            new Implementation(SampleDetector2.class, Scope.JAVA_FILE_SCOPE));\n" +
                        "}"
        );

        JavaFileObject expected = JavaFileObjects.forSourceString("com.test.Memento_IssueRegistry",
                "package com.test;\n" +
                        "\n" +
                        "import com.android.tools.lint.client.api.IssueRegistry;\n" +
                        "import com.android.tools.lint.detector.api.Issue;\n" +
                        "import java.lang.Override;\n" +
                        "import java.util.ArrayList;\n" +
                        "import java.util.List;\n" +
                        "import javax.annotation.Generated;\n" +
                        "\n" +
                        "@Generated(\"Generated by MementoProcessor\")\n" +
                        "public final class Memento_IssueRegistry extends IssueRegistry {\n" +
                        "    @Override\n" +
                        "    public List<Issue> getIssues() {\n" +
                        "        final List<Issue> issues = new ArrayList<Issue>();\n" +
                        "        issues.add(SampleDetector1.ISSUE);\n" +
                        "        issues.add(SampleDetector2.ISSUE);\n" +
                        "        return issues;\n" +
                        "    }\n" +
                        "} "
        );

        assertAbout(javaSources())
                .that(Arrays.asList(input1, input2))
                .processedWith(new MementoProcessor())
                .compilesWithoutError()
                .and()
                .generatesSources(expected);
    }

    @Test
    public void twoDetectorOneIssueEachOneExcluded() throws Exception {
        JavaFileObject input1 = JavaFileObjects.forSourceString("com.test.SampleDetector1",
                "package com.test;\n" +
                        "\n" +
                        "import com.android.tools.lint.detector.api.*;\n" +
                        "import com.soundcloud.android.memento.annotation.Exclude;\n" +
                        "import com.soundcloud.android.memento.annotation.LintDetector;\n" +
                        "\n" +
                        "@LintDetector\n" +
                        "public class SampleDetector1 extends Detector implements Detector.JavaPsiScanner {\n" +
                        "    public static final Issue ISSUE = Issue.create(\"id\",\n" +
                        "            \"description\",\n" +
                        "            \"explanation\",\n" +
                        "            Category.MESSAGES,\n" +
                        "            1,\n" +
                        "            Severity.ERROR,\n" +
                        "            new Implementation(SampleDetector1.class, Scope.JAVA_FILE_SCOPE));\n" +
                        "}"
        );
        JavaFileObject input2 = JavaFileObjects.forSourceString("com.test.SampleDetector2",
                "package com.test;\n" +
                        "\n" +
                        "import com.android.tools.lint.detector.api.*;\n" +
                        "import com.soundcloud.android.memento.annotation.Exclude;\n" +
                        "import com.soundcloud.android.memento.annotation.LintDetector;\n" +
                        "\n" +
                        "@LintDetector\n" +
                        "public class SampleDetector2 extends Detector implements Detector.JavaPsiScanner {\n" +
                        "    @Exclude\n" +
                        "    public static final Issue ISSUE = Issue.create(\"id\",\n" +
                        "            \"description\",\n" +
                        "            \"explanation\",\n" +
                        "            Category.MESSAGES,\n" +
                        "            1,\n" +
                        "            Severity.ERROR,\n" +
                        "            new Implementation(SampleDetector2.class, Scope.JAVA_FILE_SCOPE));\n" +
                        "}"
        );

        JavaFileObject expected = JavaFileObjects.forSourceString("com.test.Memento_IssueRegistry",
                "package com.test;\n" +
                        "\n" +
                        "import com.android.tools.lint.client.api.IssueRegistry;\n" +
                        "import com.android.tools.lint.detector.api.Issue;\n" +
                        "import java.lang.Override;\n" +
                        "import java.util.ArrayList;\n" +
                        "import java.util.List;\n" +
                        "import javax.annotation.Generated;\n" +
                        "\n" +
                        "@Generated(\"Generated by MementoProcessor\")\n" +
                        "public final class Memento_IssueRegistry extends IssueRegistry {\n" +
                        "    @Override\n" +
                        "    public List<Issue> getIssues() {\n" +
                        "        final List<Issue> issues = new ArrayList<Issue>();\n" +
                        "        issues.add(SampleDetector1.ISSUE);\n" +
                        "        return issues;\n" +
                        "    }\n" +
                        "} "
        );

        assertAbout(javaSources())
                .that(Arrays.asList(input1, input2))
                .processedWith(new MementoProcessor())
                .compilesWithoutError()
                .and()
                .generatesSources(expected);
    }

    @Test
    public void twoDetectorTwoIssueEach() throws Exception {
        JavaFileObject input1 = JavaFileObjects.forSourceString("com.test.SampleDetector1",
                "package com.test;\n" +
                        "\n" +
                        "import com.android.tools.lint.detector.api.*;\n" +
                        "import com.soundcloud.android.memento.annotation.Exclude;\n" +
                        "import com.soundcloud.android.memento.annotation.LintDetector;\n" +
                        "\n" +
                        "@LintDetector\n" +
                        "public class SampleDetector1 extends Detector implements Detector.JavaPsiScanner {\n" +
                        "    public static final Issue ISSUE_1 = Issue.create(\"id\",\n" +
                        "            \"description\",\n" +
                        "            \"explanation\",\n" +
                        "            Category.MESSAGES,\n" +
                        "            1,\n" +
                        "            Severity.ERROR,\n" +
                        "            new Implementation(SampleDetector1.class, Scope.JAVA_FILE_SCOPE));\n" +
                        "    public static final Issue ISSUE_2 = Issue.create(\"id\",\n" +
                        "            \"description\",\n" +
                        "            \"explanation\",\n" +
                        "            Category.MESSAGES,\n" +
                        "            1,\n" +
                        "            Severity.ERROR,\n" +
                        "            new Implementation(SampleDetector1.class, Scope.JAVA_FILE_SCOPE));\n" +
                        "}"
        );
        JavaFileObject input2 = JavaFileObjects.forSourceString("com.test.SampleDetector2",
                "package com.test;\n" +
                        "\n" +
                        "import com.android.tools.lint.detector.api.*;\n" +
                        "import com.soundcloud.android.memento.annotation.Exclude;\n" +
                        "import com.soundcloud.android.memento.annotation.LintDetector;\n" +
                        "\n" +
                        "@LintDetector\n" +
                        "public class SampleDetector2 extends Detector implements Detector.JavaPsiScanner {\n" +
                        "    public static final Issue ISSUE_1 = Issue.create(\"id\",\n" +
                        "            \"description\",\n" +
                        "            \"explanation\",\n" +
                        "            Category.MESSAGES,\n" +
                        "            1,\n" +
                        "            Severity.ERROR,\n" +
                        "            new Implementation(SampleDetector2.class, Scope.JAVA_FILE_SCOPE));\n" +
                        "    public static final Issue ISSUE_2 = Issue.create(\"id\",\n" +
                        "            \"description\",\n" +
                        "            \"explanation\",\n" +
                        "            Category.MESSAGES,\n" +
                        "            1,\n" +
                        "            Severity.ERROR,\n" +
                        "            new Implementation(SampleDetector2.class, Scope.JAVA_FILE_SCOPE));\n" +
                        "}"
        );

        JavaFileObject expected = JavaFileObjects.forSourceString("com.test.Memento_IssueRegistry",
                "package com.test;\n" +
                        "\n" +
                        "import com.android.tools.lint.client.api.IssueRegistry;\n" +
                        "import com.android.tools.lint.detector.api.Issue;\n" +
                        "import java.lang.Override;\n" +
                        "import java.util.ArrayList;\n" +
                        "import java.util.List;\n" +
                        "import javax.annotation.Generated;\n" +
                        "\n" +
                        "@Generated(\"Generated by MementoProcessor\")\n" +
                        "public final class Memento_IssueRegistry extends IssueRegistry {\n" +
                        "    @Override\n" +
                        "    public List<Issue> getIssues() {\n" +
                        "        final List<Issue> issues = new ArrayList<Issue>();\n" +
                        "        issues.add(SampleDetector1.ISSUE_1);\n" +
                        "        issues.add(SampleDetector1.ISSUE_2);\n" +
                        "        issues.add(SampleDetector2.ISSUE_1);\n" +
                        "        issues.add(SampleDetector2.ISSUE_2);\n" +
                        "        return issues;\n" +
                        "    }\n" +
                        "} "
        );

        assertAbout(javaSources())
                .that(Arrays.asList(input1, input2))
                .processedWith(new MementoProcessor())
                .compilesWithoutError()
                .and()
                .generatesSources(expected);
    }
}
