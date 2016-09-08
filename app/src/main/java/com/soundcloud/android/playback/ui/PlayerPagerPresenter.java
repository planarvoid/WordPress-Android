package com.soundcloud.android.playback.ui;

import static com.soundcloud.android.ads.AdUtils.hasAdOverlay;

import com.soundcloud.android.Consts;
import com.soundcloud.android.R;
import com.soundcloud.android.ads.AdData;
import com.soundcloud.android.ads.AdUtils;
import com.soundcloud.android.ads.AdsOperations;
import com.soundcloud.android.ads.AudioAd;
import com.soundcloud.android.ads.OverlayAdData;
import com.soundcloud.android.ads.VideoAd;
import com.soundcloud.android.cast.CastConnectionHelper;
import com.soundcloud.android.events.CurrentPlayQueueItemEvent;
import com.soundcloud.android.events.EntityStateChangedEvent;
import com.soundcloud.android.events.EventQueue;
import com.soundcloud.android.events.PlaybackProgressEvent;
import com.soundcloud.android.events.PlayerUIEvent;
import com.soundcloud.android.model.PlayableProperty;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.playback.PlayQueueItem;
import com.soundcloud.android.playback.PlayQueueManager;
import com.soundcloud.android.playback.PlaySessionStateProvider;
import com.soundcloud.android.playback.PlayStateEvent;
import com.soundcloud.android.playback.VideoSurfaceProvider;
import com.soundcloud.android.playback.ui.view.PlayerTrackPager;
import com.soundcloud.android.rx.observers.DefaultSubscriber;
import com.soundcloud.android.stations.StationRecord;
import com.soundcloud.android.stations.StationsOperations;
import com.soundcloud.android.tracks.TrackProperty;
import com.soundcloud.android.tracks.TrackRepository;
import com.soundcloud.java.collections.PropertySet;
import com.soundcloud.java.optional.Optional;
import com.soundcloud.lightcycle.DefaultSupportFragmentLightCycle;
import com.soundcloud.rx.eventbus.EventBus;
import org.jetbrains.annotations.NotNull;
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
import android.support.v4.view.ViewPager;
import android.view.TextureView;
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
    private final AudioAdPresenter audioAdPresenter;
    private final VideoAdPresenter videoAdPresenter;
    private final CastConnectionHelper castConnectionHelper;
    private final AdsOperations adOperations;
    private final EventBus eventBus;
    private final StationsOperations stationsOperations;
    private final TrackPageRecycler trackPageRecycler;
    private final VideoSurfaceProvider videoSurfaceProvider;

    private final Map<View, PlayQueueItem> pagesInPlayer = new HashMap<>(PAGE_VIEW_POOL_SIZE);
    private final TrackPagerAdapter trackPagerAdapter;

    private CompositeSubscription foregroundSubscription = new CompositeSubscription();
    private CompositeSubscription backgroundSubscription = new CompositeSubscription();

    private View audioAdView;
    private View videoAdView;

    private SkipListener skipListener;

    private List<PlayQueueItem> currentPlayQueue = Collections.emptyList();
    @NotNull private ViewVisibilityProvider viewVisibilityProvider = ViewVisibilityProvider.EMPTY;
    private PlayerUIEvent lastPlayerUIEvent;
    private PlayStateEvent lastPlayStateEvent;
    private boolean isForeground;

    private final LruCache<Urn, ReplaySubject<PropertySet>> trackObservableCache =
            new LruCache<>(TRACK_CACHE_SIZE);

    private final Func1<PlaybackProgressEvent, Boolean> currentPlayQueueItemFilter = new Func1<PlaybackProgressEvent, Boolean>() {
        @Override
        public Boolean call(PlaybackProgressEvent progressEvent) {
            final PlayQueueItem currentItem = playQueueManager.getCurrentPlayQueueItem();
            return !currentItem.isEmpty() && currentItem.getUrn().equals(progressEvent.getUrn());
        }
    };

    private static final Func1<AdData, PlayerItem> TO_PLAYER_AD = new Func1<AdData, PlayerItem>() {
        @Override
        public PlayerItem call(AdData adData) {
            return adData instanceof VideoAd
                    ? new VideoPlayerAd((VideoAd) adData)
                    : new AudioPlayerAd((AudioAd) adData);
        }
    };

    private final ViewPager.OnPageChangeListener pageChangeListener = new ViewPager.SimpleOnPageChangeListener() {
        @Override
        public void onPageSelected(int position) {
            notifySelectedView(position);
        }
    };

    private int selectedPage = Consts.NOT_SET;

    @Inject
    PlayerPagerPresenter(PlayQueueManager playQueueManager,
                         PlaySessionStateProvider playSessionStateProvider,
                         TrackRepository trackRepository,
                         StationsOperations stationsOperations,
                         TrackPagePresenter trackPagePresenter,
                         AudioAdPresenter audioAdPresenter,
                         VideoAdPresenter videoAdPresenter,
                         CastConnectionHelper castConnectionHelper,
                         AdsOperations adOperations,
                         VideoSurfaceProvider videoSurfaceProvider,
                         EventBus eventBus) {
        this.playQueueManager = playQueueManager;
        this.trackRepository = trackRepository;
        this.trackPagePresenter = trackPagePresenter;
        this.playSessionStateProvider = playSessionStateProvider;
        this.audioAdPresenter = audioAdPresenter;
        this.videoAdPresenter = videoAdPresenter;
        this.castConnectionHelper = castConnectionHelper;
        this.adOperations = adOperations;
        this.videoSurfaceProvider = videoSurfaceProvider;
        this.eventBus = eventBus;
        this.stationsOperations = stationsOperations;
        this.trackPagerAdapter = new TrackPagerAdapter();
        this.trackPageRecycler = new TrackPageRecycler();
    }

    public void setCurrentPlayQueue(List<PlayQueueItem> playQueue, int currentItem) {
        selectedPage = currentItem;
        currentPlayQueue = playQueue;
        trackPagerAdapter.notifyDataSetChanged();
    }

    List<PlayQueueItem> getCurrentPlayQueue() {
        return currentPlayQueue;
    }

    void onPlayerSlide(float slideOffset) {
        for (Map.Entry<View, PlayQueueItem> entry : pagesInPlayer.entrySet()) {
            pagePresenter(entry.getValue()).onPlayerSlide(entry.getKey(), slideOffset);
        }
    }

    PlayQueueItem getItemAtPosition(int position) {
        return currentPlayQueue.get(position);
    }

    boolean isAdPageAtPosition(int position) {
        return AdUtils.isAd(currentPlayQueue.get(position));
    }

    @Override
    public void onViewCreated(PlayerFragment fragment, View view, Bundle savedInstanceState) {
        final PlayerTrackPager trackPager = (PlayerTrackPager) view.findViewById(R.id.player_track_pager);
        trackPager.addOnPageChangeListener(pageChangeListener);
        selectedPage = trackPager.getCurrentItem();

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

        for (Map.Entry<View, PlayQueueItem> entry : pagesInPlayer.entrySet()) {
            final PlayQueueItem pageData = entry.getValue();
            final PlayerPagePresenter presenter = pagePresenter(pageData);
            final View view = entry.getKey();
            presenter.onForeground(view);
            if (pageData.isVideoAd()) {
                setVideoSurface(pageData, presenter, view);
            }
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
                                           .filter(currentPlayQueueItemFilter)
                                           .observeOn(AndroidSchedulers.mainThread())
                                           .subscribe(new PlayerPagerPresenter.PlaybackProgressSubscriber()));
    }

    @Override
    public void onPause(PlayerFragment playerFragment) {
        isForeground = false;

        foregroundSubscription.unsubscribe();
        foregroundSubscription = new CompositeSubscription();

        for (Map.Entry<View, PlayQueueItem> entry : pagesInPlayer.entrySet()) {
            pagePresenter(entry.getValue()).onBackground(entry.getKey());
        }
    }

    @Override
    public void onDestroyView(PlayerFragment playerFragment) {
        for (Map.Entry<View, PlayQueueItem> entry : pagesInPlayer.entrySet()) {
            pagePresenter(entry.getValue()).onDestroyView(entry.getKey());
        }

        if (playerFragment.getActivity().isChangingConfigurations()) {
            videoSurfaceProvider.onConfigurationChange();
        } else {
            videoSurfaceProvider.onDestroy();
        }

        final PlayerTrackPager trackPager = (PlayerTrackPager) playerFragment.getView()
                                                                             .findViewById(R.id.player_track_pager);
        trackPager.removeOnPageChangeListener(pageChangeListener);

        castConnectionHelper.removeOnConnectionChangeListener(this);
        skipListener = null;
        viewVisibilityProvider = ViewVisibilityProvider.EMPTY;

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
            }

            @Override
            public void onPrevious() {
                trackPager.setCurrentItem(trackPager.getCurrentItem() - 1);
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
        final PlayQueueItem playQueueItem = currentPlayQueue.get(position);
        if (playQueueItem.isAd()) {
            return playQueueItem.isVideoAd() ? TYPE_VIDEO_AD_VIEW : TYPE_AUDIO_AD_VIEW;
        } else {
            return TYPE_TRACK_VIEW;
        }
    }

    public boolean isTrackView(Object object) {
        return trackPagePresenter.accept((View) object);
    }

    private int getPagerAdViewPosition() {
        for (int i = 0, size = currentPlayQueue.size(); i < size; i++) {
            if (currentPlayQueue.get(i).isAd() && adOperations.isCurrentItemAd()) {
                return i;
            }
        }
        return PagerAdapter.POSITION_NONE;
    }

    @Override
    public void onCastConnectionChange() {
        for (Map.Entry<View, PlayQueueItem> entry : pagesInPlayer.entrySet()) {
            pagePresenter(entry.getValue()).setCastDeviceName(entry.getKey(), castConnectionHelper.getDeviceName());
        }
    }

    private View bindView(int position, final View view) {
        final PlayQueueItem playQueueItem = currentPlayQueue.get(position);
        pagesInPlayer.put(view, playQueueItem);

        final PlayerPagePresenter presenter = pagePresenter(playQueueItem);

        if (isForeground) {
            // this will attach the cast button
            presenter.onForeground(view);
            if (playQueueItem.isVideoAd()) {
                setVideoSurface(playQueueItem, presenter, view);
            }
        }

        foregroundSubscription.add(getTrackOrAdObservable(playQueueItem)
                                           .observeOn(AndroidSchedulers.mainThread())
                                           .filter(isPlayerItemRelatedToView(view))
                                           .subscribe(new PlayerItemSubscriber(presenter, view)));

        return view;
    }

    private void setVideoSurface(PlayQueueItem playQueueItem, PlayerPagePresenter presenter, View view) {
        final TextureView textureView = ((VideoAdPresenter) presenter).getVideoTexture(view);
        videoSurfaceProvider.setTextureView(playQueueItem.getUrn(), textureView);
    }

    private void configureInitialPageState(final View view) {
        final PlayerPagePresenter presenter = pagePresenter(pagesInPlayer.get(view));
        if (lastPlayerUIEvent != null) {
            configurePageFromUiEvent(lastPlayerUIEvent, presenter, view);
        }

        if (lastPlayStateEvent != null) {
            configurePageFromPlayerState(lastPlayStateEvent, presenter, view);
        }
    }

    private PlayerPagePresenter pagePresenter(PlayQueueItem playQueueItem) {
        if (playQueueItem.isAd()) {
            return playQueueItem.isVideoAd() ? videoAdPresenter : audioAdPresenter;
        } else {
            return trackPagePresenter;
        }
    }

    private Observable<? extends PlayerItem> getTrackOrAdObservable(final PlayQueueItem playQueueItem) {
        if (playQueueItem.isAd()) {
            return getAdObservable(playQueueItem.getAdData().get());
        } else if (playQueueItem.isTrack() && playQueueManager.getCollectionUrn().isStation()) {
            return getStationObservable(playQueueItem);
        } else {
            return getTrackObservable(playQueueItem.getUrn(), playQueueItem.getAdData())
                    .map(toPlayerTrackState(playQueueItem));
        }
    }

    private Observable<? extends PlayerItem> getStationObservable(final PlayQueueItem playQueueItem) {
        return Observable.zip(
                getTrackObservable(playQueueItem.getUrn(), playQueueItem.getAdData()).map(toPlayerTrackState(
                        playQueueItem)),
                stationsOperations.station(playQueueManager.getCollectionUrn()),
                new Func2<PlayerTrackState, StationRecord, PlayerItem>() {
                    @Override
                    public PlayerItem call(PlayerTrackState playerTrackState, StationRecord station) {
                        playerTrackState.setStation(station);
                        return playerTrackState;
                    }
                }
        );
    }

    @NonNull
    private Func1<PropertySet, PlayerTrackState> toPlayerTrackState(final PlayQueueItem playQueueItem) {
        return new Func1<PropertySet, PlayerTrackState>() {
            @Override
            public PlayerTrackState call(PropertySet propertySet) {
                return new PlayerTrackState(propertySet,
                                            playQueueManager.isCurrentItem(playQueueItem),
                                            isForeground, viewVisibilityProvider
                );
            }
        };
    }

    private Observable<PropertySet> getTrackObservable(Urn urn, final Optional<AdData> adOverlayData) {
        return getTrackObservable(urn).doOnNext(new Action1<PropertySet>() {
            @Override
            public void call(PropertySet track) {
                if (adOverlayData.isPresent() && adOverlayData.get() instanceof OverlayAdData) {
                    adOverlayData.get().setMonetizableTitle(track.get(TrackProperty.TITLE));
                    adOverlayData.get().setMonetizableCreator(track.get(TrackProperty.CREATOR_NAME));
                }
            }
        });
    }

    private Observable<? extends PlayerItem> getAdObservable(final AdData adData) {
        return getTrackObservable(adData.getMonetizableTrackUrn()).map(
                    new Func1<PropertySet, AdData>() {
                        @Override
                        public AdData call(PropertySet monetizableTrack) {
                            adData.setMonetizableTitle(monetizableTrack.get(PlayableProperty.TITLE));
                            adData.setMonetizableCreator(monetizableTrack.get(PlayableProperty.CREATOR_NAME));
                            return adData;
                        }
                    }).map(TO_PLAYER_AD);
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
        for (Map.Entry<View, PlayQueueItem> entry : pagesInPlayer.entrySet()) {
            final View trackView = entry.getKey();
            if (isTrackView(trackView)) {
                final Urn urn = entry.getValue().getUrn();
                trackPagePresenter.onPageChange(trackView);
                updateProgress(trackPagePresenter, trackView, urn);
            }
        }
    }

    private void onTrackPageSet(View view, int position) {
        final PlayQueueItem playQueueItem = currentPlayQueue.get(position);
        trackPagePresenter.onPositionSet(view, position, currentPlayQueue.size());
        trackPagePresenter.setCastDeviceName(view, castConnectionHelper.getDeviceName());
        if (hasAdOverlay(playQueueItem)) {
            final OverlayAdData overlayData = (OverlayAdData) playQueueItem.getAdData().get();
            trackPagePresenter.setAdOverlay(view, overlayData);
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
        if (item instanceof PlayerAd) {
            final Urn itemAdUrn = ((PlayerAd) item).getAdUrn();
            return pagesInPlayer.containsKey(pageView)
                    && pagesInPlayer.get(pageView).isAd()
                    && pagesInPlayer.get(pageView).getAdData().get().getAdUrn().equals(itemAdUrn);
        } else {
            return isTrackViewRelatedToUrn(pageView, item.getTrackUrn());
        }
    }

    private boolean isTrackViewRelatedToUrn(View pageView, Urn trackUrn) {
        if (pagesInPlayer.containsKey(pageView) && pagesInPlayer.get(pageView).isTrack()) {
            return pagesInPlayer.get(pageView).getUrn().equals(trackUrn);
        }
        return trackPageRecycler.isPageForUrn(pageView, trackUrn);
    }

    private void updateProgress(PlayerPagePresenter presenter, View trackView, Urn urn) {
        presenter.setProgress(trackView, playSessionStateProvider.getLastProgressForItem(urn));
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

            for (Map.Entry<View, PlayQueueItem> entry : pagesInPlayer.entrySet()) {
                final PlayerPagePresenter presenter = pagePresenter(entry.getValue());
                final View view = entry.getKey();
                configurePageFromUiEvent(event, presenter, view);
            }
        }
    }

    private final class PlaybackStateSubscriber extends DefaultSubscriber<PlayStateEvent> {
        @Override
        public void onNext(PlayStateEvent playStateEvent) {
            lastPlayStateEvent = playStateEvent;

            for (Map.Entry<View, PlayQueueItem> entry : pagesInPlayer.entrySet()) {
                final PlayQueueItem pageData = entry.getValue();
                final PlayerPagePresenter presenter = pagePresenter(pageData);
                final View view = entry.getKey();
                configurePageFromPlayerState(playStateEvent, presenter, view);
            }
        }
    }

    private final class PlaybackProgressSubscriber extends DefaultSubscriber<PlaybackProgressEvent> {
        @Override
        public void onNext(PlaybackProgressEvent progress) {
            for (Map.Entry<View, PlayQueueItem> entry : pagesInPlayer.entrySet()) {
                final PlayerPagePresenter presenter = pagePresenter(entry.getValue());
                final View pageView = entry.getKey();
                final PlayQueueItem pageData = entry.getValue();

                if (isProgressEventForPage(pageData, pageView, progress)) {
                    presenter.setProgress(pageView, progress.getPlaybackProgress());
                }
            }
        }

        private boolean isProgressEventForPage(PlayQueueItem pageData, View pageView, PlaybackProgressEvent progress) {
            return (progress.getUrn().isTrack() && isTrackViewRelatedToUrn(pageView, progress.getUrn()))
                    || progress.getUrn().isAd() && progress.getUrn().equals(pageData.getUrn());
        }
    }

    private class TrackMetadataChangedSubscriber extends DefaultSubscriber<EntityStateChangedEvent> {
        @Override
        public void onNext(EntityStateChangedEvent event) {
            trackObservableCache.remove(event.getFirstUrn());

            for (Map.Entry<View, PlayQueueItem> entry : pagesInPlayer.entrySet()) {
                final PlayerPagePresenter presenter = pagePresenter(entry.getValue());
                final View trackView = entry.getKey();
                if (isTrackViewRelatedToUrn(trackView, event.getFirstUrn())) {
                    presenter.onPlayableUpdated(trackView, event);
                }
            }
        }
    }

    private final class ClearAdOverlaySubscriber extends DefaultSubscriber<CurrentPlayQueueItemEvent> {
        @Override
        public void onNext(CurrentPlayQueueItemEvent ignored) {
            for (Map.Entry<View, PlayQueueItem> entry : pagesInPlayer.entrySet()) {
                final PlayQueueItem playQueueItem = entry.getValue();
                final PlayerPagePresenter presenter = pagePresenter(playQueueItem);
                final View trackView = entry.getKey();

                if (playQueueItem.isTrack()) {
                    if (!playQueueManager.isCurrentItem(playQueueItem)) {
                        presenter.clearAdOverlay(trackView);
                    }
                }
            }
        }
    }

    private void configurePageFromPlayerState(PlayStateEvent playStateEvent,
                                              PlayerPagePresenter presenter,
                                              View view) {
        final boolean viewPresentingCurrentAd = pagesInPlayer.containsKey(view)
                && pagesInPlayer.get(view).isAd()
                && playStateEvent.getPlayingItemUrn().equals(pagesInPlayer.get(view).getUrn());
        final boolean viewPresentingCurrentTrack = pagesInPlayer.containsKey(view)
                && pagesInPlayer.get(view).isTrack()
                && isTrackViewRelatedToUrn(view, playStateEvent.getPlayingItemUrn());

        presenter.setPlayState(view,
                               playStateEvent,
                               (viewPresentingCurrentTrack || viewPresentingCurrentAd),
                               isForeground);
    }

    private void configurePageFromUiEvent(PlayerUIEvent event, PlayerPagePresenter presenter, View view) {
        final int kind = event.getKind();
        if (kind == PlayerUIEvent.PLAYER_EXPANDED) {
            final PlayQueueItem playQueueItem = pagesInPlayer.get(view);
            presenter.setExpanded(view, playQueueItem, isCurrentPagerPage(playQueueItem));
        } else if (kind == PlayerUIEvent.PLAYER_COLLAPSED) {
            presenter.setCollapsed(view);
        }
    }

    private boolean isCurrentPagerPage(PlayQueueItem playQueueItem) {
        return selectedPage != Consts.NOT_SET && playQueueItem.equals(currentPlayQueue.get(selectedPage));
    }

    private class TrackPagerAdapter extends PagerAdapter {

        @Override
        public int getItemPosition(Object object) {
            if (isTrackView(object)) {
                final int index = currentPlayQueue.indexOf(pagesInPlayer.get(object));
                // always re-instantiate first + last items for nav. button visibility concerns
                return isValidMiddleItem(index) ? index : POSITION_NONE;
            } else {
                return getPagerAdViewPosition();
            }
        }

        private boolean isValidMiddleItem(int index) {
            return index > 0 && index < currentPlayQueue.size() - 1;
        }

        @Override
        public int getCount() {
            return currentPlayQueue.size();
        }

        @Override
        public final Object instantiateItem(ViewGroup container, int position) {
            View view;
            switch (getItemViewType(position)) {
                case TYPE_AUDIO_AD_VIEW:
                    audioAdView = view = instantiateAdView(audioAdView, audioAdPresenter, container, position);
                    break;
                case TYPE_VIDEO_AD_VIEW:
                    videoAdView = view = instantiateAdView(videoAdView, videoAdPresenter, container, position);
                    break;
                default:
                    view = instantiateTrackView(position);
                    break;
            }
            configureInitialPageState(view);
            container.addView(view);

            final PlayQueueItem playQueueItem = currentPlayQueue.get(position);
            if (playQueueManager.isCurrentItem(playQueueItem)) {
                pagePresenter(playQueueItem).onViewSelected(view, playQueueItem, isExpanded());
            }
            return view;
        }

        private View instantiateTrackView(int position) {
            final View view;
            final PlayQueueItem trackPageData = currentPlayQueue.get(position);
            final Urn urn = trackPageData.getUrn();

            if (trackPageRecycler.hasExistingPage(urn)) {
                view = trackPageRecycler.removePageByUrn(urn);
                if (!isForeground) {
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

        private View instantiateAdView(View adView, AdPagePresenter presenter, ViewGroup container, int position) {
            if (adView == null) {
                adView = presenter.createItemView(container, skipListener);
            } else {
                presenter.clearItemView(adView);
            }
            return bindView(position, adView);
        }

        @Override
        public void destroyItem(ViewGroup container, int position, Object object) {
            View view = (View) object;
            container.removeView(view);

            if (isTrackView(view)) {
                final PlayQueueItem playQueueItem = pagesInPlayer.get(view);
                final Urn trackUrn = playQueueItem.getUrn();
                trackPageRecycler.recyclePage(trackUrn, view);
                if (!playQueueManager.isCurrentItem(playQueueItem)) {
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

    private boolean isExpanded() {
        return lastPlayerUIEvent != null && lastPlayerUIEvent.getKind() == PlayerUIEvent.PLAYER_EXPANDED;
    }

    private void notifySelectedView(int position) {
        final PlayQueueItem playQueueItem = currentPlayQueue.get(position);
        for (Map.Entry<View, PlayQueueItem> playQueueItemEntry : pagesInPlayer.entrySet()) {
            if (playQueueItem.equals(playQueueItemEntry.getValue())) {
                final View view = playQueueItemEntry.getKey();
                final PlayerPagePresenter presenter = pagePresenter(pagesInPlayer.get(view));
                presenter.onViewSelected(view, playQueueItemEntry.getValue(), isExpanded());
            }
        }
        selectedPage = position;
    }
}
