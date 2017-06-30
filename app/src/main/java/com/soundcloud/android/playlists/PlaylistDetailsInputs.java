package com.soundcloud.android.playlists;

import static com.soundcloud.android.rx.RxSignal.SIGNAL;

import com.soundcloud.android.rx.RxSignal;
import io.reactivex.subjects.BehaviorSubject;
import io.reactivex.subjects.PublishSubject;

import java.util.List;

@SuppressWarnings({"PMD.ExcessiveParameterList"})
class PlaylistDetailsInputs {

    final BehaviorSubject<Boolean> editMode;
    final PublishSubject<RxSignal> refresh;
    final PublishSubject<RxSignal> playNext;
    final PublishSubject<RxSignal> delete;
    final PublishSubject<RxSignal> share;
    final PublishSubject<RxSignal> overflowUpsellImpression;
    final PublishSubject<RxSignal> firstTrackUpsellImpression;
    final PublishSubject<RxSignal> playShuffled;
    final PublishSubject<RxSignal> makeOfflineAvailable;
    final PublishSubject<RxSignal> offlineUnavailable;
    final PublishSubject<RxSignal> onCreatorClicked;
    final PublishSubject<RxSignal> onMakeOfflineUpsell;
    final PublishSubject<RxSignal> onOverflowMakeOfflineUpsell;
    final PublishSubject<PlaylistDetailUpsellItem> onUpsellItemClicked;
    final PublishSubject<PlaylistDetailUpsellItem> onUpsellDismissed;
    final PublishSubject<RxSignal> headerPlayClicked;
    final PublishSubject<Boolean> like;
    final PublishSubject<Boolean> repost;
    final PublishSubject<PlaylistDetailTrackItem> playFromTrack;
    final PublishSubject<List<PlaylistDetailTrackItem>> tracklistUpdated;

    public static PlaylistDetailsInputs create() {
        return new PlaylistDetailsInputs(BehaviorSubject.createDefault(false),
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
                                         PublishSubject.create(),
                                         PublishSubject.create());
    }

    PlaylistDetailsInputs(BehaviorSubject<Boolean> editMode,
                          PublishSubject<RxSignal> refresh,
                          PublishSubject<RxSignal> playNext,
                          PublishSubject<RxSignal> delete,
                          PublishSubject<RxSignal> share,
                          PublishSubject<RxSignal> overflowUpsellImpression,
                          PublishSubject<RxSignal> firstTrackUpsellImpression,
                          PublishSubject<RxSignal> playShuffled,
                          PublishSubject<RxSignal> makeOfflineAvailable,
                          PublishSubject<RxSignal> offlineUnavailable,
                          PublishSubject<RxSignal> onCreatorClicked,
                          PublishSubject<RxSignal> onMakeOfflineUpsell,
                          PublishSubject<RxSignal> onOverflowMakeOfflineUpsell,
                          PublishSubject<PlaylistDetailUpsellItem> onUpsellItemClicked,
                          PublishSubject<PlaylistDetailUpsellItem> onUpsellDismissed,
                          PublishSubject<RxSignal> headerPlayClicked,
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
        this.firstTrackUpsellImpression = firstTrackUpsellImpression;
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
        onCreatorClicked.onNext(SIGNAL);
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
        offlineUnavailable.onNext(SIGNAL);
    }

    void onMakeOfflineUpsell() {
        onMakeOfflineUpsell.onNext(SIGNAL);
    }

    void onHeaderPlayButtonClicked() {
        headerPlayClicked.onNext(SIGNAL);
    }

    void onPlayNext() {
        playNext.onNext(SIGNAL);
    }

    void onToggleLike(boolean isLiked) {
        like.onNext(isLiked);
    }

    void onToggleRepost(boolean isReposted) {
        repost.onNext(isReposted);
    }

    void onShareClicked() {
        share.onNext(SIGNAL);
    }

    void onMakeOfflineAvailable() {
        makeOfflineAvailable.onNext(SIGNAL);
    }

    void onOverflowUpsell() {
        onOverflowMakeOfflineUpsell.onNext(SIGNAL);
    }

    void onOverflowUpsellImpression() {
        overflowUpsellImpression.onNext(SIGNAL);
    }

    void onPlayShuffled() {
        playShuffled.onNext(SIGNAL);
    }

    void onItemTriggered(PlaylistDetailUpsellItem item) {
        onUpsellItemClicked.onNext(item);
    }

    void onItemDismissed(PlaylistDetailUpsellItem item) {
        onUpsellDismissed.onNext(item);
    }

    void onDeletePlaylist() {
        delete.onNext(SIGNAL);
    }

    void refresh() {
        refresh.onNext(SIGNAL);
    }
}
