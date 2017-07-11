package com.soundcloud.android.lint.rules.checks;

import com.android.tools.lint.checks.infrastructure.LintDetectorTest;
import com.android.tools.lint.detector.api.Detector;
import com.android.tools.lint.detector.api.Issue;

import java.util.Arrays;
import java.util.List;

public class XmlFormattingDetectorTest extends LintDetectorTest {

    @Override
    protected Detector getDetector() {
        return new XmlFormattingDetector();
    }

    @Override
    protected List<Issue> getIssues() {
        return Arrays.asList(XmlFormattingDetector.ISSUE_ORDER,
                             XmlFormattingDetector.ISSUE_EMPTY_BODY,
                             XmlFormattingDetector.ISSUE_ATTR_WRAP,
                             XmlFormattingDetector.ISSUE_WHITESPACE,
                             XmlFormattingDetector.ISSUE_LINE_BREAK);
    }

    public void testIgnoresNonLayouts() throws Exception {
        final TestFile source = xml("res/drawable/drawable1.xml",
                                    "<?xml version=\"1.0\" encoding=\"utf-8\"?>\n" +
                                            "<set xmlns:android=\"http://schemas.android.com/apk/res/android\"\n" +
                                            "        android:shareInterpolator=\"false\"\n" +
                                            "        android:zAdjustment=\"top\">\n" +
                                            "    <alpha android:fromAlpha=\"0.0\" android:toAlpha=\"1.0\"\n" +
                                            "            android:interpolator=\"@android:interpolator/decelerate_cubic\"\n" +
                                            "            android:fillEnabled=\"true\"\n" +
                                            "            android:fillBefore=\"false\" android:fillAfter=\"true\"\n" +
                                            "            android:duration=\"300\"/>\n" +
                                            "</set>");
        lint().files(source)
              .run()
              .expectClean();
    }

    public void testValid() throws Exception {
        final TestFile source = xml("res/layout/layout1.xml",
                                    "<?xml version=\"1.0\" encoding=\"utf-8\"?>\n" +
                                            "<RelativeLayout \n" +
                                            "    xmlns:android=\"http://schemas.android.com/apk/res/android\" \n" +
                                            "    xmlns:app=\"http://schemas.android.com/apk/res-auto\" \n" +
                                            "    xmlns:foo=\"http://schemas.android.com/apk/res-auto\" \n" +
                                            "    android:id=\"@+id/layout\"\n" +
                                            "    android:name=\"@style/mystyle\"\n" +
                                            "    name=\"@style/mystyle\"\n" +
                                            "    style=\"@style/mystyle\"\n" +
                                            "    include=\"@style/mystyle\"\n" +
                                            "    android:layout_width=\"match_parent\"\n" +
                                            "    android:layout_height=\"match_parent\"\n" +
                                            "    android:layout_gravity=\"match_parent\"\n" +
                                            "    android:width=\"match_parent\"\n" +
                                            "    android:height=\"match_parent\"\n" +
                                            "    android:gravity=\"center\"\n" +
                                            "    android:text=\"test\"\n" +
                                            "    app:gravity=\"this\"\n" +
                                            "    app:layout_above=\"this\"\n" +
                                            "    app:text=\"this\"\n" +
                                            "    foo:bar=\"this\" />");
        lint().files(source)
              .run()
              .expectClean();
    }

    public void testValidWithoutStyle() throws Exception {
        final TestFile source = xml("res/layout/layout1.xml",
                                    "<?xml version=\"1.0\" encoding=\"utf-8\"?>\n" +
                                            "<RelativeLayout \n" +
                                            "    xmlns:android=\"http://schemas.android.com/apk/res/android\" \n" +
                                            "    xmlns:app=\"http://schemas.android.com/apk/res-auto\" \n" +
                                            "    xmlns:foo=\"http://schemas.android.com/apk/res-auto\" \n" +
                                            "    android:id=\"@+id/layout\"\n" +
                                            "    android:name=\"@style/mystyle\"\n" +
                                            "    name=\"@style/mystyle\"\n" +
                                            "    include=\"@style/mystyle\"\n" +
                                            "    android:layout_width=\"match_parent\"\n" +
                                            "    android:layout_height=\"match_parent\"\n" +
                                            "    android:gravity=\"center\"\n" +
                                            "    android:text=\"test\"\n" +
                                            "    app:gravity=\"this\"\n" +
                                            "    app:layout_above=\"this\"\n" +
                                            "    app:text=\"this\"\n" +
                                            "    foo:bar=\"this\" />");
        lint().files(source)
              .run()
              .expectClean();
    }

    public void testValidWithoutId() throws Exception {
        final TestFile source = xml("res/layout/layout1.xml",
                                    "<?xml version=\"1.0\" encoding=\"utf-8\"?>\n" +
                                            "<RelativeLayout \n" +
                                            "    xmlns:android=\"http://schemas.android.com/apk/res/android\" \n" +
                                            "    xmlns:app=\"http://schemas.android.com/apk/res-auto\" \n" +
                                            "    xmlns:foo=\"http://schemas.android.com/apk/res-auto\" \n" +
                                            "    android:name=\"@style/mystyle\"\n" +
                                            "    name=\"@style/mystyle\"\n" +
                                            "    style=\"@style/mystyle\"\n" +
                                            "    include=\"@style/mystyle\"\n" +
                                            "    android:layout_width=\"match_parent\"\n" +
                                            "    android:layout_height=\"match_parent\"\n" +
                                            "    android:gravity=\"center\"\n" +
                                            "    android:text=\"test\"\n" +
                                            "    app:gravity=\"this\"\n" +
                                            "    app:layout_above=\"this\"\n" +
                                            "    app:text=\"this\"\n" +
                                            "    foo:bar=\"this\" />");
        lint().files(source)
              .run()
              .expectClean();
    }

