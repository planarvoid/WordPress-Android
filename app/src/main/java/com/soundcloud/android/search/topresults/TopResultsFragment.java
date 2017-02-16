package com.soundcloud.android.search.topresults;

import com.soundcloud.android.R;
import com.soundcloud.android.SoundCloudApplication;
import com.soundcloud.android.main.Screen;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.view.CollectionRenderer;
import com.soundcloud.android.view.DefaultEmptyStateProvider;
import com.soundcloud.android.view.adapters.MixedItemClickListener;
import com.soundcloud.java.collections.Pair;
import com.soundcloud.java.optional.Optional;
import rx.Observable;
import rx.android.schedulers.AndroidSchedulers;
import rx.subjects.BehaviorSubject;
import rx.subscriptions.CompositeSubscription;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
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
    @Inject MixedItemClickListener.Factory clickListenerFactory;

    private CollectionRenderer<TopResultsBucketViewModel, RecyclerView.ViewHolder> collectionRenderer;
    private CompositeSubscription subscription;

    private final BehaviorSubject<Pair<String, Optional<Urn>>> searchIntent = BehaviorSubject.create();

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
        searchIntent.onNext(Pair.of(getApiQuery(), getSearchQueryUrn()));
        collectionRenderer = new CollectionRenderer<>(adapterFactory.create(presenter.searchItemClicked()), this::isTheSameItem, Object::equals, new DefaultEmptyStateProvider(), true);
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
    public Observable<Pair<String, Optional<Urn>>> searchIntent() {
        return searchIntent;
    }

    @Override
    public Observable<Void> refreshIntent() {
        return collectionRenderer.onRefresh();
    }

    private String getApiQuery() {
        return getArguments().getString(KEY_API_QUERY);
    }

    // TODO : Not sure what this is for (tracking??), but we should probably use it
    private String getUserQuery() {
        return getArguments().getString(KEY_USER_QUERY);
    }

    private Optional<Urn> getSearchQueryUrn() {
        return Optional.fromNullable(getArguments().<Urn>getParcelable(KEY_QUERY_URN));
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.recyclerview_with_refresh_without_empty, container, false);
    }

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        collectionRenderer.attach(view, false);

        subscription = new CompositeSubscription();

        subscription.addAll(presenter
                                    .viewModel()
                                    .map(TopResultsViewModel::buckets)
                                    .observeOn(AndroidSchedulers.mainThread())
                                    .subscribe(collectionRenderer::render),
                            presenter.playlistItemClicked().subscribe(click -> clickListenerFactory.create(Screen.SEARCH_TOP_RESULTS, click.searchQuerySourceInfo()).onPlaylistItemClick(click.playlistItem(), getActivity())),
                            presenter.trackItemClicked().subscribe(click -> clickListenerFactory.create(Screen.SEARCH_TOP_RESULTS, click.searchQuerySourceInfo()).onTrackItemClick(click.playQueue(), click.position())),
                            presenter.userItemClicked().subscribe(click -> clickListenerFactory.create(Screen.SEARCH_TOP_RESULTS, click.searchQuerySourceInfo()).onUserItemClick(click.userItem(), getActivity())));
    }

    @Override
    public void onDestroyView() {
        subscription.unsubscribe();
        collectionRenderer.detach();
        super.onDestroyView();
    }
}
