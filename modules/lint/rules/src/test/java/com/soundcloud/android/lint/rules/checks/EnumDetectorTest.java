package com.soundcloud.android.lint.rules.checks;

import com.android.tools.lint.checks.infrastructure.LintDetectorTest;
import com.android.tools.lint.detector.api.Detector;
import com.android.tools.lint.detector.api.Issue;
import org.intellij.lang.annotations.Language;

import java.util.Collections;
import java.util.List;

public class EnumDetectorTest extends LintDetectorTest {

    @Override
    protected Detector getDetector() {
        return new EnumDetector();
    }

    @Override
    protected List<Issue> getIssues() {
        return Collections.singletonList(EnumDetector.ISSUE_ENUM_USAGE);
    }

    public void test_NoCall() throws Exception {
        @Language("JAVA") final String source = "package foo;\n" +
                "import bar.Sample;\n" +
                "public class Testing {\n" +
                "   public void helloWorld() {\n" +
                "       Sample sample = Sample.TEST_1;\n" +
                "   }\n" +
                "}";
        lint().files(java(source), sampleEnumNoLabel())
              .run()
              .expectClean();
    }

    public void test_NoCall_WithLabel() throws Exception {
        @Language("JAVA") final String source = "package foo;\n" +
                "import bar.Sample;\n" +
                "public class Testing {\n" +
                "   public void helloWorld() {\n" +
                "       Sample sample = Sample.TEST_1;\n" +
                "   }\n" +
                "}";
        lint().files(java(source), sampleEnumWithLabel())
              .run()
              .expectClean();
    }

    // name()

    public void test_name_otherCall() throws Exception {
        @Language("JAVA") final String source = "package foo;\n" +
                "public class Testing {\n" +
                "   public final String name() {\n" +
                "       return \"test\";\n" +
                "   }\n" +
                "\n" +
                "   public void helloWorld() {\n" +
                "       final String test = name();\n" +
                "   }\n" +
                "}";
        lint().files(java(source))
              .run()
              .expectClean();
    }

    public void test_name_NoLabel_Call() throws Exception {
        @Language("JAVA") final String source = "package foo;\n" +
                "import bar.Sample;\n" +
                "public class Testing {\n" +
                "   public void helloWorld() {\n" +
                "       String sampleName = Sample.TEST_1.name();\n" +
                "   }\n" +
                "}";
        lint().files(java(source), sampleEnumNoLabel())
              .run()
              .expectClean();
    }

    public void test_name_Label_LabelCall() throws Exception {
        @Language("JAVA") final String source = "package foo;\n" +
                "import bar.Sample;\n" +
                "public class Testing {\n" +
                "   public void helloWorld() {\n" +
                "       String sampleName = Sample.TEST_1.getLabel();\n" +
                "   }\n" +
                "}";
        lint().files(java(source), sampleEnumWithLabel())
              .run()
              .expectClean();
    }

    public void test_name_Label_Call() throws Exception {
        @Language("JAVA") final String source = "package foo;\n" +
                "import bar.Sample;\n" +
                "public class Testing {\n" +
                "   public void helloWorld() {\n" +
                "       String sampleName = Sample.TEST_1.name();\n" +
                "   }\n" +
                "}";
        lint().files(java(source), sampleEnumWithLabel())
              .run()
              .expect("src/foo/Testing.java:5: Error: Unsafe name() call to enum with label. [sc.EnumUsage]\n" +
                              "       String sampleName = Sample.TEST_1.name();\n" +
                              "                                         ~~~~\n" +
                              "1 errors, 0 warnings\n");
    }

    public void test_name_Label_Suppressed_Call() throws Exception {
        @Language("JAVA") final String source = "package foo;\n" +
                "import bar.Sample;\n" +
                "public class Testing {\n" +
                "   @SuppressWarnings(\"sc.EnumUsage\")\n" +
                "   public void helloWorld() {\n" +
                "       String sampleName = Sample.TEST_1.name();\n" +
                "   }\n" +
                "}";
        lint().files(java(source), sampleEnumWithLabel())
              .run()
              .expectClean();
    }

