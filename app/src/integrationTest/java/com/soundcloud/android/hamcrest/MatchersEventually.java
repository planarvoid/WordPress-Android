package com.soundcloud.android.hamcrest;

import com.soundcloud.android.playlists.NewPlaylistDetailsPresenterIntegrationTest;
import com.soundcloud.java.collections.Iterables;
import org.hamcrest.Description;
import org.hamcrest.Matcher;
import org.hamcrest.TypeSafeMatcher;

class MatchersEventually {
    static <T> Matcher<Iterable<T>> retry(Matcher<T> delegate) {
        return new TypeSafeMatcher<Iterable<T>>() {
            @Override
            public void describeTo(Description description) {
                delegate.describeTo(description);
            }

            @Override
            protected void describeMismatchSafely(Iterable<T> item, Description mismatchDescription) {
                final T last = Iterables.getLast(item);
                delegate.describeMismatch(last, mismatchDescription);
            }

            public boolean matchesSafely(Iterable<T> ts) {
                for (T t : ts) {
                    if (delegate.matches(t)) {
                        return true;
                    }
                }
                return false;
            }
        };
    }
}
