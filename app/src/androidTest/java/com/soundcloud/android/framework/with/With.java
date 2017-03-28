package com.soundcloud.android.framework.with;

import com.soundcloud.android.framework.viewelements.TextElement;
import com.soundcloud.android.framework.viewelements.ViewElement;
import com.soundcloud.java.functions.Predicate;
import com.soundcloud.java.objects.MoreObjects;

import android.content.res.Resources;
import android.support.annotation.StringRes;
import android.view.View;

import java.util.Arrays;
import java.util.List;
import java.util.regex.Pattern;

public abstract class With implements Predicate<ViewElement> {
    public static void setResources(Resources resources) {
        With.resources = resources;
    }

    public static Resources resources;

    public abstract String getSelector();

    public static With id(int viewId) {
        return new WithId(viewId);
    }

    public static With text(@StringRes int textId) {
        return new WithText(resources.getString(textId));
    }

    public static With text(String... text) {
        return new WithText(text);
    }

    public static With textContaining(String text) {
        return new WithTextContaining(text);
    }

    public static With textMatching(Pattern regexPattern) {
        return new WithTextMatching(regexPattern);
    }

    public static With contentDescription(String description) {
        return new WithContentDescription(description);
    }

    public static With className(Class<? extends View> classToSearch) {
        return new WithClass(classToSearch);
    }

    public static With className(String classStringName) {
        return new WithClassName(classStringName);
    }

    public static With classSimpleName(String classStringName) {
        return new WithClassSimpleName(classStringName);
    }

    public static With either(With first, With second) {
        return new WithEither(first, second);
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(this)
                          .add("selector", getSelector())
                          .toString();
    }

    static class WithEither extends With {
        private final With first;
        private final With second;

        WithEither(With first, With second) {
            this.first = first;
            this.second = second;
        }

        @Override
        public boolean apply(ViewElement viewElement) {
            return first.apply(viewElement) || second.apply(viewElement);
        }

        public String getSelector() {
            return String.format("With either: [%s] or [%s]", first.getSelector(), second.getSelector());
        }
    }

    static class WithId extends With {
        private final int viewId;

        WithId(int id) {
            viewId = id;
        }

        @Override
        public boolean apply(ViewElement viewElement) {
            return viewElement.getId() == viewId;
        }

        public String getSelector() {
            return String.format("With id: %s", resources.getResourceName(viewId));
        }
    }

    static class WithText extends With {
        private final List<String> searchedText;

        WithText(String... text) {
            searchedText = Arrays.asList(text);
        }

        @Override
        public boolean apply(ViewElement viewElement) {
            try {

                final String expected = new TextElement(viewElement).getText();
                return searchedText.contains(expected);
            } catch (UnsupportedOperationException ignored) {
                return false;
            }
        }

        @Override
        public String getSelector() {
            return String.format("With text: %s", searchedText);
        }
    }

    public static class WithPopulatedText extends With {
        @Override
        public boolean apply(ViewElement viewElement) {
            try {
                final String text = new TextElement(viewElement).getText();
                return !text.isEmpty();
            } catch (UnsupportedOperationException ignored) {
                return false;
            }
        }

        @Override
        public String getSelector() {
            return String.format("With populated text");
        }
    }

    static class WithContentDescription extends With {
        private final String description;

        WithContentDescription(String description) {
            this.description = description;
        }

        @Override
        public boolean apply(ViewElement viewElement) {
            return description.equals(viewElement.getContentDescription());
        }

        @Override
        public String getSelector() {
            return String.format("With content description: %s", description);
        }
    }

    static class WithTextContaining extends With {
        private final String searchedText;

        WithTextContaining(String text) {
            searchedText = text;
        }

        @Override
        public boolean apply(ViewElement viewElement) {
            try {
                return new TextElement(viewElement).getText().contains(searchedText);
            } catch (UnsupportedOperationException ignored) {
                return false;
            }
        }

        @Override
        public String getSelector() {
            return String.format("Containing text: %s", searchedText);
        }
    }

    static class WithTextMatching extends With {
        private final Pattern regexPattern;

        WithTextMatching(Pattern regexPattern) {
            this.regexPattern = regexPattern;
        }

        @Override
        public boolean apply(ViewElement viewElement) {
            try {
                return regexPattern.matcher(new TextElement(viewElement).getText()).matches();
            } catch (UnsupportedOperationException ignored) {
                return false;
            }
        }

        @Override
        public String getSelector() {
            return String.format("Matching regex: %s", regexPattern);
        }
    }

    private static class WithClass extends With {
        private final Class<? extends View> classToSearch;

        public WithClass(Class<? extends View> className) {
            classToSearch = className;
        }

        @Override
        public boolean apply(ViewElement viewElement) {
            return classToSearch.isAssignableFrom(viewElement.getViewClass());
        }

        @Override
        public String getSelector() {
            return String.format("With class: %s", classToSearch.toString());
        }
    }

    private static class WithClassName extends With {
        private String className;

        public WithClassName(String classStringName) {
            className = classStringName;
        }

        @Override
        public boolean apply(ViewElement viewElement) {
            return viewElement.getViewClass().getName().equals(className);
        }

        @Override
        public String getSelector() {
            return String.format("With class: %s", className);
        }
    }

    private static class WithClassSimpleName extends With {
        private String classSimpleName;

        public WithClassSimpleName(String classStringName) {
            classSimpleName = classStringName;
        }

        @Override
        public boolean apply(ViewElement viewElement) {
            return viewElement.getViewClass().getSimpleName().equals(classSimpleName);
        }

        @Override
        public String getSelector() {
            return String.format("With ClassSimpleName: %s", classSimpleName);
        }
    }
}