    public void testValidWithoutIdAndStyle() throws Exception {
        final TestFile source = xml("res/layout/layout1.xml",
                                    "<?xml version=\"1.0\" encoding=\"utf-8\"?>\n" +
                                            "<RelativeLayout \n" +
                                            "    xmlns:android=\"http://schemas.android.com/apk/res/android\" \n" +
                                            "    xmlns:app=\"http://schemas.android.com/apk/res-auto\" \n" +
                                            "    xmlns:foo=\"http://schemas.android.com/apk/res-auto\" \n" +
                                            "    android:name=\"@style/mystyle\"\n" +
                                            "    name=\"@style/mystyle\"\n" +
                                            "    include=\"@style/mystyle\"\n" +
                                            "    android:layout_width=\"match_parent\"\n" +
                                            "    android:layout_height=\"match_parent\"\n" +
                                            "    android:gravity=\"center\"\n" +
                                            "    android:text=\"test\"\n" +
                                            "    app:gravity=\"this\"\n" +
                                            "    app:layout_above=\"this\"\n" +
                                            "    app:text=\"this\"\n" +
                                            "    foo:bar=\"this\" />");
        lint().files(source)
              .run()
              .expectClean();
    }

    public void testValidWithoutEmptySpecial() throws Exception {
        final TestFile source = xml("res/layout/layout1.xml",
                                    "<?xml version=\"1.0\" encoding=\"utf-8\"?>\n" +
                                            "<RelativeLayout \n" +
                                            "    xmlns:android=\"http://schemas.android.com/apk/res/android\" \n" +
                                            "    xmlns:app=\"http://schemas.android.com/apk/res-auto\" \n" +
                                            "    xmlns:foo=\"http://schemas.android.com/apk/res-auto\" \n" +
                                            "    android:layout_width=\"match_parent\"\n" +
                                            "    android:layout_height=\"match_parent\"\n" +
                                            "    android:gravity=\"center\"\n" +
                                            "    android:text=\"test\"\n" +
                                            "    app:gravity=\"this\"\n" +
                                            "    app:layout_above=\"this\"\n" +
                                            "    app:text=\"this\"\n" +
                                            "    foo:bar=\"this\" />");
        lint().files(source)
              .run()
              .expectClean();
    }

    public void testInvalid() throws Exception {
        final TestFile source = xml("res/layout/layout1.xml",
                                    "<?xml version=\"1.0\" encoding=\"utf-8\"?>\n" +
                                            "<RelativeLayout \n" +
                                            "    xmlns:android=\"http://schemas.android.com/apk/res/android\" \n" +
                                            "    xmlns:app=\"http://schemas.android.com/apk/res-auto\" \n" +
                                            "    xmlns:foo=\"http://schemas.android.com/apk/res-auto\" \n" +
                                            "    style=\"@style/mystyle\"\n" +
                                            "    android:id=\"@+id/layout\"\n" +
                                            "    name=\"@style/mystyle\"\n" +
                                            "    android:layout_width=\"match_parent\"\n" +
                                            "    android:name=\"@style/mystyle\"\n" +
                                            "    android:layout_height=\"match_parent\"\n" +
                                            "    app:layout_above=\"this\"\n" +
                                            "    android:gravity=\"center\"\n" +
                                            "    include=\"@style/mystyle\"\n" +
                                            "    android:text=\"test\"\n" +
                                            "    app:gravity=\"this\"\n" +
                                            "    app:text=\"this\"\n" +
                                            "    foo:bar=\"this\" />");
        lint().files(source)
              .run()
              .expect("res/layout/layout1.xml:2: Information: Attributes out of order.\n" +
                              "Expected:\n" +
                              "xmlns:android=\"http://schemas.android.com/apk/res/android\"\n" +
                              "xmlns:app=\"http://schemas.android.com/apk/res-auto\"\n" +
                              "xmlns:foo=\"http://schemas.android.com/apk/res-auto\"\n" +
                              "android:id=\"@+id/layout\"\n" +
                              "android:name=\"@style/mystyle\"\n" +
                              "name=\"@style/mystyle\"\n" +
                              "style=\"@style/mystyle\"\n" +
                              "include=\"@style/mystyle\"\n" +
                              "android:layout_width=\"match_parent\"\n" +
                              "android:layout_height=\"match_parent\"\n" +
                              "android:gravity=\"center\"\n" +
                              "android:text=\"test\"\n" +
                              "app:gravity=\"this\"\n" +
                              "app:layout_above=\"this\"\n" +
                              "app:text=\"this\"\n" +
                              "foo:bar=\"this\"\n" +
                              "\n" +
                              " [sc.XmlOrder]\n" +
                              "<RelativeLayout \n" +
                              "^\n" +
                              "0 errors, 1 warnings\n");
    }

    public void testValidChild() throws Exception {
        final TestFile source = xml("res/layout/layout1.xml",
                                    "<?xml version=\"1.0\" encoding=\"utf-8\"?>\n" +
                                            "<RelativeLayout \n" +
                                            "    xmlns:android=\"http://schemas.android.com/apk/res/android\" \n" +
                                            "    xmlns:app=\"http://schemas.android.com/apk/res-auto\" \n" +
                                            "    xmlns:foo=\"http://schemas.android.com/apk/res-auto\" \n" +
                                            "    android:layout_width=\"match_parent\"\n" +
                                            "    android:layout_height=\"match_parent\">\n" +
                                            "    \n" +
                                            "    <View\n" +
                                            "        android:id=\"@+id/layout\"\n" +
                                            "        android:name=\"@style/mystyle\"\n" +
                                            "        name=\"@style/mystyle\"\n" +
                                            "        style=\"@style/mystyle\"\n" +
                                            "        include=\"@style/mystyle\"\n" +
                                            "        android:layout_width=\"match_parent\"\n" +
                                            "        android:layout_height=\"match_parent\"\n" +
                                            "        android:gravity=\"center\"\n" +
                                            "        android:text=\"test\"\n" +
                                            "        app:gravity=\"this\"\n" +
                                            "        app:layout_above=\"this\"\n" +
                                            "        app:text=\"this\"\n" +
                                            "        foo:bar=\"this\" />\n" +
                                            "    \n" +
                                            "</RelativeLayout>");
        lint().files(source)
              .run()
              .expectClean();
    }

