package com.soundcloud.android.search;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Joiner;
import com.soundcloud.android.rx.ScSchedulers;
import com.soundcloud.android.rx.ScheduledOperations;
import com.soundcloud.android.storage.StorageModule;
import com.soundcloud.android.utils.ScTextUtils;
import rx.Observable;
import rx.Scheduler;
import rx.Subscriber;

import android.content.SharedPreferences;

import javax.inject.Inject;
import javax.inject.Named;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;

public class PlaylistTagStorage extends ScheduledOperations {

    private static final String KEY_RECENT_TAGS = "recent_tags";
    private static final String KEY_POPULAR_TAGS = "popular_tags";
    private static final int MAX_RECENT_TAGS = 5;

    private final SharedPreferences sharedPreferences;

    @Inject
    public PlaylistTagStorage(@Named(StorageModule.PLAYLIST_TAGS) SharedPreferences sharedPreferences) {
        this(sharedPreferences, ScSchedulers.HIGH_PRIO_SCHEDULER);
    }

    @VisibleForTesting
    PlaylistTagStorage(SharedPreferences sharedPreferences, Scheduler scheduler) {
        super(scheduler);
        this.sharedPreferences = sharedPreferences;
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
        sharedPreferences.edit().putString(KEY_POPULAR_TAGS, serialize(tags)).apply();
    }

    public Observable<List<String>> getRecentTagsAsync() {
        return schedule(Observable.create(new Observable.OnSubscribe<List<String>>() {
            @Override
            public void call(Subscriber<? super List<String>> subscriber) {
                subscriber.onNext(getRecentTags());
                subscriber.onCompleted();
            }
        }));
    }

    public Observable<List<String>> getPopularTagsAsync() {
        return schedule(Observable.create(new Observable.OnSubscribe<List<String>>() {
            @Override
            public void call(Subscriber<? super List<String>> observer) {
                observer.onNext(getPopularTags());
                observer.onCompleted();
            }
        }));
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
        if (ScTextUtils.isBlank(storedTags)) {
            return new LinkedList<>();
        }
        return deserialize(storedTags);
    }

    public void resetPopularTags() {
        sharedPreferences.edit().putString(KEY_POPULAR_TAGS, "").apply();
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
        return Joiner.on(",").join(tags);
    }
}
