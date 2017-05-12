package com.soundcloud.android.search.topresults;

import com.soundcloud.android.Navigator;
import com.soundcloud.android.R;
import com.soundcloud.android.SoundCloudApplication;
import com.soundcloud.android.analytics.performance.MetricKey;
import com.soundcloud.android.analytics.performance.MetricParams;
import com.soundcloud.android.analytics.performance.MetricType;
import com.soundcloud.android.analytics.performance.PerformanceMetric;
import com.soundcloud.android.analytics.performance.PerformanceMetricsEngine;
import com.soundcloud.android.events.UIEvent;
import com.soundcloud.android.main.RootActivity;
import com.soundcloud.android.main.Screen;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.payments.UpsellContext;
import com.soundcloud.android.playback.PlaybackResult;
import com.soundcloud.android.playback.ui.view.PlaybackFeedbackHelper;
import com.soundcloud.android.search.SearchEmptyStateProvider;
import com.soundcloud.android.search.SearchTracker;
import com.soundcloud.android.utils.LeakCanaryWrapper;
import com.soundcloud.android.view.collection.CollectionRenderer;
import com.soundcloud.android.view.collection.CollectionRendererState;
import com.soundcloud.java.optional.Optional;
import rx.Observable;
import rx.subjects.BehaviorSubject;
import rx.subjects.PublishSubject;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import javax.inject.Inject;

public class TopResultsFragment extends Fragment implements TopResultsPresenter.TopResultsView {

    private static final String KEY_API_QUERY = "query";
    private static final String KEY_USER_QUERY = "userQuery";

    private static final String KEY_QUERY_URN = "queryUrn";
    private static final String KEY_QUERY_POSITION = "queryPosition";

    @Inject TopResultsPresenter presenter;
    @Inject TopResultsAdapterFactory adapterFactory;
    @Inject Navigator navigator;
    @Inject PlaybackFeedbackHelper playbackFeedbackHelper;
    @Inject SearchTracker searchTracker;
    @Inject PerformanceMetricsEngine performanceMetricsEngine;
    @Inject LeakCanaryWrapper leakCanaryWrapper;

    private CollectionRenderer<TopResultsBucketViewModel, RecyclerView.ViewHolder> collectionRenderer;

    private final BehaviorSubject<SearchParams> searchIntent = BehaviorSubject.create();

    private final PublishSubject<SearchItem.Track> trackClick = PublishSubject.create();
    private final PublishSubject<SearchItem.Playlist> playlistClick = PublishSubject.create();
    private final PublishSubject<SearchItem.User> userClick = PublishSubject.create();
    private final PublishSubject<TopResults.Bucket.Kind> viewAllClick = PublishSubject.create();
    private final PublishSubject<Void> helpClick = PublishSubject.create();


    public static TopResultsFragment newInstance(String apiQuery,
                                                 String userQuery,
                                                 Optional<Urn> queryUrn,
                                                 Optional<Integer> queryPosition) {
        Bundle bundle = new Bundle();
        bundle.putString(KEY_API_QUERY, apiQuery);
        bundle.putString(KEY_USER_QUERY, userQuery);
        if (queryUrn.isPresent()) {
            bundle.putParcelable(KEY_QUERY_URN, queryUrn.get());
        }

        if (queryPosition.isPresent()) {
            bundle.putInt(KEY_QUERY_POSITION, queryPosition.get());
        }

        TopResultsFragment fragment = new TopResultsFragment();
        fragment.setArguments(bundle);
        return fragment;
    }

    public TopResultsFragment() {
        SoundCloudApplication.getObjectGraph().inject(this);
        setRetainInstance(true);
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        searchIntent.onNext(createSearchParams());
        collectionRenderer = new CollectionRenderer<>(adapterFactory.create(trackClick, playlistClick, userClick, viewAllClick, helpClick),
                                                      this::isTheSameItem,
                                                      Object::equals,
                                                      new SearchEmptyStateProvider(),
                                                      true,
                                                      false);
        presenter.attachView(this);
        setHasOptionsMenu(true);
    }