    public void testInvalidChild() throws Exception {
        final TestFile source = xml("res/layout/layout1.xml",
                                    "<?xml version=\"1.0\" encoding=\"utf-8\"?>\n" +
                                            "<RelativeLayout \n" +
                                            "    xmlns:android=\"http://schemas.android.com/apk/res/android\" \n" +
                                            "    xmlns:app=\"http://schemas.android.com/apk/res-auto\" \n" +
                                            "    xmlns:foo=\"http://schemas.android.com/apk/res-auto\" \n" +
                                            "    android:layout_width=\"match_parent\"\n" +
                                            "    android:layout_height=\"match_parent\">\n" +
                                            "    \n" +
                                            "    <View\n" +
                                            "        include=\"@style/mystyle\"\n" +
                                            "        android:name=\"@style/mystyle\"\n" +
                                            "        android:id=\"@+id/layout\"\n" +
                                            "        name=\"@style/mystyle\"\n" +
                                            "        style=\"@style/mystyle\"\n" +
                                            "        android:layout_height=\"match_parent\"\n" +
                                            "        android:gravity=\"center\"\n" +
                                            "        android:layout_width=\"match_parent\"\n" +
                                            "        app:layout_above=\"this\"\n" +
                                            "        app:gravity=\"this\"\n" +
                                            "        android:text=\"test\"\n" +
                                            "        app:text=\"this\"\n" +
                                            "        foo:bar=\"this\" />\n" +
                                            "    \n" +
                                            "</RelativeLayout>");
        lint().files(source)
              .run()
              .expect("res/layout/layout1.xml:9: Information: Attributes out of order.\n" +
                              "Expected:\n" +
                              "android:id=\"@+id/layout\"\n" +
                              "android:name=\"@style/mystyle\"\n" +
                              "name=\"@style/mystyle\"\n" +
                              "style=\"@style/mystyle\"\n" +
                              "include=\"@style/mystyle\"\n" +
                              "android:layout_width=\"match_parent\"\n" +
                              "android:layout_height=\"match_parent\"\n" +
                              "android:gravity=\"center\"\n" +
                              "android:text=\"test\"\n" +
                              "app:gravity=\"this\"\n" +
                              "app:layout_above=\"this\"\n" +
                              "app:text=\"this\"\n" +
                              "foo:bar=\"this\"\n" +
                              "\n" +
                              " [sc.XmlOrder]\n" +
                              "    <View\n" +
                              "    ^\n" +
                              "0 errors, 1 warnings\n");
    }

    public void testValidSingle() throws Exception {
        final TestFile source = xml("res/layout/layout1.xml",
                                    "<?xml version=\"1.0\" encoding=\"utf-8\"?>\n" +
                                            "<include layout=\"@layout/test\" />");
        lint().files(source)
              .run()
              .expectClean();
    }

    public void testValidSingleNoAttributes() throws Exception {
        final TestFile source = xml("res/layout/layout1.xml",
                                    "<?xml version=\"1.0\" encoding=\"utf-8\"?>\n" +
                                            "<View />");
        lint().files(source)
              .run()
              .expectClean();
    }

    public void testInvalidSingle() throws Exception {
        final TestFile source = xml("res/layout/layout1.xml",
                                    "<?xml version=\"1.0\" encoding=\"utf-8\"?>\n" +
                                            "<include \n" +
                                            "    layout=\"@layout/test\" />");
        lint().files(source)
              .run()
              .expect("res/layout/layout1.xml:2: Information: Single line attributes should be on the same line as the tag. [sc.XmlAttrWrap]\n" +
                              "<include \n" +
                              "^\n" +
                              "0 errors, 1 warnings\n");
    }

    public void testInalidSingleNoAttributes() throws Exception {
        final TestFile source = xml("res/layout/layout1.xml",
                                    "<?xml version=\"1.0\" encoding=\"utf-8\"?>\n" +
                                            "<View \n" +
                                            "    />");
        lint().files(source)
              .run()
              .expect("res/layout/layout1.xml:2: Information: XML tags should be closed on the final attribute line. [sc.XmlAttrWrap]\n" +
                              "<View \n" +
                              " ^\n" +
                              "0 errors, 1 warnings\n");
    }

    public void testTrailingLineBreakMissing() throws Exception {
        final TestFile source = xml("res/layout/layout1.xml",
                                    "<?xml version=\"1.0\" encoding=\"utf-8\"?>\n" +
                                            "<include layout=\"@layout/test\" />");
        lint().files(source)
              .run()
              .expectClean(); // Clean since covered by checkstyle
    }

    public void testTrailingLineBreakSingle() throws Exception {
        final TestFile source = xml("res/layout/layout1.xml",
                                    "<?xml version=\"1.0\" encoding=\"utf-8\"?>\n" +
                                            "<include layout=\"@layout/test\" />\n");
        lint().files(source)
              .run()
              .expectClean();
    }

    public void testTrailingLineBreakMany() throws Exception {
        final TestFile source = xml("res/layout/layout1.xml",
                                    "<?xml version=\"1.0\" encoding=\"utf-8\"?>\n" +
                                            "<include layout=\"@layout/test\" />\n" +
                                            "\n" +
                                            "\n");
        lint().files(source)
              .run()
              .expect("res/layout/layout1.xml:3: Information: Lines 3-5 should be replaced by a single blank line. [sc.XmlLineBreak]\n" +
                              "\n" +
                              "^\n" +
                              "0 errors, 1 warnings\n");
    }

