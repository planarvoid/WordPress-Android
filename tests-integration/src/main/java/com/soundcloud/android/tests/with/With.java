package com.soundcloud.android.tests.with;

import android.view.View;
import com.google.common.base.Predicate;
import com.soundcloud.android.tests.ViewElement;

public abstract class With {
    public static With id(int viewId) {
        return new WithId(viewId);
    }

    public static With text(String text) {
        return new WithText(text);
    }

    public static With className (Class classToSearch) {
        return new WithClass(classToSearch);
    }

    public abstract Predicate<ViewElement> filter();

    static class WithId extends With {
        private final int viewId;

        WithId(int id) {
            viewId = id;
        }

        public Predicate filter() {
            return new Predicate<ViewElement>() {
                @Override
                public boolean apply(ViewElement viewElement) {
                    return viewElement.getId() == viewId;
                }
            };
        }
    }

    static class WithText extends With {
        private final String searchedText;

        WithText(String text) {
            searchedText = text;
        }

        @Override
        public Predicate<ViewElement> filter() {
            return new Predicate<ViewElement>() {
                public boolean apply(ViewElement viewElement) {
                    return viewElement.getText().equals(searchedText);
                }
            };
        }
    }

    private static class WithClass extends With {
        private final Class<? extends View> classToSearch;

        public WithClass(Class<? extends View> className) {
            classToSearch = className;
        }

        @Override
        public Predicate<ViewElement> filter() {
            return new Predicate<ViewElement>() {
                public boolean apply(ViewElement viewElement) {
                    return classToSearch.isAssignableFrom(viewElement.getViewClass());
                }
            };
        }
    }
}
