package com.soundcloud.android;

import static com.pivotallabs.greatexpectations.GreatExpectations.wrapped;

import com.pivotallabs.greatexpectations.AllowActualToBeNull;
import com.pivotallabs.greatexpectations.matchers.BooleanMatcher;
import com.pivotallabs.greatexpectations.matchers.ComparableMatcher;
import com.pivotallabs.greatexpectations.matchers.DateMatcher;
import com.pivotallabs.greatexpectations.matchers.IterableMatcher;
import com.pivotallabs.greatexpectations.matchers.ObjectMatcher;
import com.pivotallabs.greatexpectations.matchers.SetMatcher;
import com.pivotallabs.greatexpectations.matchers.StringMatcher;
import com.soundcloud.android.robolectric.ContentMatcher;
import com.soundcloud.android.robolectric.ContentResolverMatcher;
import com.soundcloud.android.robolectric.CursorMatcher;
import com.soundcloud.android.robolectric.IntentMatcher;
import com.soundcloud.android.robolectric.NotificationMatcher;
import com.soundcloud.android.robolectric.ToastMatcher;
import com.soundcloud.android.robolectric.UriMatcher;
import com.soundcloud.android.robolectric.ViewMatcher;
import com.soundcloud.android.storage.provider.Content;
import org.jetbrains.annotations.Nullable;

import android.app.Notification;
import android.content.ContentResolver;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.view.View;
import android.widget.Toast;

public class Expect {

    public static <T extends Object> ObjectMatcher<T, ?> expect(@Nullable T actual) {
        return wrapped(ObjectMatcher.class, actual);
    }

    public static BooleanMatcher<Boolean, ?> expect(boolean actual) {
        return wrapped(BooleanMatcher.class, actual);
    }

    public static <T extends Boolean> BooleanMatcher<T, ?> expect(T actual) {
        return wrapped(BooleanMatcher.class, actual);
    }

    public static <T extends Comparable> ComparableMatcher<T, ?> expect(T actual) {
        return wrapped(ComparableMatcher.class, actual);
    }

    public static <T extends java.util.Date> DateMatcher<T, ?> expect(T actual) {
        return wrapped(DateMatcher.class, actual);
    }

    public static <T extends Iterable<X>, X> IterableMatcher<T, X, ?> expect(T actual) {
        return wrapped(IterableMatcher.class, actual);
    }

    public static <T extends String> StringMatcher<T, ?> expect(T actual) {
        return wrapped(StringMatcher.class, actual);
    }

    public static <T extends java.util.Set<X>, X> SetMatcher<T, X, ?> expect(T actual) {
        return wrapped(SetMatcher.class, actual);
    }

    public static <T extends Content> ContentMatcher<T, ?> expect(T actual) {
        return wrapped(ContentMatcher.class, actual);
    }

    public static <T extends Uri> UriMatcher<T, ?> expect(@Nullable T actual) {
        return wrapped(UriMatcher.class, actual);
    }

    public static <T extends Cursor> CursorMatcher<T, ?> expect(T actual) {
        return wrapped(CursorMatcher.class, actual);
    }

    public static <T extends Notification> NotificationMatcher<T, ?> expect(T actual) {
        return wrapped(NotificationMatcher.class, actual);
    }

    public static <T extends ContentResolver> ContentResolverMatcher<T, ?> expect(T actual) {
        return wrapped(ContentResolverMatcher.class, actual);
    }

    public static <T extends Intent> IntentMatcher<T, ?> expect(T actual) {
        return wrapped(IntentMatcher.class, actual);
    }

    public static <T extends View> ViewMatcher<T, ?> expect(T actual) {
        return wrapped(ViewMatcher.class, actual);
    }

    public static <T extends Toast> ToastMatcher<T, ?> expect(T actual) {
        return wrapped(ToastMatcher.class, actual);
    }

    @SuppressWarnings("UnusedDeclaration")
    public static class NullStringMatcher extends StringMatcher {
        @AllowActualToBeNull
        public boolean toEqual(Object expected) {
            return (actual == null) ? expected == null : actual.equals(expected);
        }
    }
}