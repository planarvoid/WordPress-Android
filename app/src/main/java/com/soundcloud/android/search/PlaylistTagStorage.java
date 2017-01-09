package com.soundcloud.android.search;

import com.soundcloud.android.rx.ScSchedulers;
import com.soundcloud.android.storage.StorageModule;
import com.soundcloud.android.utils.CurrentDateProvider;
import com.soundcloud.android.utils.DateProvider;
import com.soundcloud.java.strings.Strings;
import rx.Observable;
import rx.Scheduler;

import android.content.SharedPreferences;
import android.support.annotation.VisibleForTesting;

import javax.inject.Inject;
import javax.inject.Named;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.TimeUnit;

public class PlaylistTagStorage {

    private static final String KEY_RECENT_TAGS = "recent_tags";
    private static final String KEY_POPULAR_TAGS = "popular_tags";
    private static final int MAX_RECENT_TAGS = 5;

    private static final String KEY_LAST_SYNC_TIME = "tags_last_sync_time";
    private static final long CACHE_EXPIRATION_TIME = TimeUnit.DAYS.toMillis(1);

    private final SharedPreferences sharedPreferences;
    private final DateProvider dateProvider;
    private final Scheduler scheduler;

    @Inject
    public PlaylistTagStorage(@Named(StorageModule.PLAYLIST_TAGS) SharedPreferences sharedPreferences,
                              CurrentDateProvider dateProvider) {
        this(sharedPreferences, dateProvider, ScSchedulers.HIGH_PRIO_SCHEDULER);
    }

    @VisibleForTesting
    PlaylistTagStorage(SharedPreferences sharedPreferences, DateProvider dateProvider, Scheduler scheduler) {
        this.sharedPreferences = sharedPreferences;
        this.dateProvider = dateProvider;
        this.scheduler = scheduler;
    }

    public void addRecentTag(String tag) {
        LinkedList<String> recentTags = new LinkedList<>(getRecentTags());
        if (recentTags.contains(tag)) {
            return;
        }
        if (recentTags.size() == MAX_RECENT_TAGS) {
            recentTags.removeLast();
        }
        recentTags.addFirst(sanitizeTag(tag));

        sharedPreferences.edit().putString(KEY_RECENT_TAGS, serialize(recentTags)).apply();
    }

    public void cachePopularTags(List<String> tags) {
        final SharedPreferences.Editor preferences = sharedPreferences.edit();
        preferences.putString(KEY_POPULAR_TAGS, serialize(tags));
        preferences.putLong(KEY_LAST_SYNC_TIME, dateProvider.getCurrentTime());
        preferences.apply();
    }

    public Observable<List<String>> getRecentTagsAsync() {
        return Observable
                .fromCallable(() -> getRecentTags())
                .subscribeOn(scheduler);
    }

    public Observable<List<String>> getPopularTagsAsync() {
        return Observable
                .fromCallable(() -> getPopularTags())
                .subscribeOn(scheduler);
    }

    public boolean isTagsCacheExpired() {
        long lastSyncTime = sharedPreferences.getLong(KEY_LAST_SYNC_TIME, 0);
        return (dateProvider.getCurrentTime() - lastSyncTime > CACHE_EXPIRATION_TIME);
    }

    @VisibleForTesting
    List<String> getRecentTags() {
        return getStoredTags(KEY_RECENT_TAGS);
    }

    @VisibleForTesting
    List<String> getPopularTags() {
        return getStoredTags(KEY_POPULAR_TAGS);
    }

    private List<String> getStoredTags(String key) {
        String storedTags = sharedPreferences.getString(key, "");
        if (Strings.isBlank(storedTags)) {
            return new LinkedList<>();
        }
        return deserialize(storedTags);
    }

    public void clear() {
        sharedPreferences.edit().clear().apply();
    }

    private String sanitizeTag(String tag) {
        return tag.replaceFirst(",.*", "");
    }

    private List<String> deserialize(String serializedTags) {
        return Arrays.asList(serializedTags.split(","));
    }

    private String serialize(List<String> tags) {
        return Strings.joinOn(",").join(tags);
    }
}
