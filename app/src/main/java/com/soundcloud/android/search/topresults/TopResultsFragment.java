package com.soundcloud.android.search.topresults;

import com.soundcloud.android.R;
import com.soundcloud.android.SoundCloudApplication;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.presentation.CellRenderer;
import com.soundcloud.android.presentation.RecyclerItemAdapter;
import com.soundcloud.android.view.CollectionRenderer;
import com.soundcloud.java.collections.Pair;
import com.soundcloud.java.optional.Optional;
import rx.Observable;
import rx.Subscription;
import rx.android.schedulers.AndroidSchedulers;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import javax.inject.Inject;
import java.util.List;

public class TopResultsFragment extends Fragment implements TopResultsPresenter.TopResultsView {

    private static final String KEY_API_QUERY = "query";
    private static final String KEY_USER_QUERY = "userQuery";

    private static final String KEY_QUERY_URN = "queryUrn";
    private static final String KEY_QUERY_POSITION = "queryPosition";

    @Inject TopResultsPresenter presenter;

    private CollectionRenderer<TopResultsBucketViewModel, RecyclerView.ViewHolder> collectionRenderer;
    private Subscription subscription;

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
        collectionRenderer = new CollectionRenderer<>(new TopResultsAdapter(), this::isTheSameItem, Object::equals);
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
        return Observable.just(Pair.of(getApiQuery(), getSearchQueryUrn()));
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
        return inflater.inflate(R.layout.default_recyclerview_with_refresh, container, false);
    }

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        collectionRenderer.attach(view);

        subscription = presenter
                .viewModel()
                .map(TopResultsViewModel::buckets)
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(collectionRenderer::render);
    }

    @Override
    public void onDestroyView() {
        subscription.unsubscribe();
        collectionRenderer.detach();
        super.onDestroyView();
    }

    static class TopResultsAdapter extends RecyclerItemAdapter<TopResultsBucketViewModel, RecyclerView.ViewHolder> {

        TopResultsAdapter() {
            super(new BucketRenderer());
        }

        @Override
        protected RecyclerView.ViewHolder createViewHolder(View itemView) {
            return new RecyclerItemAdapter.ViewHolder(itemView);
        }

        @Override
        public int getBasicItemViewType(int position) {
            return 0;
        }
    }

    static class BucketRenderer implements CellRenderer<TopResultsBucketViewModel> {

        @Override
        public View createItemView(ViewGroup parent) {
            return LayoutInflater.from(parent.getContext()).inflate(android.R.layout.simple_list_item_1, parent, false);
        }

        @Override
        public void bindItemView(int position, View itemView, List<TopResultsBucketViewModel> items) {
            ((TextView) itemView.findViewById(android.R.id.text1)).setText(String.valueOf(items.get(position)));
        }
    }
}
