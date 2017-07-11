package com.soundcloud.android.lint.rules.checks;

import static com.android.SdkConstants.ANDROID_NS_NAME;
import static com.android.SdkConstants.ATTR_HEIGHT;
import static com.android.SdkConstants.ATTR_ID;
import static com.android.SdkConstants.ATTR_LAYOUT_HEIGHT;
import static com.android.SdkConstants.ATTR_LAYOUT_RESOURCE_PREFIX;
import static com.android.SdkConstants.ATTR_LAYOUT_WIDTH;
import static com.android.SdkConstants.ATTR_NAME;
import static com.android.SdkConstants.ATTR_STYLE;
import static com.android.SdkConstants.ATTR_WIDTH;
import static com.android.SdkConstants.XMLNS;
import static com.google.common.base.Strings.nullToEmpty;
import static com.intellij.openapi.util.text.StringUtil.isEmptyOrSpaces;
import static com.intellij.openapi.util.text.StringUtil.join;
import static com.intellij.openapi.util.text.StringUtil.toLowerCase;

import com.android.ide.common.blame.SourcePosition;
import com.android.resources.ResourceFolderType;
import com.android.tools.lint.detector.api.Category;
import com.android.tools.lint.detector.api.Detector;
import com.android.tools.lint.detector.api.Implementation;
import com.android.tools.lint.detector.api.Issue;
import com.android.tools.lint.detector.api.Location;
import com.android.tools.lint.detector.api.Position;
import com.android.tools.lint.detector.api.ResourceXmlDetector;
import com.android.tools.lint.detector.api.Scope;
import com.android.tools.lint.detector.api.Severity;
import com.android.tools.lint.detector.api.XmlContext;
import com.android.utils.PositionXmlParser;
import com.soundcloud.android.memento.annotation.LintDetector;
import org.jetbrains.annotations.NotNull;
import org.w3c.dom.Attr;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@SuppressWarnings("DefaultLocale")
@LintDetector
public class XmlFormattingDetector extends ResourceXmlDetector implements Detector.XmlScanner {
    public static final Issue ISSUE_ORDER = Issue.create("sc.XmlOrder",
                                                         "XML attributes out of order.",
                                                         "Attributes should be ordered as described in our style guide " +
                                                                 "(<a href=\"https://github.com/soundcloud/android-listeners/wiki/Android-Guidelines#layout-file-structure\">StyleGuide</a>).",
                                                         Category.CORRECTNESS,
                                                         3,
                                                         Severity.INFORMATIONAL,
                                                         new Implementation(XmlFormattingDetector.class, Scope.RESOURCE_FILE_SCOPE));
    public static final Issue ISSUE_ATTR_WRAP = Issue.create("sc.XmlAttrWrap",
                                                             "Each attribute should be on its own line.",
                                                             "XML attributes should be on its own line. There has to be a linebreak after the tag, if there are more than one attributes." +
                                                                     "In case there is just one attribute, it has to be on the same line as the tag.",
                                                             Category.CORRECTNESS,
                                                             3,
                                                             Severity.INFORMATIONAL,
                                                             new Implementation(XmlFormattingDetector.class, Scope.RESOURCE_FILE_SCOPE));
    public static final Issue ISSUE_EMPTY_BODY = Issue.create("sc.XmlBody",
                                                              "XML tag has an empty body.",
                                                              "Empty bodies of XML tags should be collapsed.",
                                                              Category.CORRECTNESS,
                                                              3,
                                                              Severity.INFORMATIONAL,
                                                              new Implementation(XmlFormattingDetector.class, Scope.RESOURCE_FILE_SCOPE));
    public static final Issue ISSUE_WHITESPACE = Issue.create("sc.XmlWhiteSpace",
                                                              "No whitespace before closing tag.",
                                                              "Attributes without body should have a single whitespace before the closing tag. " +
                                                                      "If there is a body, there should be no whitespace.",
                                                              Category.CORRECTNESS,
                                                              3,
                                                              Severity.INFORMATIONAL,
                                                              new Implementation(XmlFormattingDetector.class, Scope.RESOURCE_FILE_SCOPE));
    public static final Issue ISSUE_LINE_BREAK = Issue.create("sc.XmlLineBreak",
                                                              "Elements should be separated by a single linebreak",
                                                              "All elements in a XML document should be separated by a single blank line. " +
                                                                      "There should be no blank lines before the root document tag. " +
                                                                      "At the end of the file there should be no more than one blank line.",
                                                              Category.CORRECTNESS,
                                                              3,
                                                              Severity.INFORMATIONAL,
                                                              new Implementation(XmlFormattingDetector.class, Scope.RESOURCE_FILE_SCOPE));
    private static final String LINE_SEPARATOR = System.getProperty("line.separator");
    private static final Pattern END_MATCH = Pattern.compile("/?>$");
    private static final Pattern START_MATCH = Pattern.compile("^</?[a-zA-Z0-9.]+");
    private static final Pattern FULL_CLOSING_MATCH = Pattern.compile("^</[a-zA-Z0-9.]+>$");
    private static final Pattern PATTERN_WHITESPACE_CLOSING = Pattern.compile("^[\\s]+[^\\s/]+(\\s*)/>$");
    private static final Pattern PATTERN_WHITESPACE_CLOSING_BODY = Pattern.compile("^[\\s]+[^\\s/]+(\\s*)>$");