    public void testTrailingLineBreakTwo() throws Exception {
        final TestFile source = xml("res/layout/layout1.xml",
                                    "<?xml version=\"1.0\" encoding=\"utf-8\"?>\n" +
                                            "<include layout=\"@layout/test\" />\n" +
                                            "\n");
        lint().files(source)
              .run()
              .expect("res/layout/layout1.xml:3: Information: Lines 3-4 should be replaced by a single blank line. [sc.XmlLineBreak]\n" +
                              "\n" +
                              "^\n" +
                              "0 errors, 1 warnings\n");
    }

    public void testTrailingLineBreakBeforeDocumentSingle() throws Exception {
        final TestFile source = xml("res/layout/layout1.xml",
                                    "<?xml version=\"1.0\" encoding=\"utf-8\"?>\n" +
                                            "\n" +
                                            "<include layout=\"@layout/test\" />\n");
        lint().files(source)
              .run()
              .expect("res/layout/layout1.xml:2: Information: Line 2 should be removed. [sc.XmlLineBreak]\n" +
                              "\n" +
                              "^\n" +
                              "0 errors, 1 warnings\n");
    }

    public void testTrailingLineBreakBeforeDocumentMany() throws Exception {
        final TestFile source = xml("res/layout/layout1.xml",
                                    "<?xml version=\"1.0\" encoding=\"utf-8\"?>\n" +
                                            "\n" +
                                            "\n" +
                                            "\n" +
                                            "<include layout=\"@layout/test\" />\n");
        lint().files(source)
              .run()
              .expect("res/layout/layout1.xml:2: Information: Lines 2-4 should be removed. [sc.XmlLineBreak]\n" +
                              "\n" +
                              "^\n" +
                              "0 errors, 1 warnings\n");
    }

    public void testMissingLineBreakBefore() throws Exception {
        final TestFile source = xml("res/layout/layout1.xml",
                                    "<?xml version=\"1.0\" encoding=\"utf-8\"?>\n" +
                                            "<RelativeLayout \n" +
                                            "    xmlns:android=\"http://schemas.android.com/apk/res/android\" \n" +
                                            "    android:layout_width=\"match_parent\"\n" +
                                            "    android:layout_height=\"match_parent\">\n" +
                                            "    <View\n" +
                                            "        android:layout_width=\"match_parent\"\n" +
                                            "        android:layout_height=\"match_parent\" />\n" +
                                            "    \n" +
                                            "</RelativeLayout>");
        lint().files(source)
              .run()
              .expect("res/layout/layout1.xml:6: Information: Elements should be separated by a single linebreak. [sc.XmlLineBreak]\n" +
                              "    <View\n" +
                              "^\n" +
                              "0 errors, 1 warnings\n");
    }

    public void testMissingLineBreakAfter() throws Exception {
        final TestFile source = xml("res/layout/layout1.xml",
                                    "<?xml version=\"1.0\" encoding=\"utf-8\"?>\n" +
                                            "<RelativeLayout \n" +
                                            "    xmlns:android=\"http://schemas.android.com/apk/res/android\" \n" +
                                            "    android:layout_width=\"match_parent\"\n" +
                                            "    android:layout_height=\"match_parent\">\n" +
                                            "    \n" +
                                            "    <View\n" +
                                            "        android:layout_width=\"match_parent\"\n" +
                                            "        android:layout_height=\"match_parent\" />\n" +
                                            "</RelativeLayout>");
        lint().files(source)
              .run()
              .expect("res/layout/layout1.xml:10: Information: Elements should be separated by a single linebreak. [sc.XmlLineBreak]\n" +
                              "</RelativeLayout>\n" +
                              "^\n" +
                              "0 errors, 1 warnings\n");
    }

    public void testMissingLineBreakBoth() throws Exception {
        final TestFile source = xml("res/layout/layout1.xml",
                                    "<?xml version=\"1.0\" encoding=\"utf-8\"?>\n" +
                                            "<RelativeLayout \n" +
                                            "    xmlns:android=\"http://schemas.android.com/apk/res/android\" \n" +
                                            "    android:layout_width=\"match_parent\"\n" +
                                            "    android:layout_height=\"match_parent\">\n" +
                                            "    <View\n" +
                                            "        android:layout_width=\"match_parent\"\n" +
                                            "        android:layout_height=\"match_parent\" />\n" +
                                            "</RelativeLayout>");
        lint().files(source)
              .run()
              .expect("res/layout/layout1.xml:6: Information: Elements should be separated by a single linebreak. [sc.XmlLineBreak]\n" +
                              "    <View\n" +
                              "^\n" +
                              "res/layout/layout1.xml:9: Information: Elements should be separated by a single linebreak. [sc.XmlLineBreak]\n" +
                              "</RelativeLayout>\n" +
                              "^\n" +
                              "0 errors, 2 warnings\n");
    }

    public void testMissingLineBreakNested() throws Exception {
        final TestFile source = xml("res/layout/layout1.xml",
                                    "<?xml version=\"1.0\" encoding=\"utf-8\"?>\n" +
                                            "<RelativeLayout \n" +
                                            "    xmlns:android=\"http://schemas.android.com/apk/res/android\" \n" +
                                            "    android:layout_width=\"match_parent\"\n" +
                                            "    android:layout_height=\"match_parent\">\n" +
                                            "    <FrameLayout\n" +
                                            "        android:layout_width=\"match_parent\"\n" +
                                            "        android:layout_height=\"match_parent\">\n" +
                                            "        <View\n" +
                                            "            android:layout_width=\"match_parent\"\n" +
                                            "            android:layout_height=\"match_parent\" />\n" +
                                            "    </FrameLayout>\n" +
                                            "</RelativeLayout>");
        lint().files(source)
              .run()
              .expect("res/layout/layout1.xml:6: Information: Elements should be separated by a single linebreak. [sc.XmlLineBreak]\n" +
                              "    <FrameLayout\n" +
                              "^\n" +
                              "res/layout/layout1.xml:9: Information: Elements should be separated by a single linebreak. [sc.XmlLineBreak]\n" +
                              "        <View\n" +
                              "^\n" +
                              "res/layout/layout1.xml:12: Information: Elements should be separated by a single linebreak. [sc.XmlLineBreak]\n" +
                              "    </FrameLayout>\n" +
                              "^\n" +
                              "0 errors, 3 warnings\n");
    }