    public void test_name_Label_SuppressedEnum_Call() throws Exception {
        @Language("JAVA") final String source = "package foo;\n" +
                "import bar.Sample;\n" +
                "public class Testing {\n" +
                "   public void helloWorld() {\n" +
                "       String sampleName = Sample.TEST_1.name();\n" +
                "   }\n" +
                "}";
        lint().files(java(source), sampleSuppressedEnumWithLabel())
              .run()
              .expectClean();
    }

    public void test_name_Call_Inside() throws Exception {
        @Language("JAVA") final String source = "package foo;\n" +
                "public enum Testing {\n" +
                "   TEST;\n" +
                "\n" +
                "   public static boolean isTest(String name) {\n" +
                "       return TEST.name().equals(name);\n" +
                "   }\n" +
                "}";
        lint().files(java(source))
              .run()
              .expectClean();
    }

    public void test_name_Call_WithLabelInside() throws Exception {
        @Language("JAVA") final String source = "package foo;\n" +
                "public enum Testing {\n" +
                "   TEST(\"testing\");\n" +
                "\n" +
                "   public static boolean isTest(String name) {\n" +
                "       return TEST.name().equals(name);\n" +
                "   }\n" +
                "\n" +
                "   private final String label;\n" +
                "\n" +
                "   private Testing(String label) {\n" +
                "       this.label = label;\n" +
                "   }\n" +
                "\n" +
                "   public String getLabel() {\n" +
                "       return label;\n" +
                "   }\n" +
                "}";
        lint().files(java(source))
              .run()
              .expect("src/foo/Testing.java:6: Error: Unsafe name() call to enum with label. [sc.EnumUsage]\n" +
                              "       return TEST.name().equals(name);\n" +
                              "                   ~~~~\n" +
                              "1 errors, 0 warnings\n");
    }

    // valueOf

    public void test_valueOf_otherCall() throws Exception {
        @Language("JAVA") final String source = "package foo;\n" +
                "public class Testing {\n" +
                "   public final String valueOf(String test) {\n" +
                "       return \"test\";\n" +
                "   }\n" +
                "\n" +
                "   public void helloWorld() {\n" +
                "       final String test = valueOf(\"testing\");\n" +
                "   }\n" +
                "}";
        lint().files(java(source))
              .run()
              .expectClean();
    }

    public void test_valueOf_NoLabel_Call() throws Exception {
        @Language("JAVA") final String source = "package foo;\n" +
                "import bar.Sample;\n" +
                "public class Testing {\n" +
                "   public void helloWorld() {\n" +
                "       Sample sampleName = Sample.TEST_1.valueOf(\"testing\");\n" +
                "   }\n" +
                "}";
        lint().files(java(source), sampleEnumNoLabel())
              .run()
              .expectClean();
    }

    public void test_valueOf_Label_LabelCall() throws Exception {
        @Language("JAVA") final String source = "package foo;\n" +
                "import bar.Sample;\n" +
                "public class Testing {\n" +
                "   public void helloWorld() {\n" +
                "       Sample sampleName = Sample.TEST_1.from(\"testing\");\n" +
                "   }\n" +
                "}";
        lint().files(java(source), sampleEnumWithLabel())
              .run()
              .expectClean();
    }

    public void test_valueOf_Label_Call() throws Exception {
        @Language("JAVA") final String source = "package foo;\n" +
                "import bar.Sample;\n" +
                "public class Testing {\n" +
                "   public void helloWorld() {\n" +
                "       Sample sampleName = Sample.TEST_1.valueOf(\"test\");\n" +
                "   }\n" +
                "}";
        lint().files(java(source), sampleEnumWithLabel())
              .run()
              .expect("src/foo/Testing.java:5: Error: Unsafe valueOf() call to enum with label. [sc.EnumUsage]\n" +
                              "       Sample sampleName = Sample.TEST_1.valueOf(\"test\");\n" +
                              "                                         ~~~~~~~\n" +
                              "1 errors, 0 warnings\n");
    }

