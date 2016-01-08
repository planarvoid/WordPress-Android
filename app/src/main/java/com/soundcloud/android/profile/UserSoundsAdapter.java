package com.soundcloud.android.profile;

import com.soundcloud.android.presentation.RecyclerItemAdapter;

import android.support.v7.widget.RecyclerView;
import android.view.View;

import javax.inject.Inject;

public class UserSoundsAdapter extends RecyclerItemAdapter<UserSoundsBucket, UserSoundsAdapter.ViewHolder> {
    static int VIEW_TYPE = 0;

    @Inject
    UserSoundsAdapter(UserSoundsBucketRenderer spotlightRenderer) {
        super(spotlightRenderer);
    }

    @Override
    public void setOnItemClickListener(View.OnClickListener itemClickListener) {
        super.setOnItemClickListener(itemClickListener);
    }

    @Override
    public int getBasicItemViewType(int position) {
        return VIEW_TYPE;
    }

    @Override
    protected UserSoundsAdapter.ViewHolder createViewHolder(View itemView) {
        return new UserSoundsAdapter.ViewHolder(itemView);
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        public ViewHolder(View itemView) {
            super(itemView);
        }
    }
}
