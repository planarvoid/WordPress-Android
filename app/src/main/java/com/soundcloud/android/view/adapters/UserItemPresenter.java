package com.soundcloud.android.view.adapters;

import com.soundcloud.android.Consts;
import com.soundcloud.android.R;
import com.soundcloud.android.image.ApiImageSize;
import com.soundcloud.android.image.ImageOperations;
import com.soundcloud.android.model.UserProperty;
import com.soundcloud.android.utils.ScTextUtils;
import com.soundcloud.propeller.PropertySet;
import org.jetbrains.annotations.Nullable;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.ToggleButton;

import javax.inject.Inject;
import java.util.List;

public class UserItemPresenter implements CellPresenter<PropertySet> {

    private final LayoutInflater layoutInflater;
    private final ImageOperations imageOperations;
    @Nullable private OnToggleFollowListener toggleFollowListener;

    @Inject
    public UserItemPresenter(LayoutInflater layoutInflater, ImageOperations imageOperations) {
        this.layoutInflater = layoutInflater;
        this.imageOperations = imageOperations;
    }

    @Override
    public View createItemView(int position, ViewGroup parent) {
        return layoutInflater.inflate(R.layout.user_list_item, parent, false);
    }

    @Override
    public void bindItemView(int position, View itemView, List<PropertySet> items) {
        PropertySet user = items.get(position);
        ((TextView) itemView.findViewById(R.id.list_item_header)).setText(user.get(UserProperty.USERNAME));

        final String country = user.getOrElse(UserProperty.COUNTRY, ScTextUtils.EMPTY_STRING);
        ((TextView) itemView.findViewById(R.id.list_item_subheader)).setText(country);

        setupFollowersCount(itemView, user);
        showFollowingStatus(position, itemView, user);
        loadImage(itemView, user);
    }

    public void setToggleFollowListener(@Nullable OnToggleFollowListener toggleFollowListener) {
        this.toggleFollowListener = toggleFollowListener;
    }

    private void showFollowingStatus(final int position, View itemView, PropertySet user) {
        final boolean isFollowing = user.contains(UserProperty.IS_FOLLOWED_BY_ME) && user.get(UserProperty.IS_FOLLOWED_BY_ME);
        final ToggleButton toggleButton = (ToggleButton) itemView.findViewById(R.id.toggle_btn_follow);
        toggleButton.setChecked(isFollowing);
        toggleButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (toggleFollowListener != null) {
                    toggleFollowListener.onToggleFollowClicked(position, (ToggleButton) v);
                }
            }
        });
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

    public interface OnToggleFollowListener {
        void onToggleFollowClicked(int position, ToggleButton toggleButton);
    }
}
