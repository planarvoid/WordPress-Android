package com.soundcloud.android.suggestedcreators;

import static com.soundcloud.android.rx.observers.DefaultSubscriber.fireAndForget;

import com.soundcloud.android.Navigator;
import com.soundcloud.android.R;
import com.soundcloud.android.analytics.EngagementsTracking;
import com.soundcloud.android.analytics.ScreenProvider;
import com.soundcloud.android.events.EventContextMetadata;
import com.soundcloud.android.events.Module;
import com.soundcloud.android.events.UIEvent;
import com.soundcloud.android.presentation.CellRenderer;
import com.soundcloud.android.profile.ProfileImageHelper;
import com.soundcloud.android.users.User;

import android.content.res.Resources;
import android.os.Handler;
import android.support.annotation.NonNull;
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

    private final ProfileImageHelper profileImageHelper;
    private final Resources resources;
    private final SuggestedCreatorsOperations suggestedCreatorsOperations;
    private final Navigator navigator;
    private final EngagementsTracking engagementsTracking;
    private final ScreenProvider screenProvider;
    private final long followDelay = 150L;
    private final Handler followHandler = new Handler();

    @Inject
    SuggestedCreatorRenderer(ProfileImageHelper profileImageHelper, Resources resources,
                             SuggestedCreatorsOperations suggestedCreatorsOperations,
                             Navigator navigator,
                             EngagementsTracking engagementsTracking,
                             ScreenProvider screenProvider) {
        this.profileImageHelper = profileImageHelper;
        this.resources = resources;
        this.suggestedCreatorsOperations = suggestedCreatorsOperations;
        this.navigator = navigator;
        this.engagementsTracking = engagementsTracking;
        this.screenProvider = screenProvider;
    }

    @Override
    public View createItemView(ViewGroup parent) {
        View itemView = LayoutInflater.from(parent.getContext())
                                      .inflate(R.layout.suggested_creators_item, parent, false);
        // unfortunately this doesn't work when you do it in the XML
        itemView.findViewById(R.id.suggested_creators_item)
                .setBackgroundResource(R.drawable.card_border);
        return itemView;
    }

    @Override
    public void bindItemView(int position, View itemView, List<SuggestedCreatorItem> items) {
        final SuggestedCreatorItem suggestedCreatorItem = items.get(position);
        bindArtistName(itemView, suggestedCreatorItem.creator(), position);
        bindArtistLocation(itemView, suggestedCreatorItem.creator());
        bindImages(itemView, suggestedCreatorItem);
        bindReason(itemView, suggestedCreatorItem.relation());
        bindFollowButton(itemView, suggestedCreatorItem, position);
    }

    private void bindImages(View itemView, final SuggestedCreatorItem suggestedCreatorItem) {
        final ImageView bannerView = (ImageView) itemView.findViewById(R.id.suggested_creator_visual_banner);
        final ImageView avatarView = (ImageView) itemView.findViewById(R.id.suggested_creator_avatar);

        profileImageHelper.bindImages(suggestedCreatorItem, bannerView, avatarView);
    }

    private void bindFollowButton(View view, final SuggestedCreatorItem suggestedCreatorItem, final int position) {
        final ToggleButton toggleButton = (ToggleButton) view.findViewById(R.id.toggle_btn_follow);
        toggleButton.setOnCheckedChangeListener(null);
        toggleButton.setChecked(suggestedCreatorItem.following);
        toggleButton.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, final boolean isChecked) {
                suggestedCreatorItem.following = isChecked;

                followHandler.removeCallbacksAndMessages(null);

                followHandler.postDelayed(buildOnFollowCheckedRunnable(isChecked, suggestedCreatorItem, position),
                                          followDelay);
            }
        });
    }

    @NonNull
    private Runnable buildOnFollowCheckedRunnable(final boolean isChecked,
                                                  final SuggestedCreatorItem suggestedCreatorItem,
                                                  final int position) {
        return new Runnable() {
            @Override
            public void run() {
                fireAndForget(suggestedCreatorsOperations.toggleFollow(suggestedCreatorItem.creator().urn(),
                                                                       isChecked));

                engagementsTracking.followUserUrn(suggestedCreatorItem.creator().urn(),
                                                  isChecked,
                                                  buildEventContextMetadata(position));
            }
        };
    }

    private void bindArtistName(View view, final User creator, final int position) {
        final TextView textView = (TextView) view.findViewById(R.id.suggested_creator_artist);
        textView.setText(creator.username());
        textView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                navigator.openProfile(v.getContext(),
                                      creator.urn(),
                                      UIEvent.fromNavigation(creator.urn(), buildEventContextMetadata(position)));
            }
        });
    }

    private EventContextMetadata buildEventContextMetadata(int position) {
        return EventContextMetadata.builder()
                                   .pageName(screenProvider.getLastScreen().get())
                                   .module(Module.create(Module.SUGGESTED_CREATORS, position))
                                   .build();
    }

    private void bindArtistLocation(View view, User creator) {
        final TextView textView = (TextView) view.findViewById(R.id.suggested_creator_location);
        if (!creator.city().isPresent() && !creator.country().isPresent()) {
            textView.setVisibility(View.GONE);
        } else if (creator.city().isPresent() && creator.country().isPresent()) {
            textView.setText(String.format("%s, %s",
                                           creator.city().get(),
                                           creator.country().get()));
        } else {
            textView.setText(creator.city().or(creator.country()).get());
        }
    }

    private void bindReason(View view, SuggestedCreatorRelation relation) {
        final String key = "suggested_creators_relation_" + relation.value();
        final int resourceId = resources.getIdentifier(key,
                                                       "string",
                                                       view.getContext().getPackageName());
        final String text = (resourceId != 0) ? resources.getString(resourceId) : "";
        ((TextView) view.findViewById(R.id.suggested_creator_relation)).setText(text);
    }

    void unsubscribe() {
        profileImageHelper.unsubscribe();
    }

}
