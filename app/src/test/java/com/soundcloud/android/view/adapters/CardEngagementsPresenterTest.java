package com.soundcloud.android.view.adapters;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.soundcloud.android.accounts.AccountOperations;
import com.soundcloud.android.analytics.ScreenProvider;
import com.soundcloud.android.api.model.ApiPlaylist;
import com.soundcloud.android.associations.RepostOperations;
import com.soundcloud.android.events.EventContextMetadata;
import com.soundcloud.android.events.EventQueue;
import com.soundcloud.android.events.UIEvent;
import com.soundcloud.android.likes.LikeOperations;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.playlists.PlaylistItem;
import com.soundcloud.android.presentation.PlayableItem;
import com.soundcloud.android.stream.StreamItemViewHolder;
import com.soundcloud.android.testsupport.AndroidUnitTest;
import com.soundcloud.android.testsupport.fixtures.ModelFixtures;
import com.soundcloud.android.util.CondensedNumberFormatter;
import com.soundcloud.android.view.adapters.CardEngagementsPresenter;
import com.soundcloud.android.view.adapters.CardEngagementsPresenter.CardEngagementClickListener;
import com.soundcloud.java.collections.PropertySet;
import com.soundcloud.rx.eventbus.TestEventBus;
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
    @Captor ArgumentCaptor<CardEngagementClickListener> listenerCaptor;

    private final CondensedNumberFormatter numberFormatter = CondensedNumberFormatter.create(Locale.US, resources());
    private final PlayableItem playableItem = PlaylistItem.from(ModelFixtures.create(ApiPlaylist.class));
    private final TestEventBus eventBus = new TestEventBus();
    private final PublishSubject<PropertySet> testSubject = PublishSubject.create();
    private final EventContextMetadata contextMetadata = EventContextMetadata.builder().build();

    private CardEngagementsPresenter presenter;

    @Before
    public void setUp() {
        presenter = new CardEngagementsPresenter(
                numberFormatter, likeOperations, repostOperations, accountOperations, eventBus);

        when(accountOperations.getLoggedInUserUrn()).thenReturn(Urn.forUser(999));
        when(likeOperations.toggleLike(playableItem.getEntityUrn(), !playableItem.isLiked())).thenReturn(testSubject);
        when(repostOperations.toggleRepost(playableItem.getEntityUrn(), !playableItem.isReposted())).thenReturn(testSubject);
        when(viewHolder.getContext()).thenReturn(context());
        when(screenProvider.getLastScreenTag()).thenReturn("screen");
    }

    @Test
    public void setsLikeAndRepostsStats() {
        presenter.bind(viewHolder, playableItem, contextMetadata);
        verify(viewHolder).showLikeStats(formattedStats(playableItem.getLikesCount()), playableItem.isLiked());
        verify(viewHolder).showRepostStats(formattedStats(playableItem.getRepostCount()), playableItem.isReposted());
    }

    @Test
    public void doesNotShowRepostStatsForOwnTracks() {
        when(accountOperations.isLoggedInUser(playableItem.getCreatorUrn())).thenReturn(true);
        presenter.bind(viewHolder, playableItem, contextMetadata);

        verify(viewHolder, never()).showRepostStats(formattedStats(playableItem.getRepostCount()), playableItem.isReposted());
    }

    @Test
    public void togglesLikeOnLikeClick() {
        presenter.bind(viewHolder, playableItem, contextMetadata);

        captureListener().onLikeClick(view);

        verify(likeOperations).toggleLike(playableItem.getEntityUrn(), !playableItem.isLiked());
        assertThat(testSubject.hasObservers()).isTrue();
    }

    @Test
    public void togglesRepostOnRepostClick() {
        presenter.bind(viewHolder, playableItem, contextMetadata);

        captureListener().onRepostClick(view);

        verify(repostOperations).toggleRepost(playableItem.getEntityUrn(), !playableItem.isReposted());
        assertThat(testSubject.hasObservers()).isTrue();
    }

    @Test
    public void toggleRepostSendsTrackingEvent() {
        presenter.bind(viewHolder, playableItem, contextMetadata);

        captureListener().onRepostClick(view);

        UIEvent trackingEvent = (UIEvent) eventBus.lastEventOn(EventQueue.TRACKING);
        assertThat(trackingEvent.getKind()).isEqualTo(playableItem.isReposted() ? UIEvent.KIND_UNREPOST : UIEvent.KIND_REPOST);
        assertThat(trackingEvent.isFromOverflow()).isFalse();
    }

    @Test
    public void toggleLikeSendsTrackingEvent() {
        presenter.bind(viewHolder, playableItem, contextMetadata);

        captureListener().onLikeClick(view);

        UIEvent trackingEvent = (UIEvent) eventBus.lastEventOn(EventQueue.TRACKING);
        assertThat(trackingEvent.getKind()).isEqualTo(playableItem.isLiked() ? UIEvent.KIND_UNLIKE : UIEvent.KIND_LIKE);
        assertThat(trackingEvent.isFromOverflow()).isFalse();
    }

    private CardEngagementClickListener captureListener() {
        verify(viewHolder).setEngagementClickListener(listenerCaptor.capture());
        return listenerCaptor.getValue();
    }

    private String formattedStats(int stat) {
        return numberFormatter.format(stat);
    }

}
