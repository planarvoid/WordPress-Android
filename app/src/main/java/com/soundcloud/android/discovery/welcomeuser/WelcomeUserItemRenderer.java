package com.soundcloud.android.discovery.welcomeuser;

import butterknife.ButterKnife;
import com.soundcloud.android.R;
import com.soundcloud.android.image.ApiImageSize;
import com.soundcloud.android.image.ImageOperations;
import com.soundcloud.android.presentation.CellRenderer;
import com.soundcloud.android.view.CustomFontTextView;

import android.content.res.Resources;
import android.support.v4.content.ContextCompat;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

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

        setBackground(itemView, welcomeUserItem);
        setAvatar(itemView, welcomeUserItem);
        setWelcomeMessage(itemView, welcomeUserItem);
    }

    private void setBackground(View itemView, WelcomeUserItem welcomeUserItem) {
        int background = welcomeUserItem.isNight() ? R.drawable.night_sprite : R.drawable.morning_sprite;
        int detail = welcomeUserItem.isNight() ? R.drawable.dark_moon : R.drawable.morning_sun;
        int backgroundColor = welcomeUserItem.isNight()
                              ? ContextCompat.getColor(itemView.getContext(), R.color.welcome_night)
                              : ContextCompat.getColor(itemView.getContext(), R.color.welcome_day);

        itemView.setBackgroundColor(backgroundColor);
        ButterKnife.<ImageView>findById(itemView, R.id.background_image).setImageResource(background);
        ButterKnife.<ImageView>findById(itemView, R.id.background_detail).setImageResource(detail);
    }

    private void setAvatar(View itemView, WelcomeUserItem welcomeUserItem) {
        ImageView avatar = ButterKnife.findById(itemView, R.id.welcome_user_avatar);
        imageOperations.displayCircularInAdapterView(welcomeUserItem, ApiImageSize.getFullImageSize(resources), avatar);
    }

    private void setWelcomeMessage(View itemView, WelcomeUserItem welcomeUserItem) {
        String title = itemView.getContext()
                                .getString(R.string.welcome_user_title, welcomeUserItem.userName());
        ButterKnife.<CustomFontTextView>findById(itemView, R.id.welcome_user_title).setText(title);
    }
}
