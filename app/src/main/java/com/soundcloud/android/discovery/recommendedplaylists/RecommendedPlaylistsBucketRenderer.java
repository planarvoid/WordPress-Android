package com.soundcloud.android.discovery.recommendedplaylists;

import static com.soundcloud.android.R.id.reason;
import static com.soundcloud.android.utils.ScTextUtils.toResourceKey;

import butterknife.ButterKnife;
import com.soundcloud.android.R;
import com.soundcloud.android.presentation.CellRenderer;

import android.content.Context;
import android.content.res.Resources;
import android.os.Parcelable;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import javax.inject.Inject;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class RecommendedPlaylistsBucketRenderer implements CellRenderer<RecommendedPlaylistsBucketItem> {

    private final Map<String, Parcelable> scrollingState = new HashMap<>();
    private final RecommendedPlaylistsAdapterFactory recommendedPlaylistsAdapterFactory;
    private final Resources resources;
    private RecommendedPlaylistsAdapter.QueryPositionProvider queryPositionProvider;

    @Inject
    RecommendedPlaylistsBucketRenderer(RecommendedPlaylistsAdapterFactory recommendedPlaylistsAdapterFactory, Resources resources) {
        this.recommendedPlaylistsAdapterFactory = recommendedPlaylistsAdapterFactory;
        this.resources = resources;
    }

    @Override
    public View createItemView(ViewGroup viewGroup) {
        final View view = LayoutInflater.from(viewGroup.getContext())
                                        .inflate(R.layout.recommendation_bucket, viewGroup, false);
        initCarousel(view, ButterKnife.<RecyclerView>findById(view, R.id.recommendations_carousel));
        return view;
    }

    public void setQueryPositionProvider(RecommendedPlaylistsAdapter.QueryPositionProvider queryPositionProvider) {
        this.queryPositionProvider = queryPositionProvider;
    }

    private void initCarousel(View bucketView, final RecyclerView recyclerView) {
        final Context context = recyclerView.getContext();
        final RecommendedPlaylistsAdapter adapter = recommendedPlaylistsAdapterFactory.create(queryPositionProvider);

        recyclerView.setHasFixedSize(true);
        recyclerView.setLayoutManager(new LinearLayoutManager(context, LinearLayoutManager.HORIZONTAL, false));
        recyclerView.setAdapter(adapter);

        bucketView.setTag(adapter);
    }

    @Override
    public void bindItemView(int position, View bucketView, List<RecommendedPlaylistsBucketItem> list) {
        final RecommendedPlaylistsBucketItem recommendedTracksItem = list.get(position);

        bindReasonView(bucketView, recommendedTracksItem);
        bindCarousel(bucketView, recommendedTracksItem);
    }

    private void bindReasonView(View bucketView, final RecommendedPlaylistsBucketItem bucket) {
        final TextView reasonView = ButterKnife.findById(bucketView, reason);
        reasonView.setText(getReasonText(bucketView.getContext(), bucket));
    }

    private void bindCarousel(View bucketView, RecommendedPlaylistsBucketItem recommendedTracksItem) {
        final RecommendedPlaylistsAdapter adapter = (RecommendedPlaylistsAdapter) bucketView.getTag();
        final RecyclerView recyclerView = ButterKnife.findById(bucketView, R.id.recommendations_carousel);
        if (adapter.hasBucketItem()) {
            //Save previous scrolling state
            scrollingState.put(adapter.bucketId(), recyclerView.getLayoutManager().onSaveInstanceState());
        }
        //Set new content
        adapter.setRecommendedTracksBucketItem(recommendedTracksItem);
        if (scrollingState.containsKey(adapter.bucketId())) {
            //Apply previous scrolling state
            recyclerView.getLayoutManager().onRestoreInstanceState(scrollingState.get(adapter.bucketId()));
        } else {
            recyclerView.scrollToPosition(0);
        }
    }

    private String getReasonText(Context context, RecommendedPlaylistsBucketItem recommendedPlaylists) {
        final String playlistName = nameForKey(recommendedPlaylists.key(), recommendedPlaylists.displayName(), context);
        return context.getString(R.string.recommended_playlists_reason, playlistName.toLowerCase(Locale.US));
    }

    private String nameForKey(String key, String fallbackName, Context context) {
        final String headingKey = toResourceKey("scenario_playlist_", key);
        final int headingResourceId = resources.getIdentifier(headingKey, "string", context.getPackageName());
        return (headingResourceId != 0) ? resources.getString(headingResourceId) : fallbackName;
    }
}
