package com.soundcloud.android.playlists;

import rx.subjects.BehaviorSubject;
import rx.subjects.PublishSubject;

import java.util.List;

class PlaylistDetailsInputs {
    final BehaviorSubject<Boolean> editMode;
    final PublishSubject<Void> refresh;
    final PublishSubject<Void> playNext;
    final PublishSubject<Void> delete;
    final PublishSubject<Void> share;
    final PublishSubject<Void> overflowUpsellImpression;
    final PublishSubject<Void> playShuffled;
    final PublishSubject<Void> makeOfflineAvailable;
    final PublishSubject<Void> offlineUnavailable;
    final PublishSubject<Void> onCreatorClicked;
    final PublishSubject<Void> onMakeOfflineUpsell;
    final PublishSubject<Void> onOverflowMakeOfflineUpsell;
    final PublishSubject<PlaylistDetailUpsellItem> onUpsellItemClicked;
    final PublishSubject<PlaylistDetailUpsellItem> onUpsellDismissed;
    final PublishSubject<Void> headerPlayClicked;
    final PublishSubject<Boolean> like;
    final PublishSubject<Boolean> repost;
    final PublishSubject<PlaylistDetailTrackItem> playFromTrack;
    final PublishSubject<List<PlaylistDetailTrackItem>> tracklistUpdated;

    public static PlaylistDetailsInputs create() {
        return new PlaylistDetailsInputs(BehaviorSubject.create(false),
                                         PublishSubject.create(),
                                         PublishSubject.create(),
                                         PublishSubject.create(),
                                         PublishSubject.create(),
                                         PublishSubject.create(),
                                         PublishSubject.create(),
                                         PublishSubject.create(),
                                         PublishSubject.create(),
                                         PublishSubject.create(),
                                         PublishSubject.create(),
                                         PublishSubject.create(),
                                         PublishSubject.create(),
                                         PublishSubject.create(),
                                         PublishSubject.create(),
                                         PublishSubject.create(),
                                         PublishSubject.create(),
                                         PublishSubject.create(),
                                         PublishSubject.create());
    }

    PlaylistDetailsInputs(BehaviorSubject<Boolean> editMode,
                          PublishSubject<Void> refresh,
                          PublishSubject<Void> playNext,
                          PublishSubject<Void> delete,
                          PublishSubject<Void> share,
                          PublishSubject<Void> overflowUpsellImpression,
                          PublishSubject<Void> playShuffled,
                          PublishSubject<Void> makeOfflineAvailable,
                          PublishSubject<Void> offlineUnavailable,
                          PublishSubject<Void> onCreatorClicked,
                          PublishSubject<Void> onMakeOfflineUpsell,
                          PublishSubject<Void> onOverflowMakeOfflineUpsell,
                          PublishSubject<PlaylistDetailUpsellItem> onUpsellItemClicked,
                          PublishSubject<PlaylistDetailUpsellItem> onUpsellDismissed,
                          PublishSubject<Void> headerPlayClicked,
                          PublishSubject<Boolean> like,
                          PublishSubject<Boolean> repost,
                          PublishSubject<PlaylistDetailTrackItem> playFromTrack,
                          PublishSubject<List<PlaylistDetailTrackItem>> tracklistUpdated) {
        this.editMode = editMode;
        this.refresh = refresh;
        this.playNext = playNext;
        this.delete = delete;
        this.share = share;
        this.overflowUpsellImpression = overflowUpsellImpression;
        this.playShuffled = playShuffled;
        this.makeOfflineAvailable = makeOfflineAvailable;
        this.offlineUnavailable = offlineUnavailable;
        this.onCreatorClicked = onCreatorClicked;
        this.onMakeOfflineUpsell = onMakeOfflineUpsell;
        this.onOverflowMakeOfflineUpsell = onOverflowMakeOfflineUpsell;
        this.onUpsellItemClicked = onUpsellItemClicked;
        this.onUpsellDismissed = onUpsellDismissed;
        this.headerPlayClicked = headerPlayClicked;
        this.like = like;
        this.repost = repost;
        this.playFromTrack = playFromTrack;
        this.tracklistUpdated = tracklistUpdated;
    }

    void actionUpdateTrackList(List<PlaylistDetailTrackItem> playlistDetailTrackItems) {
        this.tracklistUpdated.onNext(playlistDetailTrackItems);
    }

    void onCreatorClicked() {
        onCreatorClicked.onNext(null);
    }

    void onItemTriggered(PlaylistDetailTrackItem item) {
        playFromTrack.onNext(item);
    }

    void onEnterEditMode() {
        editMode.onNext(true);
    }

    void onExitEditMode() {
        editMode.onNext(false);
    }

    void onMakeOfflineUnavailable() {
        offlineUnavailable.onNext(null);
    }

    void onMakeOfflineUpsell() {
        onMakeOfflineUpsell.onNext(null);
    }

    void onHeaderPlayButtonClicked() {
        headerPlayClicked.onNext(null);
    }

    void onPlayNext() {
        playNext.onNext(null);
    }

    void onToggleLike(boolean isLiked) {
        like.onNext(isLiked);
    }

    void onToggleRepost(boolean isReposted) {
        repost.onNext(isReposted);
    }

    void onShareClicked() {
        share.onNext(null);
    }

    void onMakeOfflineAvailable() {
        makeOfflineAvailable.onNext(null);
    }

    void onOverflowUpsell() {
        onOverflowMakeOfflineUpsell.onNext(null);
    }

    void onOverflowUpsellImpression() {
        overflowUpsellImpression.onNext(null);
    }

    void onPlayShuffled() {
        playShuffled.onNext(null);
    }

    void onItemTriggered(PlaylistDetailUpsellItem item) {
        onUpsellItemClicked.onNext(item);
    }

    void onItemDismissed(PlaylistDetailUpsellItem item) {
        onUpsellDismissed.onNext(item);
    }

    void onDeletePlaylist() {
        delete.onNext(null);
    }

    void refresh() {
        refresh.onNext(null);
    }
}