    public void testTooManyLineBreaksBeforeSingle() throws Exception {
        final TestFile source = xml("res/layout/layout1.xml",
                                    "<?xml version=\"1.0\" encoding=\"utf-8\"?>\n" +
                                            "<RelativeLayout \n" +
                                            "    xmlns:android=\"http://schemas.android.com/apk/res/android\" \n" +
                                            "    android:layout_width=\"match_parent\"\n" +
                                            "    android:layout_height=\"match_parent\">\n" +
                                            "    \n" +
                                            "    \n" +
                                            "    <View\n" +
                                            "        android:layout_width=\"match_parent\"\n" +
                                            "        android:layout_height=\"match_parent\" />\n" +
                                            "    \n" +
                                            "</RelativeLayout>");
        lint().files(source)
              .run()
              .expect("res/layout/layout1.xml:6: Information: Lines 6-7 should be replaced by a single blank line. [sc.XmlLineBreak]\n" +
                              "    \n" +
                              "^\n" +
                              "0 errors, 1 warnings\n");
    }

    public void testTooManyLineBreaksBeforeMultiple() throws Exception {
        final TestFile source = xml("res/layout/layout1.xml",
                                    "<?xml version=\"1.0\" encoding=\"utf-8\"?>\n" +
                                            "<RelativeLayout \n" +
                                            "    xmlns:android=\"http://schemas.android.com/apk/res/android\" \n" +
                                            "    android:layout_width=\"match_parent\"\n" +
                                            "    android:layout_height=\"match_parent\">\n" +
                                            "    \n" +
                                            "    \n" +
                                            "    \n" +
                                            "    <View\n" +
                                            "        android:layout_width=\"match_parent\"\n" +
                                            "        android:layout_height=\"match_parent\" />\n" +
                                            "    \n" +
                                            "</RelativeLayout>");
        lint().files(source)
              .run()
              .expect("res/layout/layout1.xml:6: Information: Lines 6-8 should be replaced by a single blank line. [sc.XmlLineBreak]\n" +
                              "    \n" +
                              "^\n" +
                              "0 errors, 1 warnings\n");
    }

    public void testTooManyLineBreaksAfterSingle() throws Exception {
        final TestFile source = xml("res/layout/layout1.xml",
                                    "<?xml version=\"1.0\" encoding=\"utf-8\"?>\n" +
                                            "<RelativeLayout \n" +
                                            "    xmlns:android=\"http://schemas.android.com/apk/res/android\" \n" +
                                            "    android:layout_width=\"match_parent\"\n" +
                                            "    android:layout_height=\"match_parent\">\n" +
                                            "    \n" +
                                            "    <View\n" +
                                            "        android:layout_width=\"match_parent\"\n" +
                                            "        android:layout_height=\"match_parent\" />\n" +
                                            "    \n" +
                                            "    \n" +
                                            "</RelativeLayout>");
        lint().files(source)
              .run()
              .expect("res/layout/layout1.xml:10: Information: Lines 10-11 should be replaced by a single blank line. [sc.XmlLineBreak]\n" +
                              "    \n" +
                              "^\n" +
                              "0 errors, 1 warnings\n");
    }

    public void testTooManyLineBreaksAfterMultiple() throws Exception {
        final TestFile source = xml("res/layout/layout1.xml",
                                    "<?xml version=\"1.0\" encoding=\"utf-8\"?>\n" +
                                            "<RelativeLayout \n" +
                                            "    xmlns:android=\"http://schemas.android.com/apk/res/android\" \n" +
                                            "    android:layout_width=\"match_parent\"\n" +
                                            "    android:layout_height=\"match_parent\">\n" +
                                            "    \n" +
                                            "    <View\n" +
                                            "        android:layout_width=\"match_parent\"\n" +
                                            "        android:layout_height=\"match_parent\" />\n" +
                                            "    \n" +
                                            "    \n" +
                                            "    \n" +
                                            "</RelativeLayout>");
        lint().files(source)
              .run()
              .expect("res/layout/layout1.xml:10: Information: Lines 10-12 should be replaced by a single blank line. [sc.XmlLineBreak]\n" +
                              "    \n" +
                              "^\n" +
                              "0 errors, 1 warnings\n");
    }

    public void testTooManyLineBreaksBoth() throws Exception {
        final TestFile source = xml("res/layout/layout1.xml",
                                    "<?xml version=\"1.0\" encoding=\"utf-8\"?>\n" +
                                            "<RelativeLayout \n" +
                                            "    xmlns:android=\"http://schemas.android.com/apk/res/android\" \n" +
                                            "    android:layout_width=\"match_parent\"\n" +
                                            "    android:layout_height=\"match_parent\">\n" +
                                            "    \n" +
                                            "    \n" +
                                            "    \n" +
                                            "    <View\n" +
                                            "        android:layout_width=\"match_parent\"\n" +
                                            "        android:layout_height=\"match_parent\" />\n" +
                                            "    \n" +
                                            "    \n" +
                                            "</RelativeLayout>");
        lint().files(source)
              .run()
              .expect("res/layout/layout1.xml:6: Information: Lines 6-8 should be replaced by a single blank line. [sc.XmlLineBreak]\n" +
                              "    \n" +
                              "^\n" +
                              "res/layout/layout1.xml:12: Information: Lines 12-13 should be replaced by a single blank line. [sc.XmlLineBreak]\n" +
                              "    \n" +
                              "^\n" +
                              "0 errors, 2 warnings\n");
    }

