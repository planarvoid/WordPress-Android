package com.soundcloud.android.view.adapters;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Matchers.anyInt;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.soundcloud.android.events.LikesStatusEvent;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.presentation.LikeableItem;
import com.soundcloud.android.presentation.ListItem;
import com.soundcloud.android.presentation.RecyclerItemAdapter;
import com.soundcloud.android.testsupport.AndroidUnitTest;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;

import android.support.v7.widget.RecyclerView;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class LikeEntityListSubscriberTest extends AndroidUnitTest {

    private static final Urn URN = Urn.forTrack(123);
    private static final LikesStatusEvent.LikeStatus likeStatus = LikesStatusEvent.LikeStatus.create(URN, true, 10);
    private static final Urn NON_MATCHING_URN = Urn.forTrack(124);
    private static final LikesStatusEvent.LikeStatus nonMatchingLikeStatus = LikesStatusEvent.LikeStatus.create(NON_MATCHING_URN, true, 10);

    @Mock private ListItem nonLikeableListItem;
    @Mock private LikeableListItem likeableListItem;
    @Mock private ListItem updatedListItem;
    @Mock private RecyclerItemAdapter<ListItem, RecyclerView.ViewHolder> adapter;

    private LikeEntityListSubscriber subscriber;
    private List<ListItem> adapterItems;

    @Before
    public void setUp() throws Exception {
        subscriber = new LikeEntityListSubscriber(adapter);
        when(nonLikeableListItem.getUrn()).thenReturn(URN);
        when(likeableListItem.getUrn()).thenReturn(URN);
        when(likeableListItem.updatedWithLike(likeStatus)).thenReturn(updatedListItem);

        adapterItems = Arrays.asList(nonLikeableListItem, likeableListItem);
        when(adapter.getItems()).thenReturn(adapterItems);
    }

    @Test
    public void replacesUpdatedLikeableItemWithMatchingUrnInList() {
        final LikesStatusEvent likesStatusEvent = LikesStatusEvent.createFromSync(Collections.singletonMap(URN, likeStatus));

        subscriber.onNext(likesStatusEvent);

        verify(adapter).notifyItemChanged(1);
        assertThat(adapterItems.get(1)).isEqualTo(updatedListItem);
    }

    @Test
    public void doesNotUpdateItemWithNonMatchingUrn() {
        final LikesStatusEvent likesStatusEvent = LikesStatusEvent.createFromSync(Collections.singletonMap(NON_MATCHING_URN, nonMatchingLikeStatus));

        subscriber.onNext(likesStatusEvent);

        verify(adapter, never()).notifyItemChanged(anyInt());
        assertThat(adapterItems.get(1)).isEqualTo(likeableListItem);
    }
//
//    @Test
//    public void doesNotNotifyWithNoMatchingUrns() {
//        TrackItem track1 = TrackItem.from(ModelFixtures.create(ApiTrack.class));
//        TrackItem track2 = TrackItem.from(ModelFixtures.create(ApiTrack.class));
//
//        PropertySet changeSet = ModelFixtures.create(ApiTrack.class).toPropertySet();
//
//        when(adapter.getItems()).thenReturn(newArrayList(track1, track2));
//
//        final EntityStateChangedEvent event = EntityStateChangedEvent.fromFollowing(changeSet);
//        subscriber.onNext(event);
//
//        verify(adapter, never()).notifyItemChanged(anyInt());
//    }

    abstract static class LikeableListItem implements ListItem, LikeableItem {
    }
}
