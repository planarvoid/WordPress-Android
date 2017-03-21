package com.soundcloud.android.search.topresults;

import com.google.auto.factory.AutoFactory;
import com.google.auto.factory.Provided;
import com.soundcloud.android.R;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.presentation.CellRenderer;
import com.soundcloud.android.presentation.DividerItemDecoration;
import com.soundcloud.android.util.CondensedNumberFormatter;
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
    private final PublishSubject<TopResultsViewAllArgs> viewAllClicked;
    private final SearchItemAdapterFactory searchItemAdapterFactory;
    private final CondensedNumberFormatter numberFormatter;

    @Inject
    BucketRenderer(PublishSubject<SearchItem> searchItemClicked,
                   PublishSubject<TopResultsViewAllArgs> viewAllClicked,
                   @Provided SearchItemAdapterFactory searchItemAdapterFactory,
                   @Provided CondensedNumberFormatter numberFormatter) {
        this.searchItemClicked = searchItemClicked;
        this.viewAllClicked = viewAllClicked;
        this.searchItemAdapterFactory = searchItemAdapterFactory;
        this.numberFormatter = numberFormatter;
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
        bindViewAll(itemView, resources, bucketText, viewModel.shouldShowViewAll(), viewModel.totalResults(), viewModel.kind(), viewModel.queryUrn());
        final boolean lastItem = items.size() - 1 == position;
        itemView.findViewById(R.id.bucket_bottom_padding).setVisibility(lastItem ? View.VISIBLE : View.GONE);
    }

    private void bindViewAll(View itemView, Resources resources, String bucketText, boolean shouldShowViewAll, int totalResults, TopResultsBucketViewModel.Kind kind, Urn queryUrn) {
        final View viewAllButton = itemView.findViewById(R.id.bucket_view_all);
        viewAllButton.setVisibility(shouldShowViewAll ? View.VISIBLE : View.GONE);
        if (shouldShowViewAll) {
            String resultsCountString = numberFormatter.format(totalResults);
            final String viewAllText = resources.getString(R.string.top_results_view_all, resultsCountString, bucketText);
            ((TextView) itemView.findViewById(R.id.bucket_view_all_text)).setText(viewAllText);
            viewAllButton.setOnClickListener(view -> viewAllClicked.onNext(TopResultsViewAllArgs.create(kind, queryUrn)));
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