    private boolean isTheSameItem(TopResultsBucketViewModel item1, TopResultsBucketViewModel item2) {
        return item1.kind() == item2.kind();
    }

    @Override
    public void onDestroy() {
        presenter.detachView();
        super.onDestroy();
    }

    @Override
    public Observable<SearchParams> searchIntent() {
        return searchIntent;
    }

    @Override
    public Observable<SearchParams> refreshIntent() {
        return collectionRenderer.onRefresh().map(ignore -> createSearchParams());
    }

    @Override
    public Observable<Void> enterScreen() {
        return ((RootActivity) getActivity()).enterScreen();
    }

    @Override
    public Observable<SearchItem.Track> trackClick() {
        return trackClick;
    }

    @Override
    public Observable<SearchItem.Playlist> playlistClick() {
        return playlistClick;
    }

    @Override
    public Observable<SearchItem.User> userClick() {
        return userClick;
    }

    @Override
    public Observable<TopResults.Bucket.Kind> viewAllClick() {
        return viewAllClick;
    }

    @Override
    public Observable<Void> helpClick() {
        return helpClick;
    }

    @Override
    public String searchQuery() {
        return getApiQuery();
    }

    private SearchParams createSearchParams() {
        return SearchParams.create(getApiQuery(), getUserQuery(), getSearchQueryUrn(), getSearchQueryPosition());
    }

    private String getApiQuery() {
        return getArguments().getString(KEY_API_QUERY);
    }

    private String getUserQuery() {
        return getArguments().getString(KEY_USER_QUERY);
    }

    private Optional<Urn> getSearchQueryUrn() {
        return Optional.fromNullable(getArguments().<Urn>getParcelable(KEY_QUERY_URN));
    }

    private Optional<Integer> getSearchQueryPosition() {
        return Optional.fromNullable(getArguments().getInt(KEY_QUERY_POSITION));
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.recyclerview_with_refresh_without_empty, container, false);
    }

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        collectionRenderer.attach(view, false, new LinearLayoutManager(view.getContext()));
    }

    @Override
    public void navigateToPlaylist(GoToItemArgs args) {
        navigator.openPlaylist(getContext(),
                               args.itemUrn(),
                               Screen.SEARCH_EVERYTHING,
                               args.searchQuerySourceInfo(),
                               null, // top results cannot be promoted *yet
                               UIEvent.fromNavigation(args.itemUrn(), args.eventContextMetadata()));
    }

    @Override
    public void navigateToUser(GoToItemArgs args) {
        navigator.openProfile(getContext(),
                              args.itemUrn(),
                              Screen.SEARCH_EVERYTHING,
                              UIEvent.fromNavigation(args.itemUrn(), args.eventContextMetadata()));
    }

    @Override
    public void navigateToViewAll(TopResultsViewAllArgs args) {
        navigator.openSearchViewAll(getContext(),
                                    getApiQuery(),
                                    args.queryUrn(),
                                    args.kind(),
                                    args.isPremium());
    }

    @Override
    public void navigateToHelp() {
        navigator.openUpgrade(getContext(), UpsellContext.PREMIUM_CONTENT);
    }

    @Override
    public void renderNewState(CollectionRendererState<TopResultsBucketViewModel> newState) {
        collectionRenderer.render(newState);
        if (!newState.collectionLoadingState().isLoadingNextPage()) {
            MetricParams params = new MetricParams().putString(MetricKey.SCREEN, Screen.SEARCH_EVERYTHING.toString());
            PerformanceMetric metric = PerformanceMetric.builder()
                                                        .metricType(MetricType.PERFORM_SEARCH)
                                                        .metricParams(params)
                                                        .build();
            performanceMetricsEngine.endMeasuring(metric);
        }
    }

    @Override
    public void showError(PlaybackResult.ErrorReason errorReason) {
        playbackFeedbackHelper.showFeedbackOnPlaybackError(errorReason);
    }

    @Override
    public void onDestroyView() {
        collectionRenderer.detach();
        super.onDestroyView();
        leakCanaryWrapper.watch(this);
    }

}
