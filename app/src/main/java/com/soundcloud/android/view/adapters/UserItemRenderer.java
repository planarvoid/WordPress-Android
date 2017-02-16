package com.soundcloud.android.view.adapters;

import com.soundcloud.android.Consts;
import com.soundcloud.android.R;
import com.soundcloud.android.image.ApiImageSize;
import com.soundcloud.android.image.ImageOperations;
import com.soundcloud.android.presentation.CellRenderer;
import com.soundcloud.android.users.UserItem;
import com.soundcloud.android.util.CondensedNumberFormatter;
import com.soundcloud.java.optional.Optional;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import javax.inject.Inject;
import java.util.List;

public class UserItemRenderer implements CellRenderer<UserItem> {

    private final ImageOperations imageOperations;
    private final CondensedNumberFormatter numberFormatter;

    @Inject
    public UserItemRenderer(ImageOperations imageOperations, CondensedNumberFormatter numberFormatter) {
        this.imageOperations = imageOperations;
        this.numberFormatter = numberFormatter;
    }

    @Override
    public View createItemView(ViewGroup parent) {
        return LayoutInflater.from(parent.getContext()).inflate(R.layout.user_list_item, parent, false);
    }

    @Override
    public void bindItemView(int position, View itemView, List<UserItem> items) {
        bindItemView(itemView, items.get(position));
    }

    protected void bindItemView(View itemView, UserItem user) {
        ((TextView) itemView.findViewById(R.id.list_item_header)).setText(user.name());
        setOptionalCountry(itemView, user);
        setupFollowersCount(itemView, user);
        loadImage(itemView, user);
    }

    private void setOptionalCountry(View itemView, UserItem user) {
        final Optional<String> country = user.country();
        final TextView countryText = (TextView) itemView.findViewById(R.id.list_item_subheader);
        if (country.isPresent()) {
            countryText.setText(country.get());
            countryText.setVisibility(View.VISIBLE);
        } else {
            countryText.setVisibility(View.GONE);
        }
    }

    private void setupFollowersCount(View itemView, UserItem user) {
        final TextView followersCountText = (TextView) itemView.findViewById(R.id.list_item_counter);
        final int followersCount = user.followersCount();
        if (followersCount > Consts.NOT_SET) {
            followersCountText.setVisibility(View.VISIBLE);
            followersCountText.setText(numberFormatter.format(followersCount));
        } else {
            followersCountText.setVisibility(View.GONE);
        }
    }

    private void loadImage(View itemView, UserItem user) {
        imageOperations.displayCircularInAdapterView(
                user, ApiImageSize.getListItemImageSize(itemView.getResources()),
                (ImageView) itemView.findViewById(R.id.image));
    }
}
