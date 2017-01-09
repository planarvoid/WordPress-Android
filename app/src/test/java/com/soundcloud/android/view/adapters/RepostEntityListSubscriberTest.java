package com.soundcloud.android.view.adapters;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.soundcloud.android.events.RepostsStatusEvent;
import com.soundcloud.android.events.RepostsStatusEvent.RepostStatus;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.presentation.ListItem;
import com.soundcloud.android.presentation.RecyclerItemAdapter;
import com.soundcloud.android.presentation.RepostableItem;
import com.soundcloud.android.testsupport.AndroidUnitTest;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;

import android.support.v7.widget.RecyclerView;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class RepostEntityListSubscriberTest extends AndroidUnitTest {

    private static final Urn URN = Urn.forTrack(123);
    private static final RepostStatus repostStatus = RepostStatus.createReposted(URN, 10);
    private static final Urn NON_MATCHING_URN = Urn.forTrack(124);
    private static final RepostStatus nonMatchingRepostStatus = RepostStatus.createReposted(NON_MATCHING_URN, 10);

    @Mock private ListItem nonRepostableListItem;
    @Mock private RepostableListItem repostableListItem;
    @Mock private RepostableListItem updatedListItem;
    @Mock private RecyclerItemAdapter<ListItem, RecyclerView.ViewHolder> adapter;

    private RepostEntityListSubscriber subscriber;
    private List<ListItem> adapterItems;

    @Before
    public void setUp() throws Exception {
        subscriber = new RepostEntityListSubscriber(adapter);
        when(nonRepostableListItem.getUrn()).thenReturn(URN);
        when(repostableListItem.getUrn()).thenReturn(URN);
        when(repostableListItem.updatedWithRepost(repostStatus)).thenReturn(updatedListItem);

        adapterItems = Arrays.asList(nonRepostableListItem, repostableListItem);
        when(adapter.getItems()).thenReturn(adapterItems);
    }

    @Test
    public void replacesUpdatedLikeableItemWithMatchingUrnInList() {
        final RepostsStatusEvent repostsStatusEvent = RepostsStatusEvent.create(Collections.singletonMap(URN, repostStatus));

        subscriber.onNext(repostsStatusEvent);

        verify(adapter).notifyDataSetChanged();
        assertThat(adapterItems.get(1)).isEqualTo(updatedListItem);
    }

    @Test
    public void doesNotUpdateItemWithNonMatchingUrn() {
        final RepostsStatusEvent repostsStatusEvent = RepostsStatusEvent.create(Collections.singletonMap(NON_MATCHING_URN, nonMatchingRepostStatus));

        subscriber.onNext(repostsStatusEvent);

        verify(adapter, never()).notifyDataSetChanged();
        assertThat(adapterItems.get(1)).isEqualTo(repostableListItem);
    }

    abstract static class RepostableListItem implements ListItem, RepostableItem {
    }
}
