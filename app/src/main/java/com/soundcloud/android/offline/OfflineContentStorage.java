package com.soundcloud.android.offline;

import com.soundcloud.android.ApplicationModule;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.storage.StorageModule;
import com.soundcloud.android.utils.Urns;
import com.soundcloud.java.collections.Lists;
import com.soundcloud.java.collections.Sets;
import io.reactivex.Completable;
import io.reactivex.Scheduler;
import io.reactivex.Single;
import io.reactivex.functions.Action;

import android.annotation.SuppressLint;
import android.content.SharedPreferences;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Provider;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class OfflineContentStorage {

    private static final String KEY_OFFLINE_LIKES = "likes_marked_for_offline";
    private static final String KEY_OFFLINE_PLAYLISTS = "playlists_marked_for_offline";

    private final SharedPreferences offlineContent;
    private final Scheduler scheduler;

    @Inject
    public OfflineContentStorage(@Named(StorageModule.OFFLINE_CONTENT) SharedPreferences offlineContent,
                                 @Named(ApplicationModule.RX_HIGH_PRIORITY) Scheduler scheduler) {
        this.offlineContent = offlineContent;
        this.scheduler = scheduler;
    }

    Single<Boolean> isOfflinePlaylist(Urn playlistUrn) {
        return Single.fromCallable(() -> offlineContent.getStringSet(KEY_OFFLINE_PLAYLISTS, Collections.emptySet()).contains(playlistUrn.toString()));
    }

    public Single<Boolean> isOfflineLikesEnabled() {
        return Single.fromCallable(() -> offlineContent.getBoolean(KEY_OFFLINE_LIKES, false));
    }

    public Completable removePlaylistsFromOffline(Urn playlist) {
        return removePlaylistsFromOffline(Collections.singletonList(playlist));
    }

    Completable storeAsOfflinePlaylists(final List<Urn> playlistUrns) {
        return updatePlaylists(() -> {
            Set<String> playlists = Sets.newHashSet(offlineContent.getStringSet(KEY_OFFLINE_PLAYLISTS, new HashSet<>()));
            playlists.addAll(Urns.toString(playlistUrns));
            return playlists;
        });
    }

    Completable removePlaylistsFromOffline(List<Urn> playlistUrns) {
        return updatePlaylists(() -> {
            Set<String> playlists = Sets.newHashSet(offlineContent.getStringSet(KEY_OFFLINE_PLAYLISTS, Collections.emptySet()));
            playlists.removeAll(Urns.toString(playlistUrns));
            return playlists;
        });
    }

    Completable resetOfflinePlaylists(final List<Urn> expectedOfflinePlaylists) {
        return updatePlaylists(() -> new HashSet<>(Urns.toString(expectedOfflinePlaylists)));
    }

    public Single<List<Urn>> getOfflinePlaylists() {
        return Single.fromCallable(() -> new ArrayList<>(offlineContent.getStringSet(KEY_OFFLINE_PLAYLISTS, Collections.emptySet())))
                     .map(strings -> Lists.transform(strings, Urn::new));
    }

    @SuppressLint("ApplySharedPref")
    private Completable updatePlaylists(Provider<Set<String>> playlistsProvider) {
        return asyncCompletable(() -> offlineContent.edit().putStringSet(KEY_OFFLINE_PLAYLISTS, playlistsProvider.get()).commit());
    }

    @SuppressLint("ApplySharedPref")
    Completable removeLikedTrackCollection() {
        return asyncCompletable(() -> offlineContent.edit().putBoolean(KEY_OFFLINE_LIKES, false).commit());
    }

    @SuppressLint("ApplySharedPref")
    Completable removeAllOfflineContent() {
        return asyncCompletable(() -> offlineContent.edit().clear().commit());
    }

    @SuppressLint("ApplySharedPref")
    Completable addLikedTrackCollection() {
        return asyncCompletable(() -> offlineContent.edit().putBoolean(KEY_OFFLINE_LIKES, true).commit());
    }

    private Completable asyncCompletable(Action action) {
        return Completable.fromAction(action).subscribeOn(scheduler);
    }
}
