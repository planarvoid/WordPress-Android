package com.soundcloud.android.search.topresults;

import com.soundcloud.android.R;
import com.soundcloud.android.SoundCloudApplication;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.presentation.CellRenderer;
import com.soundcloud.android.presentation.RecyclerItemAdapter;
import com.soundcloud.android.utils.Log;
import com.soundcloud.android.view.AsyncViewModel;
import com.soundcloud.android.view.CollectionViewFragment;
import com.soundcloud.java.optional.Optional;
import rx.Observable;
import rx.android.schedulers.AndroidSchedulers;
import rx.functions.Func1;

import android.os.Bundle;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import javax.inject.Inject;
import java.util.List;

public class TopResultsFragment extends CollectionViewFragment<TopResultsViewModel, TopResultsBucketViewModel> {

    private static final String KEY_API_QUERY = "query";
    private static final String KEY_USER_QUERY = "userQuery";

    private static final String KEY_QUERY_URN = "queryUrn";
    private static final String KEY_QUERY_POSITION = "queryPosition";

    @Inject TopResultsPresenter presenter;

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
        presenter.search(getApiQuery(), getSearchQueryUrn());
        presenter.connect();
        setHasOptionsMenu(true);
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
    protected void onRefresh() {
        presenter.refresh();
    }

    @Override
    protected RecyclerItemAdapter<TopResultsBucketViewModel, RecyclerView.ViewHolder> createAdapter() {
        return new TopResultsAdapter();
    }


    @Override
    protected Observable<AsyncViewModel<TopResultsViewModel>> modelUpdates() {
        return presenter
                .viewModel()
                .observeOn(AndroidSchedulers.mainThread())
                .doOnNext(viewModel -> Log.i("asdf", String.valueOf(viewModel)))
                .doOnError(error -> Log.e("asdf", "error emitted " , error));
    }

    @Override
    protected Func1<TopResultsViewModel, Iterable<TopResultsBucketViewModel>> viewModelToItems() {
        return TopResultsViewModel::buckets;
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
