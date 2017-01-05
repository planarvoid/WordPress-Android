package com.soundcloud.android.playlists;

import static com.soundcloud.android.R.id.reason;

import butterknife.ButterKnife;
import com.soundcloud.android.R;
import com.soundcloud.android.presentation.CellRenderer;

import android.content.Context;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import javax.inject.Inject;
import java.util.List;

public class PlaylistDetailOtherPlaylistsItemRenderer implements CellRenderer<PlaylistDetailOtherPlaylistsItem> {

    private final OtherPlaylistsByUserAdapterFactory adapterFactory;

    @Inject
    PlaylistDetailOtherPlaylistsItemRenderer(OtherPlaylistsByUserAdapterFactory adapterFactory) {
        this.adapterFactory = adapterFactory;
    }

    @Override
    public View createItemView(ViewGroup viewGroup) {
        final View view = LayoutInflater.from(viewGroup.getContext())
                                        .inflate(R.layout.other_playlists_by_user_bucket, viewGroup, false);
        initCarousel(view, ButterKnife.<RecyclerView>findById(view, R.id.other_playlists));
        return view;
    }

    private void initCarousel(View bucketView, final RecyclerView recyclerView) {
        final Context context = recyclerView.getContext();
        final OtherPlaylistsByUserAdapter adapter = adapterFactory.create();

        recyclerView.setHasFixedSize(true);
        recyclerView.setLayoutManager(new LinearLayoutManager(context, LinearLayoutManager.HORIZONTAL, false));
        recyclerView.setAdapter(adapter);

        bucketView.setTag(adapter);
    }

    @Override
    public void bindItemView(int position, View bucketView, List<PlaylistDetailOtherPlaylistsItem> list) {
        final PlaylistDetailOtherPlaylistsItem otherPlaylistsItem = list.get(position);

        final TextView reasonView = ButterKnife.findById(bucketView, reason);
        reasonView.setText(bucketView.getResources().getString(R.string.more_playlists_by, otherPlaylistsItem.getCreatorName()));

        final OtherPlaylistsByUserAdapter adapter = (OtherPlaylistsByUserAdapter) bucketView.getTag();
        adapter.setOtherPlaylistsByUser(otherPlaylistsItem);

    }

}
