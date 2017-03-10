package com.soundcloud.android.view.adapters;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.soundcloud.android.accounts.AccountOperations;
import com.soundcloud.android.analytics.EventTracker;
import com.soundcloud.android.analytics.ScreenProvider;
import com.soundcloud.android.api.model.ApiPlaylist;
import com.soundcloud.android.associations.RepostOperations;
import com.soundcloud.android.events.EventContextMetadata;
import com.soundcloud.android.events.UIEvent;
import com.soundcloud.android.likes.LikeOperations;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.playlists.PlaylistItem;
import com.soundcloud.android.presentation.PlayableItem;
import com.soundcloud.android.stream.StreamItemViewHolder;
import com.soundcloud.android.testsupport.AndroidUnitTest;
import com.soundcloud.android.testsupport.fixtures.ModelFixtures;
import com.soundcloud.android.util.CondensedNumberFormatter;
import com.soundcloud.android.view.adapters.CardEngagementsPresenter.CardEngagementClickListener;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import rx.subjects.PublishSubject;

import android.view.View;

import java.util.Locale;

public class CardEngagementsPresenterTest extends AndroidUnitTest {

    @Mock LikeOperations likeOperations;
    @Mock RepostOperations repostOperations;
    @Mock AccountOperations accountOperations;
    @Mock StreamItemViewHolder viewHolder;
    @Mock View view;
    @Mock ScreenProvider screenProvider;
    @Mock EventTracker eventTracker;
    @Captor ArgumentCaptor<CardEngagementClickListener> listenerCaptor;
    @Captor ArgumentCaptor<UIEvent> uiEventArgumentCaptor;

    private final CondensedNumberFormatter numberFormatter = CondensedNumberFormatter.create(Locale.US, resources());
    private final PlayableItem playableItem = ModelFixtures.playlistItem();
    private final PublishSubject<LikeOperations.LikeResult> testSubject = PublishSubject.create();
    private final PublishSubject<RepostOperations.RepostResult> repostTestSubject = PublishSubject.create();
    private final EventContextMetadata contextMetadata = EventContextMetadata.builder().build();

    private CardEngagementsPresenter presenter;

    @Before
    public void setUp() {
        presenter = new CardEngagementsPresenter(
                numberFormatter, likeOperations, repostOperations, accountOperations, eventTracker);

        when(accountOperations.getLoggedInUserUrn()).thenReturn(Urn.forUser(999));
        when(likeOperations.toggleLike(playableItem.getUrn(), !playableItem.isUserLike())).thenReturn(testSubject);
        when(repostOperations.toggleRepost(playableItem.getUrn(), !playableItem.isUserRepost())).thenReturn(repostTestSubject);
        when(viewHolder.getContext()).thenReturn(context());
        when(screenProvider.getLastScreenTag()).thenReturn("screen");
    }

    @Test
    public void setsLikeAndRepostsStats() {
        presenter.bind(viewHolder, playableItem, contextMetadata);
        verify(viewHolder).showLikeStats(formattedStats(playableItem.likesCount()), playableItem.isUserLike());
        verify(viewHolder).showRepostStats(formattedStats(playableItem.repostsCount()), playableItem.isUserRepost());
    }

    @Test
    public void setsDuration() {
        presenter.bind(viewHolder, playableItem, contextMetadata);
        verify(viewHolder).showDuration("2:03");
    }

    @Test
    public void setsGenre() {
        presenter.bind(viewHolder, playableItem, contextMetadata);
        verify(viewHolder).showGenre("clownstep");
    }

    @Test
    public void doesNotShowRepostStatsForOwnTracks() {
        when(accountOperations.isLoggedInUser(playableItem.creatorUrn())).thenReturn(true);
        presenter.bind(viewHolder, playableItem, contextMetadata);

        verify(viewHolder, never()).showRepostStats(formattedStats(playableItem.repostsCount()),
                                                    playableItem.isUserRepost());
        verify(viewHolder).hideRepostStats();
    }

    @Test
    public void togglesLikeOnLikeClick() {
        presenter.bind(viewHolder, playableItem, contextMetadata);

        captureListener().onLikeClick(view);

        verify(likeOperations).toggleLike(playableItem.getUrn(), !playableItem.isUserLike());
        assertThat(testSubject.hasObservers()).isTrue();
    }

    @Test
    public void togglesRepostOnRepostClick() {
        presenter.bind(viewHolder, playableItem, contextMetadata);

        captureListener().onRepostClick(view);

        verify(repostOperations).toggleRepost(playableItem.getUrn(), !playableItem.isUserRepost());
        assertThat(repostTestSubject.hasObservers()).isTrue();
    }

    @Test
    public void toggleRepostSendsTrackingEvent() {
        presenter.bind(viewHolder, playableItem, contextMetadata);

        captureListener().onRepostClick(view);

        verify(eventTracker).trackEngagement(uiEventArgumentCaptor.capture());

        UIEvent trackingEvent = uiEventArgumentCaptor.getValue();
        assertThat(trackingEvent.kind()).isEqualTo(playableItem.isUserRepost() ?
                                                   UIEvent.Kind.UNREPOST :
                                                   UIEvent.Kind.REPOST);
        assertThat(trackingEvent.isFromOverflow().get()).isFalse();
    }

    @Test
    public void toggleLikeSendsTrackingEvent() {
        presenter.bind(viewHolder, playableItem, contextMetadata);

        captureListener().onLikeClick(view);

        verify(eventTracker).trackEngagement(uiEventArgumentCaptor.capture());

        UIEvent trackingEvent = uiEventArgumentCaptor.getValue();
        assertThat(trackingEvent.kind()).isEqualTo(playableItem.isUserLike() ? UIEvent.Kind.UNLIKE : UIEvent.Kind.LIKE);
        assertThat(trackingEvent.isFromOverflow().get()).isFalse();
    }

    private CardEngagementClickListener captureListener() {
        verify(viewHolder).setEngagementClickListener(listenerCaptor.capture());
        return listenerCaptor.getValue();
    }

    private String formattedStats(int stat) {
        return numberFormatter.format(stat);
    }

}
