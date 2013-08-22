package com.soundcloud.android.robolectric;

import com.pivotallabs.greatexpectations.matchers.ObjectMatcher;
import com.xtremelabs.robolectric.Robolectric;
import com.xtremelabs.robolectric.shadows.ShadowContentResolver;

import android.content.ContentResolver;
import android.net.Uri;

import java.util.List;

public class ContentResolverMatcher<T extends ContentResolver, M extends ContentResolverMatcher<T, M>> extends ObjectMatcher<T, M> {

    public boolean toNotifyUri(Uri expected) {
        ShadowContentResolver shadowResolver = Robolectric.shadowOf(actual);
        List<ShadowContentResolver.NotifiedUri> notifiedUris = shadowResolver.getNotifiedUris();

        for (ShadowContentResolver.NotifiedUri notifiedUri : notifiedUris) {
            if (notifiedUri.uri.equals(expected)) return true;
        }
        return false;
    }

    public boolean toNotifyUri(String expected) {
        return toNotifyUri(Uri.parse(expected));
    }

}