    @Override
    public boolean appliesTo(ResourceFolderType folderType) {
        return folderType == ResourceFolderType.LAYOUT;
    }

    @Override
    public void visitDocument(XmlContext context, Document document) {
        verifySingleLineBreaks(context, document);
        verifyNoLeadingLineBreaks(context);
        verifyNoTrailingLineBreaks(context);

        final Element documentElement = context.document.getDocumentElement();
        visitElements(context, documentElement);
    }

    /**
     * Visits recursively all elements and its child nodes.
     * On each element {@link #visitElement(XmlContext, Element)} is called.
     */
    private void visitElements(XmlContext context, Element element) {
        visitElement(context, element);
        NodeList childNodes = element.getChildNodes();
        for (int i = 0, n = childNodes.getLength(); i < n; i++) {
            Node child = childNodes.item(i);
            if (child.getNodeType() == Node.ELEMENT_NODE) {
                visitElements(context, (Element) child);
            }
        }
    }

    @Override
    public void visitElement(XmlContext context, Element element) {
        verifyAttributeWrap(context, element);
        verifyAttributeOrder(context, element);
        verifyNoEmptyBody(context, element);
        verifyWhitespaces(context, element);
    }

    /**
     * Verifies that before the closing tag `/>` there is exactly a single whitespace.
     * If the element has a body, it is verified that there is no whitespace before the closing tag `>`.
     */
    private static void verifyWhitespaces(XmlContext context, Element element) {
        final int firstClosingTagLine = getFirstClosingTagLine(context, element);
        final String closingLine = context.getContents().toString().split(LINE_SEPARATOR)[firstClosingTagLine];
        final Matcher closingMatcher = PATTERN_WHITESPACE_CLOSING.matcher(closingLine);
        if (closingMatcher.find()) {
            String group = closingMatcher.group(1);
            if (group.isEmpty()) {
                context.report(ISSUE_WHITESPACE,
                               element,
                               Location.create(context.file, new SourcePosition(firstClosingTagLine, closingLine.indexOf("/>"), 0)),
                               "Missing whitespace before closing tag.");
            } else if (group.length() > 1) {
                int closingIndex = closingLine.indexOf("/>");
                context.report(ISSUE_WHITESPACE,
                               element,
                               Location.create(context.file, new SourcePosition(firstClosingTagLine, closingIndex - group.length() + 1, 0, firstClosingTagLine, closingIndex, 0)),
                               String.format("%d whitespaces should be replaced with one whitespace.", group.length()));
            }
        } else {
            final Matcher closingBodyMatcher = PATTERN_WHITESPACE_CLOSING_BODY.matcher(closingLine);
            if (closingBodyMatcher.find()) {
                String group = closingBodyMatcher.group(1);
                if (!group.isEmpty()) {
                    int closingIndex = closingLine.indexOf(">");
                    String message;
                    if (group.length() == 1) {
                        message = "Remove whitespace before closing tag.";
                    } else {
                        message = String.format("Remove %d whitespaces before closing tag.", group.length());
                    }
                    context.report(ISSUE_WHITESPACE,
                                   element,
                                   Location.create(context.file, new SourcePosition(firstClosingTagLine, closingIndex - group.length(), 0, firstClosingTagLine, closingIndex, 0)),
                                   message);
                }
            }
        }
    }