    public void testTooManyLineBreaksNested() throws Exception {
        final TestFile source = xml("res/layout/layout1.xml",
                                    "<?xml version=\"1.0\" encoding=\"utf-8\"?>\n" +
                                            "<RelativeLayout \n" +
                                            "    xmlns:android=\"http://schemas.android.com/apk/res/android\" \n" +
                                            "    android:layout_width=\"match_parent\"\n" +
                                            "    android:layout_height=\"match_parent\">\n" +
                                            "    \n" +
                                            "    \n" +
                                            "    <FrameLayout\n" +
                                            "        android:layout_width=\"match_parent\"\n" +
                                            "        android:layout_height=\"match_parent\">\n" +
                                            "        \n" +
                                            "        \n" +
                                            "        \n" +
                                            "        <View\n" +
                                            "            android:layout_width=\"match_parent\"\n" +
                                            "            android:layout_height=\"match_parent\" />\n" +
                                            "        \n" +
                                            "        \n" +
                                            "        \n" +
                                            "        \n" +
                                            "    </FrameLayout>\n" +
                                            "    \n" +
                                            "    \n" +
                                            "</RelativeLayout>");
        lint().files(source)
              .run()
              .expect("res/layout/layout1.xml:6: Information: Lines 6-7 should be replaced by a single blank line. [sc.XmlLineBreak]\n" +
                              "    \n" +
                              "^\n" +
                              "res/layout/layout1.xml:11: Information: Lines 11-13 should be replaced by a single blank line. [sc.XmlLineBreak]\n" +
                              "        \n" +
                              "^\n" +
                              "res/layout/layout1.xml:17: Information: Lines 17-20 should be replaced by a single blank line. [sc.XmlLineBreak]\n" +
                              "        \n" +
                              "^\n" +
                              "res/layout/layout1.xml:22: Information: Lines 22-23 should be replaced by a single blank line. [sc.XmlLineBreak]\n" +
                              "    \n" +
                              "^\n" +
                              "0 errors, 4 warnings\n");
    }

    public void testMissingAttributeStart() throws Exception {
        final TestFile source = xml("res/layout/layout1.xml",
                                    "<?xml version=\"1.0\" encoding=\"utf-8\"?>\n" +
                                            "<RelativeLayout xmlns:android=\"http://schemas.android.com/apk/res/android\" \n" +
                                            "    android:layout_width=\"match_parent\" \n" +
                                            "    android:layout_height=\"match_parent\" />");
        lint().files(source)
              .run()
              .expect("res/layout/layout1.xml:2: Information: Each attribute must be followed by a linebreak. [sc.XmlAttrWrap]\n" +
                              "<RelativeLayout xmlns:android=\"http://schemas.android.com/apk/res/android\" \n" +
                              "                ~~~~~~~~~~~~~\n" +
                              "0 errors, 1 warnings\n");
    }

    public void testMissingAttributeWrap() throws Exception {
        final TestFile source = xml("res/layout/layout1.xml",
                                    "<?xml version=\"1.0\" encoding=\"utf-8\"?>\n" +
                                            "<RelativeLayout \n" +
                                            "    xmlns:android=\"http://schemas.android.com/apk/res/android\" \n" +
                                            "    android:layout_width=\"match_parent\" android:layout_height=\"match_parent\" />");
        lint().files(source)
              .run()
              .expect("res/layout/layout1.xml:4: Information: Each attribute must be followed by a linebreak. [sc.XmlAttrWrap]\n" +
                              "    android:layout_width=\"match_parent\" android:layout_height=\"match_parent\" />\n" +
                              "                                        ~~~~~~~~~~~~~~~~~~~~~\n" +
                              "0 errors, 1 warnings\n");
    }

    public void testTooManyAttributeWrapEnd() throws Exception {
        final TestFile source = xml("res/layout/layout1.xml",
                                    "<?xml version=\"1.0\" encoding=\"utf-8\"?>\n" +
                                            "<RelativeLayout \n" +
                                            "    xmlns:android=\"http://schemas.android.com/apk/res/android\" \n" +
                                            "    android:layout_width=\"match_parent\" \n" +
                                            "    android:layout_height=\"match_parent\" \n" +
                                            "    />");
        lint().files(source)
              .run()
              .expect("res/layout/layout1.xml:5: Information: XML tags should be closed on the final attribute line. [sc.XmlAttrWrap]\n" +
                              "    android:layout_height=\"match_parent\" \n" +
                              "    ~~~~~~~~~~~~~~~~~~~~~\n" +
                              "0 errors, 1 warnings\n");
    }

    public void testTooManyAttributeWrapEndMultiple() throws Exception {
        final TestFile source = xml("res/layout/layout1.xml",
                                    "<?xml version=\"1.0\" encoding=\"utf-8\"?>\n" +
                                            "<RelativeLayout \n" +
                                            "    xmlns:android=\"http://schemas.android.com/apk/res/android\" \n" +
                                            "    android:layout_width=\"match_parent\" \n" +
                                            "    android:layout_height=\"match_parent\" \n" +
                                            "    \n" +
                                            "    \n" +
                                            "    />");
        lint().files(source)
              .run()
              .expect("res/layout/layout1.xml:6: Information: XML tags should be closed on the final attribute line. [sc.XmlLineBreak]\n" +
                              "    \n" +
                              "^\n" +
                              "0 errors, 1 warnings\n");
    }

