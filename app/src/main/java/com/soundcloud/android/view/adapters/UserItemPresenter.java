package com.soundcloud.android.view.adapters;

import com.soundcloud.android.Consts;
import com.soundcloud.android.R;
import com.soundcloud.android.image.ApiImageSize;
import com.soundcloud.android.image.ImageOperations;
import com.soundcloud.android.users.UserProperty;
import com.soundcloud.android.utils.ScTextUtils;
import com.soundcloud.propeller.PropertySet;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import javax.inject.Inject;
import java.util.List;

public class UserItemPresenter implements CellPresenter<PropertySet> {

    private final ImageOperations imageOperations;

    @Inject
    public UserItemPresenter(ImageOperations imageOperations) {
        this.imageOperations = imageOperations;
    }

    @Override
    public View createItemView(int position, ViewGroup parent) {
        return LayoutInflater.from(parent.getContext()).inflate(R.layout.user_list_item, parent, false);
    }

    @Override
    public void bindItemView(int position, View itemView, List<PropertySet> items) {
        PropertySet user = items.get(position);
        ((TextView) itemView.findViewById(R.id.list_item_header)).setText(user.get(UserProperty.USERNAME));

        final String country = user.getOrElse(UserProperty.COUNTRY, ScTextUtils.EMPTY_STRING);
        ((TextView) itemView.findViewById(R.id.list_item_subheader)).setText(country);

        setupFollowersCount(itemView, user);
        loadImage(itemView, user);
    }

    private void setupFollowersCount(View itemView, PropertySet user) {
        final TextView followersCountText = (TextView) itemView.findViewById(R.id.list_item_counter);
        final int followersCount = user.get(UserProperty.FOLLOWERS_COUNT);
        if (followersCount > Consts.NOT_SET) {
            followersCountText.setVisibility(View.VISIBLE);
            followersCountText.setText(ScTextUtils.formatNumberWithCommas(followersCount));
        } else {
            followersCountText.setVisibility(View.GONE);
        }
    }

    private void loadImage(View itemView, PropertySet user) {
        imageOperations.displayInAdapterView(
                user.get(UserProperty.URN), ApiImageSize.getListItemImageSize(itemView.getContext()),
                (ImageView) itemView.findViewById(R.id.image));
    }
}