    /**
     * Verifies that the attributes of a given element are in order as described in {@link AttributeSortComparator}.
     */
    private static void verifyAttributeOrder(XmlContext context, Element element) {
        if (!element.hasAttributes()) {
            return;
        }
        final List<PositionAwareAttribute> attributes = getSortedAttributes(context, element);
        final List<Attr> sortedAttributes = sortAttributes(element);
        assert attributes.size() == sortedAttributes.size();
        for (int i = 0; i < sortedAttributes.size(); i++) {
            Node attribute = sortedAttributes.get(i);
            Node actualNode = attributes.get(i).attr;
            if (actualNode != attribute) {
                context.report(ISSUE_ORDER,
                               element,
                               context.getLocation(element),
                               String.format("Attributes out of order.\nExpected:\n`%s`\n\n", join(sortedAttributes, LINE_SEPARATOR)));
                return;
            }
        }
    }

    /**
     * Verifies that all attributes are in a single line. There has to be also a linebreak after the tag.
     * If there is just a single attributes, it has to be in the same line as the tag.
     */
    private static void verifyAttributeWrap(XmlContext context, Element element) {
        final Location elementLocation = context.getLocation(element);
        final List<PositionAwareAttribute> positionAwareNodes = getSortedAttributes(context, element);
        if (positionAwareNodes.isEmpty()) {
            verifyAttributeWrapEnd(context, element, positionAwareNodes);
        } else if (positionAwareNodes.size() == 1) {
            if (positionAwareNodes.get(0).line != elementLocation.getStart().getLine()) {
                context.report(ISSUE_ATTR_WRAP, element, elementLocation, "Single line attributes should be on the same line as the tag.");
            }
        } else {
            verifyAttributeWrapStart(context, elementLocation, positionAwareNodes);
            verifyAttributeWrapMiddle(context, positionAwareNodes);
            verifyAttributeWrapEnd(context, element, positionAwareNodes);
        }
    }

    /**
     * Verifies that the closing tag is in the same line as the last attribute.
     */
    private static void verifyAttributeWrapEnd(XmlContext context, Element element, List<PositionAwareAttribute> positionAwareNodes) {
        final int firstClosingTagLine = getFirstClosingTagLine(context, element);
        final int line;
        final Node node;
        if (positionAwareNodes.isEmpty()) {
            final Location location = context.getLocation(element);
            line = location.getStart().getLine();
            node = element;
        } else {
            final PositionAwareAttribute lastNode = positionAwareNodes.get(positionAwareNodes.size() - 1);
            line = lastNode.line;
            node = lastNode.attr;
        }
        if (firstClosingTagLine == line + 1) {
            context.report(ISSUE_ATTR_WRAP, node, context.getNameLocation(node), "XML tags should be closed on the final attribute line.");
        } else if (firstClosingTagLine != line) {
            context.report(
                    ISSUE_LINE_BREAK,
                    Location.create(
                            context.file,
                            new SourcePosition(line + 1, 0, 0, firstClosingTagLine, 0, 0)),
                    "XML tags should be closed on the final attribute line.");
        }
    }

    /**
     * Verifies that there is a single line break after the tag if there are at least to attributes.
     */
    private static void verifyAttributeWrapStart(XmlContext context, Location elementLocation, List<PositionAwareAttribute> positionAwareNodes) {
        final PositionAwareAttribute node = positionAwareNodes.get(0);
        int startLine = elementLocation.getStart().getLine();
        final int diff = node.line - startLine;
        if (diff == 0) {
            context.report(ISSUE_ATTR_WRAP, node.attr, context.getNameLocation(node.attr), "Each attribute must be followed by a linebreak.");
        } else if (diff > 1) {
            reportBlankLinesToRemove(context, startLine + 1, startLine + diff - 1);
        }
    }

    /**
     * Verifies that each attribute is on its own line.
     */
    private static void verifyAttributeWrapMiddle(XmlContext context, List<PositionAwareAttribute> positionAwareAttributes) {
        for (int i = 1; i < positionAwareAttributes.size(); i++) {
            final PositionAwareAttribute current = positionAwareAttributes.get(i);
            final PositionAwareAttribute previous = positionAwareAttributes.get(i - 1);
            final int diff = current.line - previous.line;
            if (diff == 0) {
                context.report(ISSUE_ATTR_WRAP, current.attr, context.getNameLocation(current.attr), "Each attribute must be followed by a linebreak.");
            } else if (diff > 1) {
                reportBlankLinesToRemove(context, previous.line + 1, current.line - 1);
            }
        }
    }

