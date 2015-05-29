package com.soundcloud.android.framework.with;

import com.google.common.base.Predicate;
import com.soundcloud.android.framework.viewelements.TextElement;
import com.soundcloud.android.framework.viewelements.ViewElement;

import android.content.res.Resources;
import android.view.View;

import java.util.Arrays;
import java.util.List;

public abstract class With implements Predicate<ViewElement> {
    public static void setResources(Resources resources) {
        With.resources = resources;
    }

    public static Resources resources;

    public abstract String getSelector();

    public static With id(int viewId) {
        return new WithId(viewId);
    }

    public static With text(String... text) {
        return new WithText(text);
    }

    public static With textContaining(String text) {
        return new WithTextContaining(text);
    }

    public static With className (Class<? extends View> classToSearch) {
        return new WithClass(classToSearch);
    }

    public static With className(String classStringName) {
        return new WithClassName(classStringName);
    }

    public static With classSimpleName(String classStringName) {
        return new WithClassSimpleName(classStringName);
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
            return String.format("With text: %s", searchedText );
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
            return viewElement.getViewClass().getName().toString().equals(className);
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
            return viewElement.getViewClass().getSimpleName().toString().equals(classSimpleName);
        }

        @Override
        public String getSelector() {
            return String.format("With ClassSimpleName: %s", classSimpleName);
        }
    }
}
