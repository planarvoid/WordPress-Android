package com.soundcloud.android;

import static com.pivotallabs.greatexpectations.GreatExpectations.wrapped;

import com.pivotallabs.greatexpectations.matchers.BooleanMatcher;
import com.pivotallabs.greatexpectations.matchers.ComparableMatcher;
import com.pivotallabs.greatexpectations.matchers.DateMatcher;
import com.pivotallabs.greatexpectations.matchers.IterableMatcher;
import com.pivotallabs.greatexpectations.matchers.LongMatcher;
import com.pivotallabs.greatexpectations.matchers.ObjectMatcher;
import com.pivotallabs.greatexpectations.matchers.SetMatcher;
import com.pivotallabs.greatexpectations.matchers.StringMatcher;
import com.soundcloud.android.provider.Content;
import com.soundcloud.android.robolectric.ContentMatcher;
import com.soundcloud.android.robolectric.UriMatcher;

import android.net.Uri;

@SuppressWarnings({"unchecked", "UnusedDeclaration", "TypeParameterExplicitlyExtendsObject"})
public class Expect {

    public static <T extends Object, M extends ObjectMatcher<T, M>> ObjectMatcher<T, ?> expect(T actual) {
        return wrapped(ObjectMatcher.class, actual);
    }
    public static BooleanMatcher<Boolean, ?> expect(boolean actual) {
        return wrapped(BooleanMatcher.class, actual);
    }
    public static <T extends Boolean, M extends BooleanMatcher<T, M>> BooleanMatcher<T, ?> expect(T actual) {
        return wrapped(BooleanMatcher.class, actual);
    }
    public static <T extends Comparable, M extends ComparableMatcher<T, M>> ComparableMatcher<T, ?> expect(T actual) {
        return wrapped(ComparableMatcher.class, actual);
    }
    public static <T extends java.util.Date, M extends DateMatcher<T, M>> DateMatcher<T, ?> expect(T actual) {
        return wrapped(DateMatcher.class, actual);
    }
    public static <T extends Iterable<X>, X, M extends IterableMatcher<T, X, M>> IterableMatcher<T, X, ?> expect(T actual) {
        return wrapped(IterableMatcher.class, actual);
    }
    public static <T extends java.util.Set<X>, X, M extends SetMatcher<T, X, M>> SetMatcher<T, X, ?> expect(T actual) {
        return wrapped(SetMatcher.class, actual);
    }
    public static <T extends String, M extends StringMatcher<T, M>> StringMatcher<T, ?> expect(T actual) {
        return wrapped(StringMatcher.class, actual);
    }
//    public static <T extends Long, M extends LongMatcher<T, M>> LongMatcher<T, ?> expect(T actual) {
//        return wrapped(LongMatcher.class, actual);
//    }
    public static <T extends Content, M extends ContentMatcher<T, M>> ContentMatcher<T, ?> expect(T actual) {
        return wrapped(ContentMatcher.class, actual);
    }
    public static <T extends Uri, M extends UriMatcher<T, M>> UriMatcher<T, ?> expect(T actual) {
        return wrapped(UriMatcher.class, actual);
    }
}