    public void testTooManyAttributeWrapEndWithChild() throws Exception {
        final TestFile source = xml("res/layout/layout1.xml",
                                    "<?xml version=\"1.0\" encoding=\"utf-8\"?>\n" +
                                            "<RelativeLayout \n" +
                                            "    xmlns:android=\"http://schemas.android.com/apk/res/android\" \n" +
                                            "    android:layout_width=\"match_parent\" \n" +
                                            "    android:layout_height=\"match_parent\"\n" +
                                            "    >\n" +
                                            "\n" +
                                            "    <View\n" +
                                            "        android:layout_width=\"match_parent\"\n" +
                                            "        android:layout_height=\"match_parent\" />\n" +
                                            "\n" +
                                            "</RelativeLayout>");
        lint().files(source)
              .run()
              .expect("res/layout/layout1.xml:5: Information: XML tags should be closed on the final attribute line. [sc.XmlAttrWrap]\n" +
                              "    android:layout_height=\"match_parent\"\n" +
                              "    ~~~~~~~~~~~~~~~~~~~~~\n" +
                              "0 errors, 1 warnings\n");
    }

    public void testTooManyAttributeWrapEndMultipleWithChild() throws Exception {
        final TestFile source = xml("res/layout/layout1.xml",
                                    "<?xml version=\"1.0\" encoding=\"utf-8\"?>\n" +
                                            "<RelativeLayout \n" +
                                            "    xmlns:android=\"http://schemas.android.com/apk/res/android\" \n" +
                                            "    android:layout_width=\"match_parent\" \n" +
                                            "    android:layout_height=\"match_parent\"\n" +
                                            "    \n" +
                                            "    \n" +
                                            "    >\n" +
                                            "\n" +
                                            "    <View\n" +
                                            "        android:layout_width=\"match_parent\"\n" +
                                            "        android:layout_height=\"match_parent\" />\n" +
                                            "\n" +
                                            "</RelativeLayout>");
        lint().files(source)
              .run()
              .expect("res/layout/layout1.xml:6: Information: XML tags should be closed on the final attribute line. [sc.XmlLineBreak]\n" +
                              "    \n" +
                              "^\n" +
                              "0 errors, 1 warnings\n");
    }

    public void testTooManyAttributeWrapMiddle() throws Exception {
        final TestFile source = xml("res/layout/layout1.xml",
                                    "<?xml version=\"1.0\" encoding=\"utf-8\"?>\n" +
                                            "<RelativeLayout \n" +
                                            "    xmlns:android=\"http://schemas.android.com/apk/res/android\" \n" +
                                            "    android:layout_width=\"match_parent\" \n" +
                                            "    \n" +
                                            "    android:layout_height=\"match_parent\" />");
        lint().files(source)
              .run()
              .expect("res/layout/layout1.xml:5: Information: Line 5 should be removed. [sc.XmlLineBreak]\n" +
                              "    \n" +
                              "^\n" +
                              "0 errors, 1 warnings\n");
    }

    public void testTooManyAttributeWrapStart() throws Exception {
        final TestFile source = xml("res/layout/layout1.xml",
                                    "<?xml version=\"1.0\" encoding=\"utf-8\"?>\n" +
                                            "<RelativeLayout \n" +
                                            "    \n" +
                                            "    xmlns:android=\"http://schemas.android.com/apk/res/android\" \n" +
                                            "    android:layout_width=\"match_parent\" \n" +
                                            "    android:layout_height=\"match_parent\" />");
        lint().files(source)
              .run()
              .expect("res/layout/layout1.xml:3: Information: Line 3 should be removed. [sc.XmlLineBreak]\n" +
                              "    \n" +
                              "^\n" +
                              "0 errors, 1 warnings\n");
    }

    public void testTooManyAttributeWrapStartMultiple() throws Exception {
        final TestFile source = xml("res/layout/layout1.xml",
                                    "<?xml version=\"1.0\" encoding=\"utf-8\"?>\n" +
                                            "<RelativeLayout \n" +
                                            "    \n" +
                                            "    \n" +
                                            "    xmlns:android=\"http://schemas.android.com/apk/res/android\" \n" +
                                            "    android:layout_width=\"match_parent\" \n" +
                                            "    android:layout_height=\"match_parent\" />");
        lint().files(source)
              .run()
              .expect("res/layout/layout1.xml:3: Information: Lines 3-4 should be removed. [sc.XmlLineBreak]\n" +
                              "    \n" +
                              "^\n" +
                              "0 errors, 1 warnings\n");
    }

    public void testMissingAttributeWrapInChild() throws Exception {
        final TestFile source = xml("res/layout/layout1.xml",
                                    "<?xml version=\"1.0\" encoding=\"utf-8\"?>\n" +
                                            "<RelativeLayout \n" +
                                            "    xmlns:android=\"http://schemas.android.com/apk/res/android\" \n" +
                                            "    android:layout_width=\"match_parent\"\n" +
                                            "    android:layout_height=\"match_parent\">\n" +
                                            "    \n" +
                                            "    <View\n" +
                                            "        android:layout_width=\"match_parent\" android:layout_height=\"match_parent\" />\n" +
                                            "    \n" +
                                            "</RelativeLayout>");
        lint().files(source)
              .run()
              .expect("res/layout/layout1.xml:8: Information: Each attribute must be followed by a linebreak. [sc.XmlAttrWrap]\n" +
                              "        android:layout_width=\"match_parent\" android:layout_height=\"match_parent\" />\n" +
                              "                                            ~~~~~~~~~~~~~~~~~~~~~\n" +
                              "0 errors, 1 warnings\n");
    }

    public void testMissingWhitespace() throws Exception {
        final TestFile source = xml("res/layout/layout1.xml",
                                    "<?xml version=\"1.0\" encoding=\"utf-8\"?>\n" +
                                            "<RelativeLayout \n" +
                                            "    xmlns:android=\"http://schemas.android.com/apk/res/android\" \n" +
                                            "    android:layout_width=\"match_parent\"\n" +
                                            "    android:layout_height=\"match_parent\"/>");
        lint().files(source)
              .run()
              .expect("res/layout/layout1.xml:5: Information: Missing whitespace before closing tag. [sc.XmlWhiteSpace]\n" +
                              "    android:layout_height=\"match_parent\"/>\n" +
                              "                                        ^\n" +
                              "0 errors, 1 warnings\n");
    }

