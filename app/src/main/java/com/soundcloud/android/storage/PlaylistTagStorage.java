package com.soundcloud.android.storage;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Joiner;
import com.soundcloud.android.model.PlaylistTagsCollection;
import com.soundcloud.android.rx.ScheduledOperations;
import com.soundcloud.android.utils.ScTextUtils;
import rx.Observable;
import rx.Subscriber;

import android.content.Context;
import android.content.SharedPreferences;

import javax.inject.Inject;
import javax.inject.Named;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;

public class PlaylistTagStorage extends ScheduledOperations {

    private static final String KEY_RECENT_TAGS = "recent_tags";
    private static final int MAX_TAGS = 5;

    private final SharedPreferences mSharedPreferences;

    @Inject
    public PlaylistTagStorage(@Named("PlaylistTags") SharedPreferences sharedPreferences) {
        mSharedPreferences = sharedPreferences;
    }

    public PlaylistTagStorage(Context context) {
        mSharedPreferences = context.getSharedPreferences(StorageModule.PLAYLIST_TAGS, Context.MODE_PRIVATE);
    }

    public void addRecentTag(String tag) {
        LinkedList<String> recentTags = new LinkedList<String>(getRecentTags());
        if (recentTags.contains(tag)) {
            return;
        }
        if (recentTags.size() == MAX_TAGS) {
            recentTags.removeLast();
        }
        recentTags.addFirst(sanitizeTag(tag));

        mSharedPreferences.edit().putString(KEY_RECENT_TAGS, serialize(recentTags)).apply();
    }

    public Observable<PlaylistTagsCollection> getRecentTagsAsync() {
        return schedule(Observable.create(new Observable.OnSubscribe<PlaylistTagsCollection>() {
            @Override
            public void call(Subscriber<? super PlaylistTagsCollection> observer) {
                observer.onNext(new PlaylistTagsCollection(getRecentTags()));
                observer.onCompleted();
            }
        }));
    }

    public void clear() {
        mSharedPreferences.edit().clear().apply();
    }

    @VisibleForTesting
    List<String> getRecentTags() {
        String storedTags = mSharedPreferences.getString(KEY_RECENT_TAGS, "");
        if (ScTextUtils.isBlank(storedTags)) {
            return new LinkedList<String>();
        }
        return deserialize(storedTags);
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
