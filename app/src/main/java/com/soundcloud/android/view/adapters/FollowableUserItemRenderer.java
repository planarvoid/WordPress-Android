package com.soundcloud.android.view.adapters;

import static com.soundcloud.android.rx.observers.DefaultSubscriber.fireAndForget;

import com.soundcloud.android.Consts;
import com.soundcloud.android.R;
import com.soundcloud.android.associations.NextFollowingOperations;
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
import android.widget.ToggleButton;

import javax.inject.Inject;
import java.util.List;

public class FollowableUserItemRenderer implements CellRenderer<UserItem> {

    private final ImageOperations imageOperations;
    private final NextFollowingOperations followingOperations;

    @Inject
    public FollowableUserItemRenderer(ImageOperations imageOperations, NextFollowingOperations followingOperations) {
        this.imageOperations = imageOperations;
        this.followingOperations = followingOperations;
    }

    @Override
    public View createItemView(ViewGroup parent) {
        return LayoutInflater.from(parent.getContext()).inflate(R.layout.followable_user_list_item, parent, false);
    }

    @Override
    public void bindItemView(int position, View itemView, List<UserItem> items) {
        UserItem user = items.get(position);
        ((TextView) itemView.findViewById(R.id.list_item_header)).setText(user.getName());
        ((TextView) itemView.findViewById(R.id.list_item_subheader)).setText(user.getCountry());

        setupFollowersCount(itemView, user);
        loadImage(itemView, user);
        setupFollowToggle(itemView, user);
    }

    private void setupFollowToggle(View itemView, final UserItem user) {
        final ToggleButton toggleFollow = ((ToggleButton) itemView.findViewById(R.id.toggle_btn_follow));

        toggleFollow.setChecked(user.isFollowedByMe());
        toggleFollow.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                fireAndForget(followingOperations.toggleFollowing(user.getEntityUrn(), toggleFollow.isChecked()));
            }
        });
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