    /**
     * Verifies that no element has an empty body.
     */
    private static void verifyNoEmptyBody(XmlContext context, Element element) {
        final NodeList childNodes = element.getChildNodes();
        final Location location = context.getLocation(element);
        if (childNodes.getLength() == 1) {
            if (childNodes.item(0).getNodeType() == Node.TEXT_NODE) {
                if (isEmptyOrSpaces(childNodes.item(0).getTextContent())) {
                    context.report(ISSUE_EMPTY_BODY, element, location, "XML tags with an empty body should be collapsed.");
                }
            }
        } else if (childNodes.getLength() == 0) {
            final String xml = join(getContentXmlBlock(context, location), "");
            if (xml.startsWith("<" + element.getTagName()) && xml.endsWith("</" + element.getTagName() + ">")) {
                context.report(ISSUE_EMPTY_BODY, element, location, "XML tags with an empty body should be collapsed.");
            }
        }
    }

    /**
     * Verifies that two {@link Element}s are separated by a single linebreak.
     */
    private static void verifySingleLineBreaks(XmlContext context, Document document) {
        List<String> fileLines = getContentXmlBlock(context, context.getLocation(document.getDocumentElement()));
        if (fileLines.size() < 2) {
            return;
        }
        int lastNonBlank = isEmptyOrSpaces(fileLines.get(0)) ? -1 : 0;
        for (int i = 1; i < fileLines.size(); i++) {
            String previous = fileLines.get(i - 1);
            String current = fileLines.get(i);
            boolean previousIsClosingTag = END_MATCH.matcher(previous.trim()).find();
            boolean currentIsOpeningTag = START_MATCH.matcher(current.trim()).find();
            if (previousIsClosingTag && currentIsOpeningTag) {
                if (FULL_CLOSING_MATCH.matcher(previous.trim()).find() && FULL_CLOSING_MATCH.matcher(current.trim()).find()) {
                    continue;
                }
                context.report(
                        ISSUE_LINE_BREAK,
                        document.getDocumentElement(),
                        Location.create(
                                context.file,
                                new SourcePosition(i + 1, 0, 0)),
                        "Elements should be separated by a single linebreak.");
            }
            if (!isEmptyOrSpaces(current) && lastNonBlank < i - 2) {
                if (!isInsideAttributes(context, i)) {
                    reportExtraBlankLines(context, lastNonBlank + 2, i);
                }
            }
            if (!isEmptyOrSpaces(current)) {
                lastNonBlank = i;
            }
        }
    }

    /**
     * Verifies that there is no empty before the document element.
     */
    private static void verifyNoLeadingLineBreaks(XmlContext context) {
        final String fileContent = context.getContents().toString().replaceAll(LINE_SEPARATOR, LINE_SEPARATOR + " ");
        final List<String> fileLines = Arrays.asList(fileContent.split(LINE_SEPARATOR));
        if (fileLines.size() == 1) {
            return;
        }
        int i = 1;
        for (int n = fileLines.size(); i < n; i++) {
            final String current = fileLines.get(i);
            if (!isEmptyOrSpaces(current)) {
                break;
            }
        }
        if (i > 1) {
            reportBlankLinesToRemove(context, 1, i - 1);
        }
    }

    /**
     * Verifies that there are no more than one linebreak at the end of the file.
     * Verification that there is a single line at the end of the file is covered by Checkstyle.
     */
    private static void verifyNoTrailingLineBreaks(XmlContext context) {
        final String fileContent = context.getContents().toString().replaceAll(LINE_SEPARATOR, LINE_SEPARATOR + " ");
        final List<String> fileLines = Arrays.asList(fileContent.split(LINE_SEPARATOR));
        if (fileLines.size() == 0) {
            return;
        }
        int lastNonBlank = -1;
        for (int i = 0, n = fileLines.size(); i < n; i++) {
            final String current = fileLines.get(i);
            if (!isEmptyOrSpaces(current)) {
                lastNonBlank = i;
            }
        }
        int diff = fileLines.size() - (lastNonBlank + 1);
        if (diff > 1) {
            reportExtraBlankLines(context, lastNonBlank + 1, fileLines.size() - 1);
        }
    }

