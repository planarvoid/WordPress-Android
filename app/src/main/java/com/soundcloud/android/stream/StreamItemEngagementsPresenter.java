package com.soundcloud.android.stream;

import static com.soundcloud.java.strings.Strings.EMPTY;

import com.soundcloud.android.associations.RepostOperations;
import com.soundcloud.android.likes.LikeOperations;
import com.soundcloud.android.likes.LikeToggleSubscriber;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.playlists.RepostResultSubscriber;
import com.soundcloud.android.presentation.PlayableItem;
import com.soundcloud.android.util.CondensedNumberFormatter;
import rx.android.schedulers.AndroidSchedulers;

import android.view.View;

import javax.inject.Inject;

class StreamItemEngagementsPresenter {

    private final CondensedNumberFormatter numberFormatter;
    private final LikeOperations likeOperations;
    private final RepostOperations repostOperations;

    @Inject
    StreamItemEngagementsPresenter(CondensedNumberFormatter numberFormatter,
                                   LikeOperations likeOperations,
                                   RepostOperations repostOperations) {
        this.numberFormatter = numberFormatter;
        this.likeOperations = likeOperations;
        this.repostOperations = repostOperations;
    }

    void bind(StreamItemViewHolder trackView, final PlayableItem playable) {
        trackView.resetAdditionalInformation();
        trackView.showLikeStats(getCountString(playable.getLikesCount()), playable.isLiked());
        trackView.showRepostStats(getCountString(playable.getRepostCount()), playable.isReposted());

        trackView.setEngagementClickListener(new StreamItemViewHolder.CardEngagementClickListener() {
            @Override
            public void onLikeClick(View likeButton) {
                handleLike(likeButton, playable);
            }

            @Override
            public void onRepostClick(View repostButton) {
                handleRepost(repostButton, playable);
            }
        });
    }

    private void handleRepost(View repostButton, PlayableItem playableItem) {
        final Urn entityUrn = playableItem.getEntityUrn();
        final boolean addRepost = !playableItem.isReposted();
        repostOperations.toggleRepost(entityUrn, addRepost)
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new RepostResultSubscriber(repostButton.getContext(), addRepost));
    }

    private void handleLike(View likeButton, PlayableItem playableItem) {
        final Urn entityUrn = playableItem.getEntityUrn();
        final boolean addLike = !playableItem.isLiked();
        likeOperations.toggleLike(entityUrn, addLike)
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new LikeToggleSubscriber(likeButton.getContext(), addLike));
    }

    private String getCountString(int count) {
        return (count > 0) ? numberFormatter.format(count) : EMPTY;
    }
}
