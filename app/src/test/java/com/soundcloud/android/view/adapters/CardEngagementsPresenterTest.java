package com.soundcloud.android.view.adapters;

import static org.assertj.core.api.Java6Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.soundcloud.android.accounts.AccountOperations;
import com.soundcloud.android.analytics.EventTracker;
import com.soundcloud.android.analytics.ScreenProvider;
import com.soundcloud.android.associations.RepostOperations;
import com.soundcloud.android.configuration.experiments.ChangeLikeToSaveExperiment;
import com.soundcloud.android.events.EventContextMetadata;
import com.soundcloud.android.events.UIEvent;
import com.soundcloud.android.likes.LikeOperations;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.navigation.NavigationExecutor;
import com.soundcloud.android.presentation.PlayableItem;
import com.soundcloud.android.stream.StreamItemViewHolder;
import com.soundcloud.android.testsupport.AndroidUnitTest;
import com.soundcloud.android.testsupport.PlaylistFixtures;
import com.soundcloud.android.tracks.TrackStatsDisplayPolicy;
import com.soundcloud.android.util.CondensedNumberFormatter;
import com.soundcloud.android.view.adapters.CardEngagementsPresenter.CardEngagementClickListener;
import com.soundcloud.android.view.snackbar.FeedbackController;
import com.soundcloud.java.strings.Strings;
import io.reactivex.subjects.SingleSubject;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;

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
    @Mock ChangeLikeToSaveExperiment changeLikeToSaveExperiment;
    @Mock FeedbackController feedbackController;
    @Mock NavigationExecutor navigationExecutor;
    @Mock TrackStatsDisplayPolicy trackStatsDisplayPolicy;

    @Captor ArgumentCaptor<CardEngagementClickListener> listenerCaptor;
    @Captor ArgumentCaptor<UIEvent> uiEventArgumentCaptor;

    private final CondensedNumberFormatter numberFormatter = CondensedNumberFormatter.create(Locale.US, resources());
    private final PlayableItem playlistItem = PlaylistFixtures.playlistItem();
    private final SingleSubject<LikeOperations.LikeResult> testSubject = SingleSubject.create();
    private final SingleSubject<RepostOperations.RepostResult> repostTestSubject = SingleSubject.create();
    private final EventContextMetadata contextMetadata = EventContextMetadata.builder().build();

    private CardEngagementsPresenter presenter;

    @Before
    public void setUp() {
        presenter = new CardEngagementsPresenter(numberFormatter,
                                                 likeOperations,
                                                 repostOperations,
                                                 accountOperations,
                                                 eventTracker,
                                                 changeLikeToSaveExperiment,
                                                 feedbackController,
                                                 navigationExecutor,
                                                 trackStatsDisplayPolicy);

        when(accountOperations.getLoggedInUserUrn()).thenReturn(Urn.forUser(999));
        when(likeOperations.toggleLike(playlistItem.getUrn(), !playlistItem.isUserLike())).thenReturn(testSubject);
        when(repostOperations.toggleRepost(playlistItem.getUrn(), !playlistItem.isUserRepost())).thenReturn(repostTestSubject);
        when(viewHolder.getContext()).thenReturn(context());
        when(screenProvider.getLastScreenTag()).thenReturn("screen");
    }

    @Test
    public void setsLikeAndRepostsStatsWhenDisplayIsEnabled() {
        when(trackStatsDisplayPolicy.displayLikesCount(any())).thenReturn(true);
        when(trackStatsDisplayPolicy.displayRepostsCount(any())).thenReturn(true);

        presenter.bind(viewHolder, playlistItem, contextMetadata);

        verify(viewHolder).showLikeStats(formattedStats(playlistItem.likesCount()), playlistItem.isUserLike());
        verify(viewHolder).showRepostStats(formattedStats(playlistItem.repostsCount()), playlistItem.isUserRepost());
    }

    @Test
    public void setsBlackLikeAndRepostsStatsWhenDisplayIsDisabled() {
        when(trackStatsDisplayPolicy.displayLikesCount(any())).thenReturn(false);
        when(trackStatsDisplayPolicy.displayRepostsCount(any())).thenReturn(false);

        presenter.bind(viewHolder, playlistItem, contextMetadata);

        verify(viewHolder).showLikeStats(Strings.EMPTY, playlistItem.isUserLike());
        verify(viewHolder).showRepostStats(Strings.EMPTY, playlistItem.isUserRepost());
    }

    @Test
    public void setsDuration() {
        presenter.bind(viewHolder, playlistItem, contextMetadata);
        verify(viewHolder).showDuration("0:12");
    }

    @Test
    public void setsGenre() {
        presenter.bind(viewHolder, playlistItem, contextMetadata);
        verify(viewHolder).showGenre("clownstep");
    }

    @Test
    public void doesNotShowRepostStatsForOwnTracks() {
        when(accountOperations.isLoggedInUser(playlistItem.creatorUrn())).thenReturn(true);
        presenter.bind(viewHolder, playlistItem, contextMetadata);

        verify(viewHolder, never()).showRepostStats(formattedStats(playlistItem.repostsCount()),
                                                    playlistItem.isUserRepost());
        verify(viewHolder).hideRepostStats();
    }

    @Test
    public void togglesLikeOnLikeClick() {
        presenter.bind(viewHolder, playlistItem, contextMetadata);

        captureListener().onLikeClick(view);

        verify(likeOperations).toggleLike(playlistItem.getUrn(), !playlistItem.isUserLike());
        assertThat(testSubject.hasObservers()).isTrue();
    }

    @Test
    public void togglesRepostOnRepostClick() {
        presenter.bind(viewHolder, playlistItem, contextMetadata);

        captureListener().onRepostClick(view);

        verify(repostOperations).toggleRepost(playlistItem.getUrn(), !playlistItem.isUserRepost());
        assertThat(repostTestSubject.hasObservers()).isTrue();
    }

    @Test
    public void toggleRepostSendsTrackingEvent() {
        presenter.bind(viewHolder, playlistItem, contextMetadata);

        captureListener().onRepostClick(view);

        verify(eventTracker).trackEngagement(uiEventArgumentCaptor.capture());

        UIEvent trackingEvent = uiEventArgumentCaptor.getValue();
        assertThat(trackingEvent.kind()).isEqualTo(playlistItem.isUserRepost() ?
                                                   UIEvent.Kind.UNREPOST :
                                                   UIEvent.Kind.REPOST);
        assertThat(trackingEvent.isFromOverflow().get()).isFalse();
    }

    @Test
    public void toggleLikeSendsTrackingEvent() {
        presenter.bind(viewHolder, playlistItem, contextMetadata);

        captureListener().onLikeClick(view);

        verify(eventTracker).trackEngagement(uiEventArgumentCaptor.capture());

        UIEvent trackingEvent = uiEventArgumentCaptor.getValue();
        assertThat(trackingEvent.kind()).isEqualTo(playlistItem.isUserLike() ? UIEvent.Kind.UNLIKE : UIEvent.Kind.LIKE);
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