    /**
     * @return list of all attributes of the given element sorted by the {@link AttributeSortComparator}
     */
    private static List<Attr> sortAttributes(Element element) {
        final Set<Attr> sortedAttributes = new TreeSet<>(new AttributeSortComparator());
        NamedNodeMap attributes = element.getAttributes();
        for (int i = 0; i < attributes.getLength(); i++) {
            Node item = attributes.item(i);
            if (item.getNodeType() == Node.ATTRIBUTE_NODE) {
                sortedAttributes.add((Attr) item);
            }
        }
        return new ArrayList<>(sortedAttributes);
    }

    /**
     * @return line of the opening body (`>`) or closing tag (`/>`)
     */
    private static int getFirstClosingTagLine(XmlContext context, Element element) {
        NodeList childNodes = element.getChildNodes();
        for (int i = 0, n = childNodes.getLength(); i < n; i++) {
            Node child = childNodes.item(i);
            if (child.getNodeType() == Node.TEXT_NODE) {
                return context.getLocation(child).getStart().getLine();
            }
        }
        return context.getLocation(element).getEnd().getLine();
    }

    /**
     * @return List of Strings each entry representing the part of the given line that is described by locations start and end column and line.
     */
    private static List<String> getContentXmlBlock(XmlContext context, Location location) {
        final List<String> fileLines = Arrays.asList(context.getContents().toString().replaceAll(LINE_SEPARATOR, LINE_SEPARATOR + " ").split(LINE_SEPARATOR));
        final Position start = location.getStart();
        final Position end = location.getEnd();
        final List<String> xmlParts = new ArrayList<>(fileLines.subList(start.getLine(), end.getLine() + 1));
        final String firstLine = xmlParts.get(0);
        final String lastLine = xmlParts.get(xmlParts.size() - 1);
        xmlParts.set(0, firstLine.substring(Math.min(firstLine.length(), start.getColumn() + 1)));
        xmlParts.set(xmlParts.size() - 1, lastLine.substring(0, Math.min(lastLine.length(), end.getColumn() + 1)));
        return xmlParts;
    }

    private static boolean isInsideAttributes(XmlContext context, int line) {
        final Node node = PositionXmlParser.findNodeAtLineAndCol(context.document, line, 0);
        if (node == null || node.getNodeType() != Node.ELEMENT_NODE) {
            return false;
        }
        final int start = context.getLocation(node).getStart().getLine();
        final int end = getFirstClosingTagLine(context, (Element) node);
        return start <= line && line <= end;
    }

    private static void reportExtraBlankLines(
            final XmlContext context,
            final int firstBlankLine,
            final int lastBlankLine) {
        context.report(
                ISSUE_LINE_BREAK,
                Location.create(
                        context.file,
                        new SourcePosition(firstBlankLine, 0, 0, lastBlankLine, 0, 0)),
                String.format(
                        "Lines %d-%d should be replaced by a single blank line.", firstBlankLine + 1, lastBlankLine + 1));
    }

    private static void reportBlankLinesToRemove(
            final XmlContext context,
            final int firstBlankLine,
            final int lastBlankLine) {
        final String description;
        if (firstBlankLine == lastBlankLine) {
            description = String.format("Line %d should be removed.", firstBlankLine + 1);
        } else {
            description = String.format("Lines %d-%d should be removed.", firstBlankLine + 1, lastBlankLine + 1);
        }
        context.report(
                ISSUE_LINE_BREAK,
                Location.create(
                        context.file,
                        new SourcePosition(firstBlankLine, 0, 0, lastBlankLine, 0, 0)),
                description);
    }

    /**
     * Returns a list of all attributes sorted by its line and column of the start location.
     */
    private static List<PositionAwareAttribute> getSortedAttributes(XmlContext context, Element element) {
        final NamedNodeMap attributes = element.getAttributes();
        final Set<PositionAwareAttribute> positionAwareNodes = new TreeSet<>();
        for (int i = 0; i < attributes.getLength(); i++) {
            Node item = attributes.item(i);
            if (item.getNodeType() == Node.ATTRIBUTE_NODE) {
                positionAwareNodes.add(new PositionAwareAttribute(context, (Attr) item));
            }
        }
        assert positionAwareNodes.size() == attributes.getLength();
        return new ArrayList<>(positionAwareNodes);
    }

