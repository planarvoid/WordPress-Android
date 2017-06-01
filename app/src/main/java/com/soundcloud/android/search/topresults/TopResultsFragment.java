package com.soundcloud.android.search.topresults;

import com.soundcloud.android.R;
import com.soundcloud.android.SoundCloudApplication;
import com.soundcloud.android.analytics.performance.MetricKey;
import com.soundcloud.android.analytics.performance.MetricParams;
import com.soundcloud.android.analytics.performance.MetricType;
import com.soundcloud.android.analytics.performance.PerformanceMetric;
import com.soundcloud.android.analytics.performance.PerformanceMetricsEngine;
import com.soundcloud.android.main.RootActivity;
import com.soundcloud.android.main.Screen;
import com.soundcloud.android.model.CollectionLoadingState;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.rx.RxJava;
import com.soundcloud.android.search.SearchEmptyStateProvider;
import com.soundcloud.android.search.topresults.UiAction.Refresh;
import com.soundcloud.android.utils.LeakCanaryWrapper;
import com.soundcloud.android.view.collection.CollectionRenderer;
import com.soundcloud.android.view.collection.CollectionRendererState;
import com.soundcloud.java.collections.Lists;
import com.soundcloud.java.optional.Optional;
import io.reactivex.Observable;
import io.reactivex.annotations.NonNull;
import io.reactivex.subjects.BehaviorSubject;
import io.reactivex.subjects.PublishSubject;

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
    @Inject PerformanceMetricsEngine performanceMetricsEngine;
    @Inject LeakCanaryWrapper leakCanaryWrapper;

    private CollectionRenderer<TopResultsBucketViewModel, RecyclerView.ViewHolder> collectionRenderer;

    private final BehaviorSubject<UiAction.Search> searchIntent = BehaviorSubject.create();

    private final PublishSubject<SearchItem.Track> trackClick = PublishSubject.create();
    private final PublishSubject<SearchItem.Playlist> playlistClick = PublishSubject.create();
    private final PublishSubject<SearchItem.User> userClick = PublishSubject.create();
    private final PublishSubject<TopResults.Bucket.Kind> viewAllClick = PublishSubject.create();
    private final PublishSubject<UiAction.HelpClick> helpClick = PublishSubject.create();


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
        searchIntent.onNext(UiAction.Search.create(SearchParams.create(apiQuery(), userQuery(), searchQueryUrn(), searchQueryPosition())));
        collectionRenderer = new CollectionRenderer<>(adapterFactory.create(trackClick, playlistClick, userClick, viewAllClick, helpClick),
                                                      this::isTheSameItem,
                                                      Object::equals,
                                                      new SearchEmptyStateProvider(),
                                                      true,
                                                      false);
        setHasOptionsMenu(true);
    }

    @Override
    public void accept(@NonNull ViewModel viewModel) throws Exception {
        final CollectionRendererState<TopResultsBucketViewModel> newState = toLegacyModel(viewModel);
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

    private CollectionRendererState<TopResultsBucketViewModel> toLegacyModel(ViewModel viewModel) {
        final CollectionLoadingState.Builder builder = CollectionLoadingState.builder();
        builder.isRefreshing(viewModel.isRefreshing());
        builder.isLoadingNextPage(viewModel.isLoading());
        builder.refreshError(viewModel.error());
        return CollectionRendererState.create(builder.build(), viewModel.data().isPresent() ? viewModel.data().get().buckets() : Lists.newArrayList());
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
    public Observable<UiAction.Search> searchIntent() {
        return searchIntent;
    }

    @Override
    public Observable<Refresh> refreshIntent() {
        return RxJava.toV2Observable(collectionRenderer.onRefresh()
                                                       .map(ignore -> Refresh.create(SearchParams.createRefreshing(apiQuery(), userQuery(), searchQueryUrn(), searchQueryPosition()))));
    }

    @Override
    public Observable<UiAction.Enter> enterScreen() {
        return RxJava.toV2Observable(((RootActivity) getActivity()).enterScreen().map(ignore -> UiAction.Enter.create(apiQuery())));
    }

    @Override
    public Observable<UiAction.TrackClick> trackClick() {
        return trackClick.map(track -> UiAction.TrackClick.create(apiQuery(), track));
    }

    @Override
    public Observable<UiAction.PlaylistClick> playlistClick() {
        return playlistClick.map(playlist -> UiAction.PlaylistClick.create(apiQuery(), playlist));
    }

    @Override
    public Observable<UiAction.UserClick> userClick() {
        return userClick.map(user -> UiAction.UserClick.create(apiQuery(), user));
    }

    @Override
    public Observable<UiAction.ViewAllClick> viewAllClick() {
        return viewAllClick.map(kind -> UiAction.ViewAllClick.create(apiQuery(), kind));
    }

    @Override
    public Observable<UiAction.HelpClick> helpClick() {
        return helpClick;
    }

    @Override
    public void handleActionResult(ClickResultAction action) {
        action.run(getActivity());
    }

    private String apiQuery() {
        return getArguments().getString(KEY_API_QUERY);
    }

    private String userQuery() {
        return getArguments().getString(KEY_USER_QUERY);
    }

    private Optional<Urn> searchQueryUrn() {
        return Optional.fromNullable(getArguments().<Urn>getParcelable(KEY_QUERY_URN));
    }

    private Optional<Integer> searchQueryPosition() {
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
        presenter.attachView(this);
    }

    @Override
    public void onDestroyView() {
        collectionRenderer.detach();
        super.onDestroyView();
        leakCanaryWrapper.watch(this);
    }

}
