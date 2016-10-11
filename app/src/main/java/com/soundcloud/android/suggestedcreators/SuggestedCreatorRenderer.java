package com.soundcloud.android.suggestedcreators;

import butterknife.ButterKnife;
import com.soundcloud.android.Navigator;
import com.soundcloud.android.R;
import com.soundcloud.android.associations.FollowingOperations;
import com.soundcloud.android.events.UIEvent;
import com.soundcloud.android.image.ApiImageSize;
import com.soundcloud.android.image.ImageOperations;
import com.soundcloud.android.image.SimpleImageResource;
import com.soundcloud.android.presentation.CellRenderer;
import com.soundcloud.android.rx.observers.DefaultSubscriber;
import com.soundcloud.android.users.User;
import com.soundcloud.java.collections.PropertySet;

import android.content.res.Resources;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CompoundButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.ToggleButton;

import javax.inject.Inject;
import java.util.List;

public class SuggestedCreatorRenderer implements CellRenderer<SuggestedCreatorItem> {

    private final ImageOperations imageOperations;
    private final Resources resources;
    private final FollowingOperations followingOperations;
    private final Navigator navigator;

    @Inject
    SuggestedCreatorRenderer(ImageOperations imageOperations,
                             Resources resources,
                             FollowingOperations followingOperations, Navigator navigator) {
        this.imageOperations = imageOperations;
        this.resources = resources;
        this.followingOperations = followingOperations;
        this.navigator = navigator;
    }

    @Override
    public View createItemView(ViewGroup parent) {
        return LayoutInflater.from(parent.getContext()).inflate(R.layout.suggested_creators_item, parent, false);
    }

    @Override
    public void bindItemView(int position, View itemView, List<SuggestedCreatorItem> items) {
        final SuggestedCreatorItem suggestedCreatorItem = items.get(position);

        bindArtistName(itemView, suggestedCreatorItem.user());
        bindArtistLocation(itemView, suggestedCreatorItem.user());
        bindVisualBanner(itemView, suggestedCreatorItem.user());
        bindAvatar(itemView, suggestedCreatorItem.user());
        bindReason(itemView, suggestedCreatorItem.relation());
        bindFollowButton(itemView, suggestedCreatorItem);
    }

    private void bindFollowButton(View view, final SuggestedCreatorItem suggestedCreatorItem) {
        final ToggleButton toggleButton = ButterKnife.findById(view, R.id.toggle_btn_follow);
        toggleButton.setChecked(suggestedCreatorItem.following);
        toggleButton.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                suggestedCreatorItem.following = isChecked;
                followingOperations.toggleFollowing(suggestedCreatorItem.user().urn(), isChecked)
                                   .subscribe(new DefaultSubscriber<PropertySet>());
            }
        });
    }

    private void bindArtistName(View view, final User creator) {
        final TextView textView = ButterKnife.findById(view, R.id.suggested_creator_artist);
        textView.setText(creator.username());
        textView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //TODO Fix event for tracking
                navigator.openProfile(v.getContext(), creator.urn(), new UIEvent(UIEvent.KIND_NAVIGATION));
            }
        });
    }

    private void bindArtistLocation(View view, User creator) {
        final TextView textView = ButterKnife.findById(view, R.id.suggested_creator_location);
        if (!creator.city().isPresent() && !creator.country().isPresent()) {
            textView.setVisibility(View.GONE);
        } else if (creator.city().isPresent() && creator.country().isPresent()) {
            textView.setText(String.format("%s, %s", creator.city().get(), creator.country().get()));
        } else {
            textView.setText(creator.city().or(creator.country()).get());
        }
    }

    private void bindReason(View view, SuggestedCreatorRelation relation) {
        final String key = "suggested_creators_relation_" + relation.value();
        final int resourceId = resources.getIdentifier(key, "string", view.getContext().getPackageName());
        final String text = (resourceId != 0) ? resources.getString(resourceId) : "";
        ButterKnife.<TextView>findById(view, R.id.suggested_creator_relation).setText(text);
    }

    private void bindVisualBanner(View view, final User creator) {
        final ImageView imageView = (ImageView) view.findViewById(R.id.suggested_creator_visual_banner);
        imageOperations.displayInAdapterView(SimpleImageResource.create(creator.urn(), creator.visualUrl()),
                                             ApiImageSize.getFullBannerSize(resources),
                                             imageView);

        // unfortunately this doesn't work when you do it in the XML
        ButterKnife.findById(view, R.id. suggested_creators_item).setBackgroundResource(R.drawable.card_border);
    }

    private void bindAvatar(View view, User creator) {
        final ImageView imageView = (ImageView) view.findViewById(R.id.suggested_creator_avatar);
        imageOperations.displayCircularInAdapterView(
                SimpleImageResource.create(creator.urn(), creator.avatarUrl()),
                ApiImageSize.getFullImageSize(resources),
                imageView);
    }
}
