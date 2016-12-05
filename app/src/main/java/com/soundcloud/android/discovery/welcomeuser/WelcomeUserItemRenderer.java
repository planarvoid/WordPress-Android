package com.soundcloud.android.discovery.welcomeuser;

import butterknife.ButterKnife;
import com.soundcloud.android.R;
import com.soundcloud.android.image.ApiImageSize;
import com.soundcloud.android.image.ImageOperations;
import com.soundcloud.android.presentation.CellRenderer;

import android.content.res.Resources;
import android.support.v4.content.ContextCompat;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import javax.inject.Inject;
import java.util.List;

public class WelcomeUserItemRenderer implements CellRenderer<WelcomeUserItem> {

    private final ImageOperations imageOperations;
    private final Resources resources;

    @Inject
    public WelcomeUserItemRenderer(ImageOperations imageOperations,
                                   Resources resources) {
        this.imageOperations = imageOperations;
        this.resources = resources;
    }

    @Override
    public View createItemView(ViewGroup parent) {
        return LayoutInflater.from(parent.getContext())
                             .inflate(R.layout.welcome_user_item, parent, false);
    }

    @Override
    public void bindItemView(int position, View itemView, List<WelcomeUserItem> items) {
        WelcomeUserItem welcomeUserItem = items.get(position);
        WelcomeResourceBundle resourceBundle = WelcomeResourceBundle.forTimeOfDay(welcomeUserItem.timeOfDay());

        setAvatar(itemView, welcomeUserItem);
        setBackground(itemView, resourceBundle);
        setWelcomeMessage(itemView, welcomeUserItem, resourceBundle);
        setDescription(itemView, resourceBundle);
    }

    private void setBackground(View itemView, WelcomeResourceBundle resources) {
        itemView.setBackgroundColor(ContextCompat.getColor(itemView.getContext(), resources.backgroundColorId()));
        ButterKnife.<ImageView>findById(itemView, R.id.background_image).setImageResource(resources.backgroundResId());
        ButterKnife.<ImageView>findById(itemView, R.id.background_detail).setImageResource(resources.detailSpriteResId());
    }

    private void setAvatar(View itemView, WelcomeUserItem welcomeUserItem) {
        ImageView avatar = ButterKnife.findById(itemView, R.id.welcome_user_avatar);
        imageOperations.displayCircularInAdapterView(welcomeUserItem, ApiImageSize.getFullImageSize(resources), avatar);
    }

    private void setWelcomeMessage(View itemView,
                                   WelcomeUserItem welcomeUserItem,
                                   WelcomeResourceBundle resourceBundle) {
        String title = itemView.getContext().getString(resourceBundle.titleStringId(), welcomeUserItem.userName());

        TextView titleView = ButterKnife.findById(itemView, R.id.welcome_user_title);
        titleView.setText(title);
        titleView.setTextColor(ContextCompat.getColor(itemView.getContext(), resourceBundle.titleTextColorId()));
    }

    private void setDescription(View itemView, WelcomeResourceBundle resourceBundle) {
        TextView descriptionView = ButterKnife.findById(itemView, R.id.welcome_user_description);
        descriptionView.setTextColor(ContextCompat.getColor(itemView.getContext(), resourceBundle.descriptionTextColorId()));
    }
}
