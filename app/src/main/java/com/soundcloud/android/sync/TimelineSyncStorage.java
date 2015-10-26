package com.soundcloud.android.sync;

import com.soundcloud.android.api.model.Link;
import com.soundcloud.android.utils.Log;
import com.soundcloud.android.utils.ScTextUtils;
import com.soundcloud.java.optional.Optional;

import android.content.SharedPreferences;

// Stores sync meta-data pertaining to content based on our Timeline services, i.e.
// the sound stream and activities
public class TimelineSyncStorage {

    private static final String PREFS_NEXT_URL = "next_url";
    private static final String PREFS_FUTURE_URL = "future_url";

    private final SharedPreferences prefs;

    public TimelineSyncStorage(SharedPreferences prefs) {
        this.prefs = prefs;
    }

    public String getNextPageUrl() {
        return prefs.getString(PREFS_NEXT_URL, ScTextUtils.EMPTY_STRING);
    }

    public boolean hasNextPageUrl() {
        return prefs.contains(PREFS_NEXT_URL);
    }

    public String getFuturePageUrl() {
        return prefs.getString(PREFS_FUTURE_URL, ScTextUtils.EMPTY_STRING);
    }

    public boolean isMissingFuturePageUrl() {
        return !prefs.contains(PREFS_FUTURE_URL);
    }

    public void storeNextPageUrl(Optional<Link> nextLink) {
        if (nextLink.isPresent()) {
            final String href = nextLink.get().getHref();
            Log.d(this, "Writing next soundstream link to preferences : " + href);
            prefs.edit().putString(PREFS_NEXT_URL, href).apply();
        } else {
            Log.d(this, "No next link in soundstream response, clearing any stored link");
            prefs.edit().remove(PREFS_NEXT_URL).apply();
        }
    }

    public void storeFuturePageUrl(Link futureLink) {
        final String href = futureLink.getHref();
        Log.d(this, "Writing future soundstream link to preferences : " + href);
        prefs.edit().putString(PREFS_FUTURE_URL, href).apply();
    }

    public void clear() {
        prefs.edit().clear().apply();
    }

}
