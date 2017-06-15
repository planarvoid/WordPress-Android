package com.soundcloud.android.suggestedcreators;

import static com.soundcloud.android.utils.ViewUtils.getFragmentActivity;

import com.soundcloud.android.R;
import com.soundcloud.android.analytics.EngagementsTracking;
import com.soundcloud.android.analytics.ScreenProvider;
import com.soundcloud.android.configuration.experiments.ChangeLikeToSaveExperimentStringHelper;
import com.soundcloud.android.configuration.experiments.ChangeLikeToSaveExperimentStringHelper.ExperimentString;
import com.soundcloud.android.events.EventContextMetadata;
import com.soundcloud.android.events.Module;
import com.soundcloud.android.events.UIEvent;
import com.soundcloud.android.navigation.NavigationExecutor;
import com.soundcloud.android.navigation.NavigationTarget;
import com.soundcloud.android.navigation.Navigator;
import com.soundcloud.android.presentation.CellRenderer;
import com.soundcloud.android.profile.ProfileImageHelper;
import com.soundcloud.android.rx.observers.DefaultDisposableCompletableObserver;
import com.soundcloud.android.users.User;
import com.soundcloud.java.optional.Optional;

import android.content.res.Resources;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.ToggleButton;

import javax.inject.Inject;
import java.util.List;

public class SuggestedCreatorRenderer implements CellRenderer<SuggestedCreatorItem> {

    private final ProfileImageHelper profileImageHelper;
    private final Resources resources;
    private final SuggestedCreatorsOperations suggestedCreatorsOperations;
    private final NavigationExecutor navigationExecutor;
    private final EngagementsTracking engagementsTracking;
    private final ScreenProvider screenProvider;
    private final ChangeLikeToSaveExperimentStringHelper changeLikeToSaveExperimentStringHelper;
    private final Navigator navigator;

    @Inject
    SuggestedCreatorRenderer(ProfileImageHelper profileImageHelper,
                             Resources resources,
                             SuggestedCreatorsOperations suggestedCreatorsOperations,
                             NavigationExecutor navigationExecutor,
                             EngagementsTracking engagementsTracking,
                             ScreenProvider screenProvider,
                             ChangeLikeToSaveExperimentStringHelper changeLikeToSaveExperimentStringHelper,
                             Navigator navigator) {
        this.profileImageHelper = profileImageHelper;
        this.resources = resources;
        this.suggestedCreatorsOperations = suggestedCreatorsOperations;
        this.navigationExecutor = navigationExecutor;
        this.engagementsTracking = engagementsTracking;
        this.screenProvider = screenProvider;
        this.changeLikeToSaveExperimentStringHelper = changeLikeToSaveExperimentStringHelper;
        this.navigator = navigator;
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
        toggleButton.setEnabled(true);
        toggleButton.setOnCheckedChangeListener((buttonView, isChecked) -> {
            toggleButton.setEnabled(false);
            suggestedCreatorItem.following = isChecked;
            suggestedCreatorsOperations.toggleFollow(suggestedCreatorItem.creator().urn(), isChecked)
                                       .subscribe(new DefaultDisposableCompletableObserver());

            engagementsTracking.followUserUrn(suggestedCreatorItem.creator().urn(),
                                              isChecked,
                                              buildEventContextMetadata(position));
        });
    }

    private void bindArtistName(View view, final User creator, final int position) {
        final TextView textView = (TextView) view.findViewById(R.id.suggested_creator_artist);
        textView.setText(creator.username());
        textView.setOnClickListener(v -> navigator.navigateTo(NavigationTarget.forProfile(getFragmentActivity(v),
                                                                                          creator.urn(),
                                                                                          Optional.of(UIEvent.fromNavigation(creator.urn(), buildEventContextMetadata(position))),
                                                                                          Optional.absent(),
                                                                                          Optional.absent())));
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
        final int stringResId = changeLikeToSaveExperimentStringHelper.getStringResId(ExperimentString.SUGGESTED_CREATORS_RELATION_LIKED);
        final String key = "suggested_creators_relation_" + relation.value();
        final int resourceId = SuggestedCreatorRelation.LIKED == relation
                               ? stringResId
                               : resources.getIdentifier(key, "string", view.getContext().getPackageName());
        final String text = (resourceId != 0) ? resources.getString(resourceId) : "";
        ((TextView) view.findViewById(R.id.suggested_creator_relation)).setText(text);
    }

    void unsubscribe() {
        profileImageHelper.unsubscribe();
    }

}
