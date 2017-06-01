package com.soundcloud.android.search.topresults;

import com.google.auto.value.AutoValue;

abstract class SearchResult {
    enum Kind {
        LOADING, DATA, ERROR
    }

    abstract Kind kind();

    @AutoValue
    abstract static class Loading extends SearchResult {
        abstract boolean isRefreshing();

        public static Loading create(boolean isRefreshing) {
            return new AutoValue_SearchResult_Loading(Kind.LOADING, isRefreshing);
        }
    }

    @AutoValue
    abstract static class Data extends SearchResult {
        abstract TopResults data();

        public static SearchResult create(TopResults data) {
            return new AutoValue_SearchResult_Data(Kind.DATA, data);
        }
    }

    @AutoValue
    abstract static class Error extends SearchResult {
        abstract Throwable throwable();

        public static Error create(Throwable throwable) {
            return new AutoValue_SearchResult_Error(Kind.ERROR, throwable);
        }
    }
}
