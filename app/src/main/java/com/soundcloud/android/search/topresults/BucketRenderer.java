package com.soundcloud.android.search.topresults;

import static com.soundcloud.android.search.topresults.TopResults.Bucket.Kind.GO_TRACKS;

import com.google.auto.factory.AutoFactory;
import com.google.auto.factory.Provided;
import com.soundcloud.android.R;
import com.soundcloud.android.configuration.FeatureOperations;
import com.soundcloud.android.presentation.CellRenderer;
import com.soundcloud.android.presentation.DividerItemDecoration;
import com.soundcloud.android.search.topresults.TopResults.Bucket;
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
    private final PublishSubject<Void> helpClicked;
    private final SearchItemAdapterFactory searchItemAdapterFactory;
    private final CondensedNumberFormatter numberFormatter;
    private final FeatureOperations featureOperations;

    @Inject
    BucketRenderer(PublishSubject<SearchItem> searchItemClicked,
                   PublishSubject<TopResultsViewAllArgs> viewAllClicked,
                   PublishSubject<Void> helpClicked,
                   @Provided SearchItemAdapterFactory searchItemAdapterFactory,
                   @Provided CondensedNumberFormatter numberFormatter,
                   @Provided FeatureOperations featureOperations) {
        this.searchItemClicked = searchItemClicked;
        this.viewAllClicked = viewAllClicked;
        this.helpClicked = helpClicked;
        this.searchItemAdapterFactory = searchItemAdapterFactory;
        this.numberFormatter = numberFormatter;
        this.featureOperations = featureOperations;
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
        bindViewAll(itemView, resources, bucketText, viewModel.shouldShowViewAll(), viewModel.totalResults(), viewModel.kind());
        final boolean lastItem = items.size() - 1 == position;
        itemView.findViewById(R.id.bucket_bottom_padding).setVisibility(lastItem ? View.VISIBLE : View.GONE);
        bindHighTierHelpItem(itemView, viewModel.kind());
    }

    private void bindViewAll(View itemView, Resources resources, String bucketText, boolean shouldShowViewAll, int totalResults, Bucket.Kind kind) {
        final View viewAllButton = itemView.findViewById(R.id.bucket_view_all);
        viewAllButton.setVisibility(shouldShowViewAll ? View.VISIBLE : View.GONE);
        if (shouldShowViewAll) {
            String resultsCountString = numberFormatter.format(totalResults);
            final String viewAllText = resources.getString(R.string.top_results_view_all, resultsCountString, bucketText);
            ((TextView) itemView.findViewById(R.id.bucket_view_all_text)).setText(viewAllText);
            viewAllButton.setOnClickListener(view -> viewAllClicked.onNext(TopResultsViewAllArgs.create(kind)));
        }
    }

    private void bindResultList(View itemView, Resources resources, List<SearchItem> searchItems) {
        ((SearchItemAdapter) itemView.getTag()).setItems(searchItems);
        addListDividers(((RecyclerView) itemView.findViewById(R.id.bucket_items)), resources);
    }

    private void bindTitle(View itemView, String bucketText) {
        ((TextView) itemView.findViewById(R.id.bucket_header)).setText(bucketText);
    }


    private void bindHighTierHelpItem(View itemView, Bucket.Kind kind) {
        final View helpItemView = itemView.findViewById(R.id.help);
        if (featureOperations.upsellHighTier() && kind == GO_TRACKS) {
            helpItemView.setVisibility(View.VISIBLE);
            helpItemView.setOnClickListener(ignored -> helpClicked.onNext(null));
        } else {
            helpItemView.setVisibility(View.GONE);
        }
    }

    private void addListDividers(RecyclerView recyclerView, Resources resources) {
        Drawable divider = ContextCompat.getDrawable(recyclerView.getContext(), com.soundcloud.androidkit.R.drawable.ak_list_divider_item);
        int dividerHeight = resources.getDimensionPixelSize(com.soundcloud.androidkit.R.dimen.ak_list_divider_horizontal_height);
        recyclerView.addItemDecoration(new DividerItemDecoration(divider, dividerHeight));
    }
}