    public void test_valueOf_Label_Suppressed_Call() throws Exception {
        @Language("JAVA") final String source = "package foo;\n" +
                "import bar.Sample;\n" +
                "public class Testing {\n" +
                "   @SuppressWarnings(\"sc.EnumUsage\")\n" +
                "   public void helloWorld() {\n" +
                "       Sample sampleName = Sample.TEST_1.valueOf(\"test\");\n" +
                "   }\n" +
                "}";
        lint().files(java(source), sampleEnumWithLabel())
              .run()
              .expectClean();
    }

    public void test_valueOf_Label_SuppressedEnum_Call() throws Exception {
        @Language("JAVA") final String source = "package foo;\n" +
                "import bar.Sample;\n" +
                "public class Testing {\n" +
                "   public void helloWorld() {\n" +
                "       Sample sampleName = Sample.TEST_1.valueOf(\"test\");\n" +
                "   }\n" +
                "}";
        lint().files(java(source), sampleSuppressedEnumWithLabel())
              .run()
              .expectClean();
    }

    public void test_valueOf_Call_Inside() throws Exception {
        @Language("JAVA") final String source = "package foo;\n" +
                "public enum Testing {\n" +
                "   TEST;\n" +
                "\n" +
                "   public static Testing from(String name) {\n" +
                "       return Testing.valueOf(\"test\");\n" +
                "   }\n" +
                "}";
        lint().files(java(source))
              .run()
              .expectClean();
    }

    public void test_valueOf_Call_WithLabelInside() throws Exception {
        @Language("JAVA") final String source = "package foo;\n" +
                "public enum Testing {\n" +
                "   TEST(\"testing\");\n" +
                "\n" +
                "   public static Testing from(String name) {\n" +
                "       return Testing.valueOf(\"test\");\n" +
                "   }\n" +
                "\n" +
                "   private final String label;\n" +
                "\n" +
                "   private Testing(String label) {\n" +
                "       this.label = label;\n" +
                "   }\n" +
                "\n" +
                "   public String getLabel() {\n" +
                "       return label;\n" +
                "   }\n" +
                "}";
        lint().files(java(source))
              .run()
              .expect("src/foo/Testing.java:6: Error: Unsafe valueOf() call to enum with label. [sc.EnumUsage]\n" +
                              "       return Testing.valueOf(\"test\");\n" +
                              "                      ~~~~~~~\n" +
                              "1 errors, 0 warnings\n");
    }

    private static TestFile sampleEnumNoLabel() {
        return java("package bar;\n" +
                            "public enum Sample {\n" +
                            "   TEST_1,\n" +
                            "   TEST_2\n" +
                            "}");
    }

    private static TestFile sampleEnumWithLabel() {
        return java("package bar;\n" +
                            "public enum Sample {\n" +
                            "   TEST_1(\"test1\"),\n" +
                            "   TEST_2(\"test2\");\n" +
                            "   \n" +
                            "   private final String label;\n" +
                            "   \n" +
                            "   private Sample(String label) {\n" +
                            "       this.label = label;\n" +
                            "   }\n" +
                            "   \n" +
                            "   public String getLabel() {\n" +
                            "       return label;\n" +
                            "   }\n" +
                            "   \n" +
                            "   public static Sample from(String label) {\n" +
                            "       return TEST_1;\n" +
                            "   }\n" +
                            "}");
    }

    private static TestFile sampleSuppressedEnumWithLabel() {
        return java("package bar;\n" +
                            "@SuppressWarnings(\"sc.EnumUsage\") \n" +
                            "public enum Sample {\n" +
                            "   TEST_1(\"test1\"),\n" +
                            "   TEST_2(\"test2\");\n" +
                            "   \n" +
                            "   private final String label;\n" +
                            "   \n" +
                            "   private Sample(String label) {\n" +
                            "       this.label = label;\n" +
                            "   }\n" +
                            "   \n" +
                            "   public String getLabel() {\n" +
                            "       return label;\n" +
                            "   }\n" +
                            "}");
    }
}
