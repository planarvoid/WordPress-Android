package com.soundcloud.android.suggestedcreators;

import static com.soundcloud.android.suggestedcreators.SuggestedCreatorsFixtures.createSuggestedCreatorItems;
import static org.assertj.core.api.Assertions.assertThat;

import com.soundcloud.android.events.FollowingStatusEvent;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.testsupport.AndroidUnitTest;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;

import java.util.List;

public class SuggestedCreatorsAdapterTest extends AndroidUnitTest {

    private final List<SuggestedCreatorItem> suggestedCreatorItems = createSuggestedCreatorItems(1);

    @Mock SuggestedCreatorRenderer suggestedCreatorRenderer;

    private SuggestedCreatorsAdapter suggestedCreatorsAdapter;


    @Before
    public void setUp() throws Exception {
        suggestedCreatorsAdapter = new SuggestedCreatorsAdapter(suggestedCreatorRenderer);
        suggestedCreatorsAdapter.onNext(suggestedCreatorItems);
    }

    @Test
    public void updateItemOnFollowEvent() throws Exception {
        final SuggestedCreatorItem item = suggestedCreatorItems.get(0);
        final FollowingStatusEvent event = FollowingStatusEvent.createUnfollowed(item.creator().urn(), 0);

        suggestedCreatorsAdapter.onFollowingEntityChange(event);

        assertThat(item.following).isFalse();
    }

    @Test
    public void doNotUpdateUnrelatedItemOnFollowEvent() throws Exception {
        final SuggestedCreatorItem item = suggestedCreatorItems.get(0);
        final FollowingStatusEvent event = FollowingStatusEvent.createFollowed(Urn.forUser(104918), 1);

        suggestedCreatorsAdapter.onFollowingEntityChange(event);

        assertThat(item.following).isTrue();
    }

}
