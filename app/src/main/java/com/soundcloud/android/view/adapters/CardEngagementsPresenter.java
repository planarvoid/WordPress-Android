package com.soundcloud.android.view.adapters;

import static com.soundcloud.java.strings.Strings.EMPTY;
import static com.soundcloud.java.strings.Strings.isNotBlank;

import com.soundcloud.android.accounts.AccountOperations;
import com.soundcloud.android.analytics.EventTracker;
import com.soundcloud.android.analytics.PromotedSourceInfo;
import com.soundcloud.android.associations.RepostOperations;
import com.soundcloud.android.events.EntityMetadata;
import com.soundcloud.android.events.EventContextMetadata;
import com.soundcloud.android.events.UIEvent;
import com.soundcloud.android.likes.LikeOperations;
import com.soundcloud.android.likes.LikeToggleSubscriber;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.playlists.PromotedPlaylistItem;
import com.soundcloud.android.playlists.RepostResultSubscriber;
import com.soundcloud.android.presentation.PlayableItem;
import com.soundcloud.android.tracks.PromotedTrackItem;
import com.soundcloud.android.util.CondensedNumberFormatter;
import com.soundcloud.android.utils.ScTextUtils;
import rx.android.schedulers.AndroidSchedulers;

import android.view.View;

import javax.inject.Inject;
import java.util.concurrent.TimeUnit;

public class CardEngagementsPresenter {

    public interface CardEngagementClickListener {
        void onLikeClick(View likeButton);

        void onRepostClick(View repostButton);
    }

    private final CondensedNumberFormatter numberFormatter;
    private final LikeOperations likeOperations;
    private final RepostOperations repostOperations;
    private final AccountOperations accountOperations;
    private final EventTracker eventTracker;

    @Inject
    CardEngagementsPresenter(CondensedNumberFormatter numberFormatter,
                             LikeOperations likeOperations,
                             RepostOperations repostOperations,
                             AccountOperations accountOperations,
                             EventTracker eventTracker) {
        this.numberFormatter = numberFormatter;
        this.likeOperations = likeOperations;
        this.repostOperations = repostOperations;
        this.accountOperations = accountOperations;
        this.eventTracker = eventTracker;
    }

    public void bind(final CardViewHolder viewHolder,
                     final PlayableItem playable,
                     final EventContextMetadata contextMetadata) {

        viewHolder.showDuration(ScTextUtils.formatTimestamp(playable.getDuration(), TimeUnit.MILLISECONDS));
        if (isNotBlank(playable.getGenre())) {
            viewHolder.showGenre(playable.getGenre());
        }

        viewHolder.showLikeStats(getCountString(playable.getLikesCount()), playable.isLiked());

        if (accountOperations.isLoggedInUser(playable.getCreatorUrn())) {
            viewHolder.hideRepostStats();
        } else {
            viewHolder.showRepostStats(getCountString(playable.getRepostCount()), playable.isRepostedByCurrentUser());
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
        final boolean addRepost = !playableItem.isRepostedByCurrentUser();
        repostOperations.toggleRepost(entityUrn, addRepost)
                        .observeOn(AndroidSchedulers.mainThread())
                        .subscribe(new RepostResultSubscriber(repostButton.getContext(), addRepost));

        eventTracker.trackEngagement(UIEvent.fromToggleRepost(addRepost, entityUrn,
                                                                       contextMetadata,
                                                                       getPromotedSourceInfo(playableItem),
                                                                       EntityMetadata.from(playableItem)));
    }

    private void handleLike(View likeButton, PlayableItem playableItem, EventContextMetadata contextMetadata) {
        final Urn entityUrn = playableItem.getUrn();
        final boolean addLike = !playableItem.isLiked();
        likeOperations.toggleLike(entityUrn, addLike)
                      .observeOn(AndroidSchedulers.mainThread())
                      .subscribe(new LikeToggleSubscriber(likeButton.getContext(), addLike));

        eventTracker.trackEngagement(UIEvent.fromToggleLike(addLike, entityUrn,
                                                                     contextMetadata,
                                                                     getPromotedSourceInfo(playableItem),
                                                                     EntityMetadata.from(playableItem)));
    }

    private PromotedSourceInfo getPromotedSourceInfo(PlayableItem playable) {
        if (playable instanceof PromotedPlaylistItem) {
            return PromotedSourceInfo.fromItem((PromotedPlaylistItem) playable);
        } else if (playable instanceof PromotedTrackItem) {
            return PromotedSourceInfo.fromItem((PromotedTrackItem) playable);
        } else {
            return null;
        }
    }

    private String getCountString(int count) {
        return (count > 0) ? numberFormatter.format(count) : EMPTY;
    }
}
