package com.soundcloud.android.view.adapters;

import com.soundcloud.android.presentation.PagingRecyclerViewAdapter;
import com.soundcloud.android.presentation.ViewTypes;
import com.soundcloud.android.users.UserItem;

import android.support.v7.widget.RecyclerView;
import android.view.View;

import javax.inject.Inject;

public class UserRecyclerViewAdapter extends PagingRecyclerViewAdapter<UserItem, UserRecyclerViewAdapter.UserViewHolder> {

    @Inject
    public UserRecyclerViewAdapter(UserItemRenderer userItemRenderer) {
        super(userItemRenderer);
    }

    @Override
    public int getBasicItemViewType(int position) {
        return ViewTypes.DEFAULT_VIEW_TYPE;
    }

    @Override
    protected UserViewHolder createViewHolder(View itemView) {
        return new UserViewHolder(itemView);
    }

    public static class UserViewHolder extends RecyclerView.ViewHolder {
        public UserViewHolder(View itemView) {
            super(itemView);
        }
    }

}
