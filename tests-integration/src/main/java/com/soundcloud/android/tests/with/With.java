package com.soundcloud.android.tests.with;

import com.google.common.base.Predicate;
import com.soundcloud.android.tests.ViewElement;

import android.view.View;

public abstract class With implements Predicate<ViewElement> {
    public static With id(int viewId) {
        return new WithId(viewId);
    }

    public static With text(String text) {
        return new WithText(text);
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
    }

    static class WithText extends With {
        private final String searchedText;

        WithText(String text) {
            searchedText = text;
        }

        @Override
        public boolean apply(ViewElement viewElement) {
            return viewElement.getText().equals(searchedText);
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
    }
}
