package com.soundcloud.android.playback.ui;

import com.soundcloud.android.R;
import com.soundcloud.android.ads.AdProperty;
import com.soundcloud.android.api.model.StationRecord;
import com.soundcloud.android.cast.CastConnectionHelper;
import com.soundcloud.android.events.CurrentPlayQueueItemEvent;
import com.soundcloud.android.events.EntityStateChangedEvent;
import com.soundcloud.android.events.EventQueue;
import com.soundcloud.android.events.PlayControlEvent;
import com.soundcloud.android.events.PlaybackProgressEvent;
import com.soundcloud.android.events.PlayerUIEvent;
import com.soundcloud.android.model.PlayableProperty;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.playback.PlayQueueManager;
import com.soundcloud.android.playback.PlaySessionStateProvider;
import com.soundcloud.android.playback.Player;
import com.soundcloud.android.playback.Player.StateTransition;
import com.soundcloud.android.playback.ui.view.PlayerTrackPager;
import com.soundcloud.android.rx.observers.DefaultSubscriber;
import com.soundcloud.android.stations.StationsOperations;
import com.soundcloud.android.tracks.TrackProperty;
import com.soundcloud.android.tracks.TrackRepository;
import com.soundcloud.java.collections.PropertySet;
import com.soundcloud.lightcycle.DefaultSupportFragmentLightCycle;
import com.soundcloud.rx.eventbus.EventBus;
import rx.Observable;
import rx.android.schedulers.AndroidSchedulers;
import rx.functions.Action1;
import rx.functions.Func1;
import rx.functions.Func2;
import rx.subjects.ReplaySubject;
import rx.subscriptions.CompositeSubscription;

import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.util.LruCache;
import android.support.v4.view.PagerAdapter;
import android.view.View;
import android.view.ViewGroup;

