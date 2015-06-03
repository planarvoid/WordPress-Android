package com.soundcloud.android.view.adapters;

import com.soundcloud.android.Consts;
import com.soundcloud.android.R;
import com.soundcloud.android.image.ApiImageSize;
import com.soundcloud.android.image.ImageOperations;
import com.soundcloud.android.presentation.CellRenderer;
import com.soundcloud.android.users.UserItem;
import com.soundcloud.android.utils.ScTextUtils;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import javax.inject.Inject;
import java.util.List;

public class UserItemRenderer implements CellRenderer<UserItem> {

    private final ImageOperations imageOperations;

    @Inject
    public UserItemRenderer(ImageOperations imageOperations) {
        this.imageOperations = imageOperations;
    }

    @Override
    public View createItemView(ViewGroup parent) {
        return LayoutInflater.from(parent.getContext()).inflate(R.layout.user_list_item, parent, false);
    }

    @Override
    public void bindItemView(int position, View itemView, List<UserItem> items) {
        UserItem user = items.get(position);
        ((TextView) itemView.findViewById(R.id.list_item_header)).setText(user.getName());
        ((TextView) itemView.findViewById(R.id.list_item_subheader)).setText(user.getCountry());

        setupFollowersCount(itemView, user);
        loadImage(itemView, user);
    }

    private void setupFollowersCount(View itemView, UserItem user) {
        final TextView followersCountText = (TextView) itemView.findViewById(R.id.list_item_counter);
        final int followersCount = user.getFollowersCount();
        if (followersCount > Consts.NOT_SET) {
            followersCountText.setVisibility(View.VISIBLE);
            followersCountText.setText(ScTextUtils.formatNumberWithCommas(followersCount));
        } else {
            followersCountText.setVisibility(View.GONE);
        }
    }

    private void loadImage(View itemView, UserItem user) {
        imageOperations.displayInAdapterView(
                user.getEntityUrn(), ApiImageSize.getListItemImageSize(itemView.getContext()),
                (ImageView) itemView.findViewById(R.id.image));
    }
}
