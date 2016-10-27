package com.soundcloud.android.view.adapters;

import com.soundcloud.android.presentation.PagingRecyclerItemAdapter;
import com.soundcloud.android.presentation.ViewTypes;
import com.soundcloud.android.users.UserItem;

import android.support.v7.widget.RecyclerView;
import android.view.View;

import javax.inject.Inject;

public class UserRecyclerItemAdapter
        extends PagingRecyclerItemAdapter<UserItem, RecyclerView.ViewHolder> {

    @Inject
    public UserRecyclerItemAdapter(UserItemRenderer userItemRenderer) {
        super(userItemRenderer);
    }

    @Override
    public int getBasicItemViewType(int position) {
        return ViewTypes.DEFAULT_VIEW_TYPE;
    }

    @Override
    protected ViewHolder createViewHolder(View itemView) {
        return new ViewHolder(itemView);
    }
}