import javax.inject.Inject;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class PlayerPagerPresenter extends DefaultSupportFragmentLightCycle<PlayerFragment>
        implements CastConnectionHelper.OnConnectionChangeListener {

    static final int PAGE_VIEW_POOL_SIZE = 6;
    private static final int TYPE_TRACK_VIEW = 0;
    private static final int TYPE_AUDIO_AD_VIEW = 1;
    private static final int TYPE_VIDEO_AD_VIEW = 2;
    private static final int TRACK_CACHE_SIZE = 10;

    private final PlayQueueManager playQueueManager;
    private final PlaySessionStateProvider playSessionStateProvider;
    private final TrackRepository trackRepository;
    private final TrackPagePresenter trackPagePresenter;
    private final AdPagePresenter adPagePresenter;
    private final VideoPagePresenter videoPagePresenter;
    private final CastConnectionHelper castConnectionHelper;
    private final EventBus eventBus;
    private final StationsOperations stationsOperations;
    private final TrackPageRecycler trackPageRecycler;

    private final Map<View, PlayerPageData> pagesInPlayer = new HashMap<>(PAGE_VIEW_POOL_SIZE);
    private final TrackPagerAdapter trackPagerAdapter;

    private CompositeSubscription foregroundSubscription = new CompositeSubscription();
    private CompositeSubscription backgroundSubscription = new CompositeSubscription();

    private View audioAdView;
    private View videoAdView;
    private SkipListener skipListener;
    private List<PlayerPageData> currentData = Collections.emptyList();
    private ViewVisibilityProvider viewVisibilityProvider = ViewVisibilityProvider.EMPTY;
    private PlayerUIEvent lastPlayerUIEvent;
    private StateTransition lastStateTransition;
    private boolean isForeground;

    private final LruCache<Urn, ReplaySubject<PropertySet>> trackObservableCache =
            new LruCache<>(TRACK_CACHE_SIZE);

    private final Func1<PlaybackProgressEvent, Boolean> currentTrackFilter = new Func1<PlaybackProgressEvent, Boolean>() {
        @Override
        public Boolean call(PlaybackProgressEvent progressEvent) {
            return playQueueManager.isCurrentTrack(progressEvent.getTrackUrn());
        }
    };

    private static final Func1<PropertySet, PlayerItem> TO_PLAYER_AD = new Func1<PropertySet, PlayerItem>() {
        @Override
        public PlayerItem call(PropertySet propertySet) {
            return new PlayerAd(propertySet);
        }
    };
    private final Func1<PropertySet, PlayerTrackState> toPlayerTrack = new Func1<PropertySet, PlayerTrackState>() {
        @Override
        public PlayerTrackState call(PropertySet propertySet) {
            return new PlayerTrackState(propertySet,
                    playQueueManager.isCurrentTrack(propertySet.get(TrackProperty.URN)),
                    isForeground, viewVisibilityProvider
            );
        }
    };

    @Inject
    PlayerPagerPresenter(PlayQueueManager playQueueManager,
                         PlaySessionStateProvider playSessionStateProvider,
                         TrackRepository trackRepository,
                         StationsOperations stationsOperations, TrackPagePresenter trackPagePresenter,
                         AdPagePresenter adPagePresenter,
                         VideoPagePresenter videoPagePresenter,
                         CastConnectionHelper castConnectionHelper,
                         EventBus eventBus) {
        this.playQueueManager = playQueueManager;
        this.trackRepository = trackRepository;
        this.trackPagePresenter = trackPagePresenter;
        this.playSessionStateProvider = playSessionStateProvider;
        this.adPagePresenter = adPagePresenter;
        this.videoPagePresenter = videoPagePresenter;
        this.castConnectionHelper = castConnectionHelper;
        this.eventBus = eventBus;
        this.stationsOperations = stationsOperations;

        this.trackPagerAdapter = new TrackPagerAdapter();
        this.trackPageRecycler = new TrackPageRecycler();
    }

    public void setCurrentData(List<PlayerPageData> data) {
        currentData = data;
        trackPagerAdapter.notifyDataSetChanged();
    }

    void onPlayerSlide(float slideOffset) {
        for (Map.Entry<View, PlayerPageData> entry : pagesInPlayer.entrySet()) {
            pagePresenter(entry.getValue()).onPlayerSlide(entry.getKey(), slideOffset);
        }
    }

    int getPlayQueuePosition(int position) {
        return currentData.get(position).getPositionInPlayQueue();
    }

    boolean isAdPageAtPosition(int position) {
        return currentData.get(position).isAdPage();
    }

    @Override
    public void onViewCreated(PlayerFragment fragment, View view, Bundle savedInstanceState) {
        final PlayerTrackPager trackPager = (PlayerTrackPager) view.findViewById(R.id.player_track_pager);

        viewVisibilityProvider = new PlayerViewVisibilityProvider(trackPager);
        trackPager.setPageMargin(view.getResources().getDimensionPixelSize(R.dimen.player_pager_spacing));
        trackPager.setPageMarginDrawable(R.color.black);
        trackPager.setAdapter(trackPagerAdapter);

        skipListener = createSkipListener(trackPager);
        castConnectionHelper.addOnConnectionChangeListener(this);
        populateScrapViews(trackPager);

        setupPlayerPanelSubscriber();
        setupTrackMetadataChangedSubscriber();
        setupClearAdOverlaySubscriber();
    }

    private void setupClearAdOverlaySubscriber() {
        backgroundSubscription.add(eventBus
                .queue(EventQueue.CURRENT_PLAY_QUEUE_ITEM)
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new ClearAdOverlaySubscriber()));
    }

    private void setupPlayerPanelSubscriber() {
        backgroundSubscription.add(eventBus
                .subscribe(EventQueue.PLAYER_UI, new PlayerPanelSubscriber()));
    }

    private void setupTrackMetadataChangedSubscriber() {
        backgroundSubscription.add(eventBus
                .queue(EventQueue.ENTITY_STATE_CHANGED)
                .filter(EntityStateChangedEvent.IS_TRACK_FILTER)
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new TrackMetadataChangedSubscriber()));
    }

    @Override
    public void onResume(PlayerFragment playerFragment) {
        isForeground = true;

        setupPlaybackStateSubscriber();
        setupPlaybackProgressSubscriber();

        for (Map.Entry<View, PlayerPageData> entry : pagesInPlayer.entrySet()) {
            final PlayerPageData pageData = entry.getValue();
            final PlayerPagePresenter presenter = pagePresenter(pageData);
            final View view = entry.getKey();
            presenter.onForeground(view);
        }
    }

    private void setupPlaybackStateSubscriber() {
        foregroundSubscription.add(
                eventBus.queue(EventQueue.PLAYBACK_STATE_CHANGED)
                        .observeOn(AndroidSchedulers.mainThread())
                        .subscribe(new PlayerPagerPresenter.PlaybackStateSubscriber()));
    }

    private void setupPlaybackProgressSubscriber() {
        foregroundSubscription.add(eventBus
                .queue(EventQueue.PLAYBACK_PROGRESS)
                .filter(currentTrackFilter)
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new PlayerPagerPresenter.PlaybackProgressSubscriber()));
    }

    @Override
    public void onPause(PlayerFragment playerFragment) {
        isForeground = false;

        foregroundSubscription.unsubscribe();
        foregroundSubscription = new CompositeSubscription();

        for (Map.Entry<View, PlayerPageData> entry : pagesInPlayer.entrySet()) {
            pagePresenter(entry.getValue()).onBackground(entry.getKey());
        }
    }

    @Override
    public void onDestroyView(PlayerFragment playerFragment) {
        for (Map.Entry<View, PlayerPageData> entry : pagesInPlayer.entrySet()) {
            pagePresenter(entry.getValue()).onDestroyView(entry.getKey());
        }

        castConnectionHelper.removeOnConnectionChangeListener(this);
        skipListener = null;
        viewVisibilityProvider = null;

        backgroundSubscription.unsubscribe();
        backgroundSubscription = new CompositeSubscription();

        super.onDestroyView(playerFragment);
    }

    @NonNull
    private SkipListener createSkipListener(final PlayerTrackPager trackPager) {
        return new SkipListener() {
            @Override
            public void onNext() {
                trackPager.setCurrentItem(trackPager.getCurrentItem() + 1);
                eventBus.publish(EventQueue.TRACKING, PlayControlEvent.skip(PlayControlEvent.SOURCE_FULL_PLAYER));
            }

            @Override
            public void onPrevious() {
                trackPager.setCurrentItem(trackPager.getCurrentItem() - 1);
                eventBus.publish(EventQueue.TRACKING, PlayControlEvent.previous(PlayControlEvent.SOURCE_FULL_PLAYER));
            }
        };
    }

    private void populateScrapViews(PlayerTrackPager trackPager) {
        for (int i = 0; i < PAGE_VIEW_POOL_SIZE; i++) {
            final View itemView = trackPagePresenter.createItemView(trackPager, skipListener);
            trackPageRecycler.addScrapView(itemView);
        }
    }

    public int getItemViewType(int position) {
        final PlayerPageData pageData = currentData.get(position);
        if (pageData.isAdPage()) {
            return pageData.isVideoPage() ? TYPE_VIDEO_AD_VIEW : TYPE_AUDIO_AD_VIEW;
        } else {
            return TYPE_TRACK_VIEW;
        }
    }

    public boolean isTrackView(Object object) {
        return trackPagePresenter.accept((View) object);
    }

    @Override
    public void onCastConnectionChange() {
        for (Map.Entry<View, PlayerPageData> entry : pagesInPlayer.entrySet()) {
            pagePresenter(entry.getValue()).setCastDeviceName(entry.getKey(), castConnectionHelper.getDeviceName());
        }
    }

    private View bindView(int position, final View view) {
        final PlayerPageData playerPageData = currentData.get(position);
        pagesInPlayer.put(view, playerPageData);

        final PlayerPagePresenter presenter = pagePresenter(playerPageData);

        if (isForeground){
            // this will attach the cast button
            presenter.onForeground(view);
        }

        foregroundSubscription.add(getTrackOrAdObservable(playerPageData)
                .observeOn(AndroidSchedulers.mainThread())
                .filter(isPlayerItemRelatedToView(view))
                .subscribe(new PlayerItemSubscriber(presenter, view)));

        return view;
    }

    private void configureInitialPageState(final View view) {
        final PlayerPagePresenter presenter = pagePresenter(pagesInPlayer.get(view));
        if (lastPlayerUIEvent != null) {
            configurePageFromUiEvent(lastPlayerUIEvent, presenter, view);
        }

        if (lastStateTransition != null) {
            configurePageFromPlayerState(lastStateTransition, presenter, view);
        }
    }

    private PlayerPagePresenter pagePresenter(PlayerPageData playerPageData) {
        if (playerPageData.isAdPage()) {
            return playerPageData.isTrackPage() ? adPagePresenter : videoPagePresenter;
        } else {
            return trackPagePresenter;
        }
    }

    private Observable<? extends PlayerItem> getTrackOrAdObservable(PlayerPageData viewData) {
        if (viewData.isVideoPage()) {
            return getVideoAdObservable(viewData.getProperties()).map(TO_PLAYER_AD);
        }

        final TrackPageData trackData = (TrackPageData) viewData;
        if (trackData.isAdPage()) {
            return getAudioAdObservable(trackData.getTrackUrn(), trackData.getProperties())
                    .map(TO_PLAYER_AD);
        } else if (trackData.isTrackPage() && trackData.getCollectionUrn().isStation()) {
            return getStationObservable(trackData);
        } else {
            return getTrackObservable(trackData.getTrackUrn(), trackData.getProperties())
                    .map(toPlayerTrack);
        }
    }

    private Observable<? extends PlayerItem> getStationObservable(TrackPageData trackPageData)  {
        return Observable.zip(
                getTrackObservable(trackPageData.getTrackUrn(), trackPageData.getProperties()).map(toPlayerTrack),
                stationsOperations.station(trackPageData.getCollectionUrn()),
                new Func2<PlayerTrackState, StationRecord, PlayerItem>() {
                    @Override
                    public PlayerItem call(PlayerTrackState playerTrackState, StationRecord station) {
                        playerTrackState.setStation(station);
                        return playerTrackState;
                    }
                }
        );
    }

    private Observable<PropertySet> getTrackObservable(Urn urn, final PropertySet adOverlayData) {
        return getTrackObservable(urn).doOnNext(new Action1<PropertySet>() {
            @Override
            public void call(PropertySet track) {
                adOverlayData.put(TrackProperty.URN, track.get(TrackProperty.URN))
                        .put(TrackProperty.TITLE, track.get(TrackProperty.TITLE))
                        .put(TrackProperty.CREATOR_NAME, track.get(TrackProperty.CREATOR_NAME))
                        .put(TrackProperty.CREATOR_URN, track.get(TrackProperty.CREATOR_URN));
            }
        });
    }

    private Observable<PropertySet> getVideoAdObservable(final PropertySet videoAd) {
        return getTrackObservable(videoAd.get(AdProperty.MONETIZABLE_TRACK_URN)).map(
                new Func1<PropertySet, PropertySet>() {
                    @Override
                    public PropertySet call(PropertySet monetizableTrack) {
                        return videoAd
                                .put(AdProperty.MONETIZABLE_TRACK_TITLE, monetizableTrack.get(PlayableProperty.TITLE))
                                .put(AdProperty.MONETIZABLE_TRACK_CREATOR, monetizableTrack.get(PlayableProperty.CREATOR_NAME));
                    }
                });
    }

    private Observable<PropertySet> getAudioAdObservable(Urn urn, final PropertySet audioAd) {
        // merge together audio ad track data and track data from the upcoming monetizable track
        return Observable.zip(getTrackObservable(urn), getTrackObservable(audioAd.get(AdProperty.MONETIZABLE_TRACK_URN)),
                new Func2<PropertySet, PropertySet, PropertySet>() {
                    @Override
                    public PropertySet call(PropertySet audioAdTrack, PropertySet monetizableTrack) {
                        return audioAdTrack.merge(audioAd)
                                .put(AdProperty.MONETIZABLE_TRACK_TITLE, monetizableTrack.get(PlayableProperty.TITLE))
                                .put(AdProperty.MONETIZABLE_TRACK_CREATOR, monetizableTrack.get(PlayableProperty.CREATOR_NAME));
                    }
                });
    }

    @NonNull
    private Func1<PlayerItem, Boolean> isPlayerItemRelatedToView(final View pageView) {
        return new Func1<PlayerItem, Boolean>() {
            @Override
            public Boolean call(PlayerItem playerItem) {
                return isPlayerItemRelatedToView(pageView, playerItem);
            }
        };
    }

    void onTrackChange() {
        for (Map.Entry<View, PlayerPageData> entry : pagesInPlayer.entrySet()) {
            final View trackView = entry.getKey();
            if (isTrackView(trackView)) {
                final TrackPageData trackPageData = (TrackPageData) entry.getValue();
                final Urn urn = trackPageData.getTrackUrn();
                trackPagePresenter.onPageChange(trackView);
                updateProgress(trackPagePresenter, trackView, urn);
            }
        }
    }

    private void onTrackPageSet(View view, int position) {
        final TrackPageData trackPageData = (TrackPageData) currentData.get(position);
        trackPagePresenter.onPositionSet(view, position, currentData.size());
        trackPagePresenter.setCastDeviceName(view, castConnectionHelper.getDeviceName());
        if (trackPageData.hasAdOverlay()){
            trackPagePresenter.setAdOverlay(view, trackPageData.getProperties());
        } else {
            trackPagePresenter.clearAdOverlay(view);
        }
    }

    private Observable<PropertySet> getTrackObservable(Urn urn) {
        ReplaySubject<PropertySet> trackSubject = trackObservableCache.get(urn);
        if (trackSubject == null) {
            trackSubject = ReplaySubject.create();
            trackRepository
                    .track(urn)
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe(trackSubject);
            trackObservableCache.put(urn, trackSubject);
        }
        return trackSubject;
    }

    private Boolean isPlayerItemRelatedToView(View pageView, PlayerItem item) {
        if (item.source.contains(AdProperty.AD_URN)) {
            return pagesInPlayer.containsKey(pageView) &&
                    pagesInPlayer.get(pageView).isAdPage() &&
                    pagesInPlayer.get(pageView).getProperties().get(AdProperty.AD_URN).equals(item.source.get(AdProperty.AD_URN));
        } else {
            return isTrackViewRelatedToUrn(pageView, item.getTrackUrn());
        }
    }

    private boolean isTrackViewRelatedToUrn(View pageView, Urn trackUrn) {
        if (pagesInPlayer.containsKey(pageView) && pagesInPlayer.get(pageView).isTrackPage()) {
            final TrackPageData trackPageData = (TrackPageData) pagesInPlayer.get(pageView);
            return trackPageData.getTrackUrn().equals(trackUrn);
        }
        return trackPageRecycler.isPageForUrn(pageView, trackUrn);
    }

    private void updateProgress(PlayerPagePresenter presenter, View trackView, Urn urn) {
        presenter.setProgress(trackView, playSessionStateProvider.getLastProgressForTrack(urn));
    }

    private static class PlayerItemSubscriber extends DefaultSubscriber<PlayerItem> {
        private final PlayerPagePresenter presenter;
        private final View pageView;

        public PlayerItemSubscriber(PlayerPagePresenter presenter, View pageView) {
            this.presenter = presenter;
            this.pageView = pageView;
        }

        @Override
        public void onNext(PlayerItem playerItem) {
            presenter.bindItemView(pageView, playerItem);
        }
    }

    private final class PlayerPanelSubscriber extends DefaultSubscriber<PlayerUIEvent> {
        @Override
        public void onNext(PlayerUIEvent event) {
            lastPlayerUIEvent = event;

            for (Map.Entry<View, PlayerPageData> entry : pagesInPlayer.entrySet()) {
                final PlayerPagePresenter presenter = pagePresenter(entry.getValue());
                final View view = entry.getKey();
                configurePageFromUiEvent(event, presenter, view);
            }
        }
    }

    private final class PlaybackStateSubscriber extends DefaultSubscriber<Player.StateTransition> {
        @Override
        public void onNext(Player.StateTransition stateTransition) {
            lastStateTransition = stateTransition;

            for (Map.Entry<View, PlayerPageData> entry : pagesInPlayer.entrySet()) {
                final PlayerPageData pageData = entry.getValue();
                final PlayerPagePresenter presenter = pagePresenter(pageData);
                final View view = entry.getKey();
                configurePageFromPlayerState(stateTransition, presenter, view);
            }
        }
    }

    private final class PlaybackProgressSubscriber extends DefaultSubscriber<PlaybackProgressEvent> {
        @Override
        public void onNext(PlaybackProgressEvent progress) {
            for (Map.Entry<View, PlayerPageData> entry : pagesInPlayer.entrySet()) {
                final PlayerPagePresenter presenter = pagePresenter(entry.getValue());
                final View trackView = entry.getKey();
                if (isTrackViewRelatedToUrn(trackView, progress.getTrackUrn())) {
                    presenter.setProgress(trackView, progress.getPlaybackProgress());
                }
            }
        }
    }

    private class TrackMetadataChangedSubscriber extends DefaultSubscriber<EntityStateChangedEvent> {
        @Override
        public void onNext(EntityStateChangedEvent event) {
            trackObservableCache.remove(event.getFirstUrn());

            for (Map.Entry<View, PlayerPageData> entry : pagesInPlayer.entrySet()) {
                final PlayerPagePresenter presenter = pagePresenter(entry.getValue());
                final View trackView = entry.getKey();
                if (isTrackViewRelatedToUrn(trackView, event.getFirstUrn())) {
                    presenter.onPlayableUpdated(trackView, event);
                }
            }
        }
    }

    private final class ClearAdOverlaySubscriber extends DefaultSubscriber<CurrentPlayQueueItemEvent>{
        @Override
        public void onNext(CurrentPlayQueueItemEvent ignored) {
            for (Map.Entry<View, PlayerPageData> entry : pagesInPlayer.entrySet()) {
                final PlayerPageData pageData = entry.getValue();
                final PlayerPagePresenter presenter = pagePresenter(pageData);
                final View trackView = entry.getKey();

                if (pageData.isTrackPage()) {
                    final TrackPageData trackData = (TrackPageData) pageData;
                    if (!playQueueManager.isCurrentTrack(trackData.getTrackUrn())) {
                        presenter.clearAdOverlay(trackView);
                    }
                }
            }
        }
    }

    private void configurePageFromPlayerState(StateTransition stateTransition, PlayerPagePresenter presenter, View view) {
        final boolean viewPresentingCurrentTrack = pagesInPlayer.containsKey(view)
                && pagesInPlayer.get(view).isTrackPage()
                && isTrackViewRelatedToUrn(view, stateTransition.getTrackUrn());

        presenter.setPlayState(view, stateTransition, viewPresentingCurrentTrack, isForeground);
    }

    private void configurePageFromUiEvent(PlayerUIEvent event, PlayerPagePresenter presenter, View view) {
        final int kind = event.getKind();
        if (kind == PlayerUIEvent.PLAYER_EXPANDED) {
            presenter.setExpanded(view);
        } else if (kind == PlayerUIEvent.PLAYER_COLLAPSED) {
            presenter.setCollapsed(view);
        }
    }

    private class TrackPagerAdapter extends PagerAdapter {

        @Override
        public int getItemPosition(Object object) {
            return POSITION_NONE;
        }

        @Override
        public int getCount() {
            return currentData.size();
        }

        @Override
        public final Object instantiateItem(ViewGroup container, int position) {
            View view;
            switch (getItemViewType(position)) {
                case TYPE_AUDIO_AD_VIEW:
                    view = instantiateAdView(adPagePresenter, container, position);
                    break;
                case TYPE_VIDEO_AD_VIEW:
                    view = instantiateAdView(videoPagePresenter, container, position);
                    break;
                default:
                    view = instantiateTrackView(position);
                    break;
            }

            configureInitialPageState(view);
            container.addView(view);
            return view;
        }

        private View instantiateTrackView(int position) {
            final View view;
            final TrackPageData trackPageData = (TrackPageData) currentData.get(position);
            final Urn urn = trackPageData.getTrackUrn();

            if (trackPageRecycler.hasExistingPage(urn)){
                view = trackPageRecycler.removePageByUrn(urn);
                if (!isForeground){
                    trackPagePresenter.onBackground(view);
                }
            } else {
                view = trackPageRecycler.getRecycledPage();
                pagePresenter(trackPageData).clearItemView(view);
            }

            bindView(position, view);
            onTrackPageSet(view, position);
            return view;
        }

        private View instantiateAdView(PlayerPagePresenter presenter, ViewGroup container, int position) {
            final PlayerPageData playerPageData = currentData.get(position);

            if (playerPageData.isTrackPage() && audioAdView == null) {
                audioAdView = presenter.createItemView(container, skipListener);
            } else if (playerPageData.isVideoPage() && videoAdView == null) {
                videoAdView = presenter.createItemView(container, skipListener);
            }

            return bindView(position, playerPageData.isTrackPage() ? audioAdView : videoAdView);
        }

        @Override
        public void destroyItem(ViewGroup container, int position, Object object) {
            View view = (View) object;
            container.removeView(view);

            if (isTrackView(view)) {
                final TrackPageData pageData = (TrackPageData) pagesInPlayer.get(view);
                final Urn trackUrn = pageData.getTrackUrn();
                trackPageRecycler.recyclePage(trackUrn, view);
                if (!playQueueManager.isCurrentTrack(trackUrn)) {
                    trackPagePresenter.onBackground(view);
                }
            }

            pagesInPlayer.remove(view);
        }

        @Override
        public boolean isViewFromObject(View view, Object object) {
            return view.equals(object);
        }
    }
}