    public void testNoWhitespaceIfBody() throws Exception {
        final TestFile source = xml("res/layout/layout1.xml",
                                    "<?xml version=\"1.0\" encoding=\"utf-8\"?>\n" +
                                            "<RelativeLayout \n" +
                                            "    xmlns:android=\"http://schemas.android.com/apk/res/android\" \n" +
                                            "    android:layout_width=\"match_parent\"\n" +
                                            "    android:layout_height=\"match_parent\">\n" +
                                            "\n" +
                                            "    <View />\n" +
                                            "\n" +
                                            "</RelativeLayout>");
        lint().files(source)
              .run()
              .expectClean();
    }

    public void testSingleWhitespacesIfBody() throws Exception {
        final TestFile source = xml("res/layout/layout1.xml",
                                    "<?xml version=\"1.0\" encoding=\"utf-8\"?>\n" +
                                            "<RelativeLayout \n" +
                                            "    xmlns:android=\"http://schemas.android.com/apk/res/android\" \n" +
                                            "    android:layout_width=\"match_parent\"\n" +
                                            "    android:layout_height=\"match_parent\" >\n" +
                                            "\n" +
                                            "    <View />\n" +
                                            "\n" +
                                            "</RelativeLayout>");
        lint().files(source)
              .run()
              .expect("res/layout/layout1.xml:5: Information: Remove whitespace before closing tag. [sc.XmlWhiteSpace]\n" +
                              "    android:layout_height=\"match_parent\" >\n" +
                              "                                        ~\n" +
                              "0 errors, 1 warnings\n");
    }

    public void testManyWhitespacesIfBody() throws Exception {
        final TestFile source = xml("res/layout/layout1.xml",
                                    "<?xml version=\"1.0\" encoding=\"utf-8\"?>\n" +
                                            "<RelativeLayout \n" +
                                            "    xmlns:android=\"http://schemas.android.com/apk/res/android\" \n" +
                                            "    android:layout_width=\"match_parent\"\n" +
                                            "    android:layout_height=\"match_parent\"    >\n" +
                                            "\n" +
                                            "    <View />\n" +
                                            "\n" +
                                            "</RelativeLayout>");
        lint().files(source)
              .run()
              .expect("res/layout/layout1.xml:5: Information: Remove 4 whitespaces before closing tag. [sc.XmlWhiteSpace]\n" +
                              "    android:layout_height=\"match_parent\"    >\n" +
                              "                                        ~~~~\n" +
                              "0 errors, 1 warnings\n");
    }

    public void testTooManyWhitespace() throws Exception {
        final TestFile source = xml("res/layout/layout1.xml",
                                    "<?xml version=\"1.0\" encoding=\"utf-8\"?>\n" +
                                            "<RelativeLayout \n" +
                                            "    xmlns:android=\"http://schemas.android.com/apk/res/android\" \n" +
                                            "    android:layout_width=\"match_parent\"\n" +
                                            "    android:layout_height=\"match_parent\"     />");
        lint().files(source)
              .run()
              .expect("res/layout/layout1.xml:5: Information: 5 whitespaces should be replaced with one whitespace. [sc.XmlWhiteSpace]\n" +
                              "    android:layout_height=\"match_parent\"     />\n" +
                              "                                         ~~~~\n" +
                              "0 errors, 1 warnings\n");
    }

    public void testEmptyBody() throws Exception {
        //noinspection CheckTagEmptyBody
        final TestFile source = xml("res/layout/layout1.xml",
                                    "<?xml version=\"1.0\" encoding=\"utf-8\"?>\n" +
                                            "<RelativeLayout \n" +
                                            "    xmlns:android=\"http://schemas.android.com/apk/res/android\" \n" +
                                            "    android:layout_width=\"match_parent\"\n" +
                                            "    android:layout_height=\"match_parent\"></RelativeLayout>");
        lint().files(source)
              .run()
              .expect("res/layout/layout1.xml:2: Information: XML tags with an empty body should be collapsed. [sc.XmlBody]\n" +
                              "<RelativeLayout \n" +
                              "^\n" +
                              "0 errors, 1 warnings\n");
    }

    public void testEmptyBodyWithWhitespace() throws Exception {
        final TestFile source = xml("res/layout/layout1.xml",
                                    "<?xml version=\"1.0\" encoding=\"utf-8\"?>\n" +
                                            "<RelativeLayout \n" +
                                            "    xmlns:android=\"http://schemas.android.com/apk/res/android\" \n" +
                                            "    android:layout_width=\"match_parent\"\n" +
                                            "    android:layout_height=\"match_parent\"> </RelativeLayout>");
        lint().files(source)
              .run()
              .expect("res/layout/layout1.xml:2: Information: XML tags with an empty body should be collapsed. [sc.XmlBody]\n" +
                              "<RelativeLayout \n" +
                              "^\n" +
                              "0 errors, 1 warnings\n");
    }

    public void testEmptyBodyWithLinebreaks() throws Exception {
        final TestFile source = xml("res/layout/layout1.xml",
                                    "<?xml version=\"1.0\" encoding=\"utf-8\"?>\n" +
                                            "<RelativeLayout \n" +
                                            "    xmlns:android=\"http://schemas.android.com/apk/res/android\" \n" +
                                            "    android:layout_width=\"match_parent\"\n" +
                                            "    android:layout_height=\"match_parent\">\n" +
                                            "\n" +
                                            "</RelativeLayout>");
        lint().files(source)
              .run()
              .expect("res/layout/layout1.xml:2: Information: XML tags with an empty body should be collapsed. [sc.XmlBody]\n" +
                              "<RelativeLayout \n" +
                              "^\n" +
                              "0 errors, 1 warnings\n");
    }
}
