package com.soundcloud.android.search;

import com.soundcloud.android.Consts;
import com.soundcloud.android.analytics.Screen;
import com.soundcloud.android.collections.ItemAdapter;
import com.soundcloud.android.collections.ListRow;
import com.soundcloud.android.collections.ScBaseAdapter;
import com.soundcloud.android.collections.views.PlayableRow;
import com.soundcloud.android.collections.views.UserlistRow;
import com.soundcloud.android.image.ImageOperations;
import com.soundcloud.android.model.ScResource;
import com.soundcloud.android.model.SearchResultsCollection;
import com.soundcloud.android.model.User;
import com.soundcloud.android.playback.PlaybackOperations;
import com.soundcloud.android.profile.ProfileActivity;
import rx.Observer;

import android.content.Context;
import android.content.Intent;
import android.view.View;
import android.view.ViewGroup;

import javax.inject.Inject;

public class SearchResultsAdapter extends ItemAdapter<ScResource> implements Observer<SearchResultsCollection> {

    private static final int TYPE_PLAYABLE = 0;
    private static final int TYPE_USER = 1;

    private final ImageOperations mImageOperations;

    private final PlaybackOperations mPlaybackOperations;

    @Inject
    public SearchResultsAdapter(ImageOperations imageOperations, PlaybackOperations playbackOperations) {
        super(Consts.COLLECTION_PAGE_SIZE);
        mImageOperations = imageOperations;
        mPlaybackOperations = playbackOperations;
    }

    @Override
    public void onCompleted() {}

    @Override
    public void onError(Throwable e) {}

    @Override
    public void onNext(SearchResultsCollection results) {
        for (ScResource item : results) {
            addItem(item);
        }
        notifyDataSetChanged();
    }

    @Override
    protected View createItemView(int position, ViewGroup parent) {
        int type = getItemViewType(position);
        switch (type) {
            case TYPE_PLAYABLE:
                return new PlayableRow(parent.getContext(), mImageOperations);
            case TYPE_USER:
                return new UserlistRow(parent.getContext(), Screen.SEARCH_EVERYTHING, mImageOperations);
            default:
                throw new IllegalArgumentException("no view for playlists yet");
        }
    }

    @Override
    protected void bindItemView(int position, View itemView) {
        ((ListRow) itemView).display(position, getItem(position));
    }

    @Override
    public int getItemViewType(int position) {
        return getItem(position) instanceof User ? TYPE_USER : TYPE_PLAYABLE;
    }

    @Override
    public int getViewTypeCount() {
        return 2;
    }

    public void handleClick(Context context, int position) {
        int type = getItemViewType(position);
        if (type == TYPE_PLAYABLE) {
            mPlaybackOperations.playFromAdapter(context, mItems, position, null, Screen.SEARCH_EVERYTHING);
        } else if (type == TYPE_USER) {
            context.startActivity(new Intent(context, ProfileActivity.class).putExtra(ProfileActivity.EXTRA_USER, getItem(position)));
        }
    }

}
