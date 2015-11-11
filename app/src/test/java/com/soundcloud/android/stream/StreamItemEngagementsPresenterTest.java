package com.soundcloud.android.stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.soundcloud.android.accounts.AccountOperations;
import com.soundcloud.android.analytics.ScreenProvider;
import com.soundcloud.android.api.model.ApiPlaylist;
import com.soundcloud.android.associations.RepostOperations;
import com.soundcloud.android.events.EventQueue;
import com.soundcloud.android.events.UIEvent;
import com.soundcloud.android.likes.LikeOperations;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.playlists.PlaylistItem;
import com.soundcloud.android.presentation.PlayableItem;
import com.soundcloud.android.testsupport.AndroidUnitTest;
import com.soundcloud.android.testsupport.fixtures.ModelFixtures;
import com.soundcloud.android.util.CondensedNumberFormatter;
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

public class StreamItemEngagementsPresenterTest extends AndroidUnitTest {

    @Mock LikeOperations likeOperations;
    @Mock RepostOperations repostOperations;
    @Mock AccountOperations accountOperations;
    @Mock StreamItemViewHolder viewHolder;
    @Mock View view;
    @Mock ScreenProvider screenProvider;
    @Captor ArgumentCaptor<StreamItemViewHolder.CardEngagementClickListener> listenerCaptor;

    private final CondensedNumberFormatter numberFormatter =
            CondensedNumberFormatter.create(Locale.US, resources());

    private PlayableItem playableItem;
    private TestEventBus eventBus;
    private StreamItemEngagementsPresenter presenter;
    private PublishSubject<PropertySet> testSubject;

    @Before
    public void setUp() {
        testSubject = PublishSubject.create();
        playableItem = PlaylistItem.from(ModelFixtures.create(ApiPlaylist.class));
        eventBus = new TestEventBus();
        presenter = new StreamItemEngagementsPresenter(numberFormatter, likeOperations, repostOperations,
                accountOperations, eventBus);

        when(accountOperations.getLoggedInUserUrn()).thenReturn(Urn.forUser(999));
        when(likeOperations.toggleLike(playableItem.getEntityUrn(), !playableItem.isLiked())).thenReturn(testSubject);
        when(repostOperations.toggleRepost(playableItem.getEntityUrn(), !playableItem.isReposted())).thenReturn(testSubject);
        when(viewHolder.getContext()).thenReturn(context());
        when(screenProvider.getLastScreenTag()).thenReturn("screen");
    }

    @Test
    public void resetsEngagementsBar() {
        presenter.bind(viewHolder, playableItem);
        verify(viewHolder).resetAdditionalInformation();
    }

    @Test
    public void setsLikeAndRepostsStats() {
        presenter.bind(viewHolder, playableItem);
        verify(viewHolder).showLikeStats(formattedStats(playableItem.getLikesCount()), playableItem.isLiked());
        verify(viewHolder).showRepostStats(formattedStats(playableItem.getRepostCount()), playableItem.isReposted());
    }

    @Test
    public void doesNotShowRepostStatsForOwnTracks() {
        when(accountOperations.isLoggedInUser(playableItem.getCreatorUrn())).thenReturn(true);
        presenter.bind(viewHolder, playableItem);

        verify(viewHolder, never()).showRepostStats(formattedStats(playableItem.getRepostCount()), playableItem.isReposted());
    }

    @Test
    public void togglesLikeOnLikeClick() {
        presenter.bind(viewHolder, playableItem);

        captureListener().onLikeClick(view);

        verify(likeOperations).toggleLike(playableItem.getEntityUrn(), !playableItem.isLiked());
        assertThat(testSubject.hasObservers()).isTrue();
    }

    @Test
    public void togglesRepostOnRepostClick() {
        presenter.bind(viewHolder, playableItem);

        captureListener().onRepostClick(view);

        verify(repostOperations).toggleRepost(playableItem.getEntityUrn(), !playableItem.isReposted());
        assertThat(testSubject.hasObservers()).isTrue();
    }

    @Test
    public void toggleRepostSendsTrackingEvent() {
        presenter.bind(viewHolder, playableItem);

        captureListener().onRepostClick(view);

        UIEvent trackingEvent = (UIEvent) eventBus.lastEventOn(EventQueue.TRACKING);
        assertThat(trackingEvent.getKind()).isEqualTo(playableItem.isReposted() ? UIEvent.KIND_UNREPOST : UIEvent.KIND_REPOST);
        assertThat(trackingEvent.isFromOverflow()).isFalse();
    }

    @Test
    public void toggleLikeSendsTrackingEvent() {
        presenter.bind(viewHolder, playableItem);

        captureListener().onLikeClick(view);

        UIEvent trackingEvent = (UIEvent) eventBus.lastEventOn(EventQueue.TRACKING);
        assertThat(trackingEvent.getKind()).isEqualTo(playableItem.isLiked() ? UIEvent.KIND_UNLIKE : UIEvent.KIND_LIKE);
        assertThat(trackingEvent.isFromOverflow()).isFalse();
    }

    private StreamItemViewHolder.CardEngagementClickListener captureListener() {
        verify(viewHolder).setEngagementClickListener(listenerCaptor.capture());
        return listenerCaptor.getValue();
    }

    private String formattedStats(int stat) {
        return numberFormatter.format(stat);
    }

}