    /**
     * Helper class that keeps track of the actual location of an attribute.
     */
    private static class PositionAwareAttribute implements Comparable<PositionAwareAttribute> {
        private final int line;
        private final int column;
        private final Attr attr;
        private final Location location;

        PositionAwareAttribute(XmlContext context, Attr attr) {
            this.attr = attr;
            this.location = context.getLocation(attr);
            this.line = location.getStart().getLine();
            this.column = location.getStart().getColumn();
        }

        @Override
        public int compareTo(@NotNull PositionAwareAttribute positionAwareAttribute) {
            int lineCompare = Integer.compare(line, positionAwareAttribute.line);
            if (lineCompare == 0) {
                return Integer.compare(column, positionAwareAttribute.column);
            } else {
                return lineCompare;
            }
        }
    }

    /**
     * Sorts as described in our styleguide and Intellij Android project configuration:
     *
     * xmlns:android
     * other xmlns alphabetically
     * android:id
     * android:name
     * name
     * style
     * other attributes without namespace alphabetically
     * android:layout_width
     * android:layout_height
     * android:layout_* alphabetically
     * android:width
     * android:height
     * android:* alphabetically
     * other attributes with namespace alphabetically
     *
     * @see <a href="https://github.com/soundcloud/android-listeners/wiki/Android-Guidelines#layout-file-structure">StyleGuide</a>
     * @see com.android.ide.common.xml.XmlAttributeSortOrder for default Android sort order
     */
    private static class AttributeSortComparator implements Comparator<Attr> {
        @Override
        public int compare(Attr attr1, Attr attr2) {
            final String prefix1 = attr1.getPrefix();
            final String prefix2 = attr2.getPrefix();
            if (XMLNS.equals(prefix1)) {
                if (XMLNS.equals(prefix2)) {
                    String name1 = nullToEmpty(attr1.getLocalName());
                    String name2 = nullToEmpty(attr2.getLocalName());
                    int priority1 = getNamespacePriority(name1);
                    int priority2 = getNamespacePriority(name2);
                    if (priority1 != priority2) {
                        return priority1 - priority2;
                    }
                    return name1.compareTo(name2);
                }
                return -1;
            } else if (XMLNS.equals(attr2.getPrefix())) {
                return 1;
            }

            final String name1 = prefix1 != null ? attr1.getLocalName() : attr1.getName();
            final String name2 = prefix2 != null ? attr2.getLocalName() : attr2.getName();
            return compareAttributes(nullToEmpty(prefix1), name1, nullToEmpty(prefix2), name2);
        }

        private static int compareAttributes(String prefix1, String name1, String prefix2, String name2) {
            final int priority1 = getAttributePriority(prefix1, name1);
            final int priority2 = getAttributePriority(prefix2, name2);
            if (priority1 != priority2) {
                return priority1 - priority2;
            }
            final int namespaceDelta = prefix1.compareTo(prefix2);
            if (namespaceDelta != 0) {
                return namespaceDelta;
            }
            return name1.compareTo(name2);
        }

        private static int getAttributePriority(String prefix, String name) {
            if (prefix.equals(ANDROID_NS_NAME) && ATTR_ID.equals(name)) {
                return 10;
            }
            if (ATTR_NAME.equals(name)) {
                if (prefix.equals(ANDROID_NS_NAME)) {
                    return 15;
                } else {
                    return 20;
                }
            }
            if (prefix.isEmpty()) {
                if (ATTR_STYLE.equals(name)) {
                    return 25;
                } else {
                    return 30;
                }
            }
            if (prefix.equals(ANDROID_NS_NAME)) {
                if (name.startsWith(ATTR_LAYOUT_RESOURCE_PREFIX)) {
                    if (name.equals(ATTR_LAYOUT_WIDTH)) {
                        return 40;
                    }
                    if (name.equals(ATTR_LAYOUT_HEIGHT)) {
                        return 50;
                    }

                    return 60;
                }
                if (name.equals(ATTR_WIDTH)) {
                    return 70;
                }
                if (name.equals(ATTR_HEIGHT)) {
                    return 80;
                }
            }

            return 90;
        }

        private int getNamespacePriority(String name) {
            if (ANDROID_NS_NAME.equals(name)) {
                return 10;
            }
            return 90;
        }
    }
}
