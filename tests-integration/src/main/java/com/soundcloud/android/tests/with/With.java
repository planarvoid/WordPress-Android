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
}
