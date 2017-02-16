package com.soundcloud.android.search.topresults;

import com.google.auto.factory.AutoFactory;
import com.google.auto.factory.Provided;
import com.soundcloud.android.R;
import com.soundcloud.android.presentation.CellRenderer;
import com.soundcloud.android.presentation.DividerItemDecoration;
import rx.subjects.PublishSubject;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.drawable.Drawable;
import android.support.v4.content.ContextCompat;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import javax.inject.Inject;
import java.util.List;

@AutoFactory
class BucketRenderer implements CellRenderer<TopResultsBucketViewModel> {

    private final PublishSubject<SearchItem> searchItemClicked;
    private final SearchItemAdapterFactory searchItemAdapterFactory;

    @Inject
    BucketRenderer(PublishSubject<SearchItem> searchItemClicked, @Provided SearchItemAdapterFactory searchItemAdapterFactory) {
        this.searchItemClicked = searchItemClicked;
        this.searchItemAdapterFactory = searchItemAdapterFactory;
    }

    @Override
    public View createItemView(ViewGroup parent) {
        final View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.bucket_list_item, parent, false);

        initCarousel(view, ((RecyclerView) view.findViewById(R.id.bucket_items)));
        return view;
    }

    private void initCarousel(View bucketView, final RecyclerView recyclerView) {
        final Context context = recyclerView.getContext();
        final SearchItemAdapter adapter = searchItemAdapterFactory.create(searchItemClicked);

        recyclerView.setHasFixedSize(true);
        recyclerView.setLayoutManager(new LinearLayoutManager(context, LinearLayoutManager.VERTICAL, false));
        recyclerView.setAdapter(adapter);

        bucketView.setTag(adapter);
    }

    @Override
    public void bindItemView(int position, View itemView, List<TopResultsBucketViewModel> items) {
        final TopResultsBucketViewModel viewModel = items.get(position);
        final Resources resources = itemView.getResources();
        final String bucketText = resources.getString(viewModel.titleResourceId());
        bindTitle(itemView, bucketText);
        bindResultList(itemView, resources, viewModel.items());
        bindViewAll(itemView, resources, bucketText, viewModel.shouldShowViewAll(), viewModel.totalResults());
    }

    private void bindViewAll(View itemView, Resources resources, String bucketText, boolean shouldShowViewAll, int totalResults) {
        itemView.findViewById(R.id.bucket_view_all).setVisibility(shouldShowViewAll ? View.VISIBLE : View.GONE);
        if (shouldShowViewAll) {
            final String viewAllText = resources.getString(R.string.top_results_view_all, totalResults, bucketText);
            ((TextView) itemView.findViewById(R.id.bucket_view_all_text)).setText(viewAllText);
        }
    }

    private void bindResultList(View itemView, Resources resources, List<SearchItem> searchItems) {
        ((SearchItemAdapter) itemView.getTag()).setItems(searchItems);
        addListDividers(((RecyclerView) itemView.findViewById(R.id.bucket_items)), resources);
    }

    private void bindTitle(View itemView, String bucketText) {
        ((TextView) itemView.findViewById(R.id.bucket_header)).setText(bucketText);
    }

    private void addListDividers(RecyclerView recyclerView, Resources resources) {
        Drawable divider = ContextCompat.getDrawable(recyclerView.getContext(), com.soundcloud.androidkit.R.drawable.ak_list_divider_item);
        int dividerHeight = resources.getDimensionPixelSize(com.soundcloud.androidkit.R.dimen.ak_list_divider_horizontal_height);
        recyclerView.addItemDecoration(new DividerItemDecoration(divider, dividerHeight));
    }
}
