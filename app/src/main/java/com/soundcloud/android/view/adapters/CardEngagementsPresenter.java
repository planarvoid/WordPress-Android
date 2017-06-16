package com.soundcloud.android.view.adapters;

import static com.soundcloud.java.strings.Strings.EMPTY;
import static com.soundcloud.java.strings.Strings.isNotBlank;

import com.soundcloud.android.accounts.AccountOperations;
import com.soundcloud.android.analytics.EventTracker;
import com.soundcloud.android.analytics.PromotedSourceInfo;
import com.soundcloud.android.associations.RepostOperations;
import com.soundcloud.android.configuration.experiments.ChangeLikeToSaveExperiment;
import com.soundcloud.android.events.EntityMetadata;
import com.soundcloud.android.events.EventContextMetadata;
import com.soundcloud.android.events.UIEvent;
import com.soundcloud.android.likes.LikeOperations;
import com.soundcloud.android.likes.LikeToggleObserver;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.navigation.NavigationExecutor;
import com.soundcloud.android.playlists.RepostResultSingleObserver;
import com.soundcloud.android.presentation.PlayableItem;
import com.soundcloud.android.util.CondensedNumberFormatter;
import com.soundcloud.android.utils.ScTextUtils;
import com.soundcloud.android.view.snackbar.FeedbackController;
import io.reactivex.android.schedulers.AndroidSchedulers;

import android.support.annotation.Nullable;
import android.view.View;

import javax.inject.Inject;
import java.util.concurrent.TimeUnit;

public class CardEngagementsPresenter {

    private final CondensedNumberFormatter numberFormatter;
    private final LikeOperations likeOperations;
    private final RepostOperations repostOperations;
    private final AccountOperations accountOperations;
    private final EventTracker eventTracker;
    private final ChangeLikeToSaveExperiment changeLikeToSaveExperiment;
    private final FeedbackController feedbackController;
    private final NavigationExecutor navigationExecutor;

    public interface CardEngagementClickListener {
        void onLikeClick(View likeButton);

        void onRepostClick(View repostButton);
    }

    @Inject
    CardEngagementsPresenter(CondensedNumberFormatter numberFormatter,
                             LikeOperations likeOperations,
                             RepostOperations repostOperations,
                             AccountOperations accountOperations,
                             EventTracker eventTracker,
                             ChangeLikeToSaveExperiment changeLikeToSaveExperiment,
                             FeedbackController feedbackController,
                             NavigationExecutor navigationExecutor) {
        this.numberFormatter = numberFormatter;
        this.likeOperations = likeOperations;
        this.repostOperations = repostOperations;
        this.accountOperations = accountOperations;
        this.eventTracker = eventTracker;
        this.changeLikeToSaveExperiment = changeLikeToSaveExperiment;
        this.feedbackController = feedbackController;
        this.navigationExecutor = navigationExecutor;
    }

    public void bind(final CardViewHolder viewHolder,
                     final PlayableItem playable,
                     final EventContextMetadata contextMetadata) {

        viewHolder.showDuration(ScTextUtils.formatTimestamp(playable.getDuration(), TimeUnit.MILLISECONDS));
        if (playable.genre().isPresent() && isNotBlank(playable.genre().get())) {
            viewHolder.showGenre(playable.genre().get());
        }

        viewHolder.showLikeStats(getCountString(playable.likesCount()), playable.isUserLike());

        if (accountOperations.isLoggedInUser(playable.creatorUrn())) {
            viewHolder.hideRepostStats();
        } else {
            viewHolder.showRepostStats(getCountString(playable.repostsCount()), playable.isUserRepost());
        }

        viewHolder.setEngagementClickListener(new CardEngagementClickListener() {
            @Override
            public void onLikeClick(View likeButton) {
                handleLike(likeButton, playable, contextMetadata);
            }

            @Override
            public void onRepostClick(View repostButton) {
                handleRepost(repostButton, playable, contextMetadata);
            }
        });
    }

    private void handleRepost(View repostButton, PlayableItem playableItem, EventContextMetadata contextMetadata) {
        final Urn entityUrn = playableItem.getUrn();
        final boolean addRepost = !playableItem.isUserRepost();
        repostOperations.toggleRepost(entityUrn, addRepost)
                        .observeOn(AndroidSchedulers.mainThread())
                        .subscribe(new RepostResultSingleObserver(repostButton.getContext()));

        eventTracker.trackEngagement(UIEvent.fromToggleRepost(addRepost, entityUrn,
                                                                       contextMetadata,
                                                                       getPromotedSourceInfo(playableItem),
                                                                       EntityMetadata.from(playableItem)));
    }

    private void handleLike(View likeButton, PlayableItem playableItem, EventContextMetadata contextMetadata) {
        final Urn entityUrn = playableItem.getUrn();
        final boolean addLike = !playableItem.isUserLike();
        likeOperations.toggleLike(entityUrn, addLike)
                      .observeOn(AndroidSchedulers.mainThread())
                      .subscribe(new LikeToggleObserver(likeButton.getContext(), addLike, changeLikeToSaveExperiment, feedbackController, navigationExecutor));

        eventTracker.trackEngagement(UIEvent.fromToggleLike(addLike, entityUrn,
                                                                     contextMetadata,
                                                                     getPromotedSourceInfo(playableItem),
                                                                     EntityMetadata.from(playableItem)));
    }

    @Nullable
    private PromotedSourceInfo getPromotedSourceInfo(PlayableItem playable) {
        if (playable.isPromoted()) {
            return PromotedSourceInfo.fromItem(playable);
        } else {
            return null;
        }
    }

    private String getCountString(int count) {
        return (count > 0) ? numberFormatter.format(count) : EMPTY;
    }
}
