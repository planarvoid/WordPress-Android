package com.soundcloud.android.suggestedcreators;

import static com.soundcloud.android.events.EntityStateChangedEvent.fromFollowing;
import static com.soundcloud.android.suggestedcreators.SuggestedCreatorsFixtures.createSuggestedCreatorItems;
import static org.assertj.core.api.Assertions.assertThat;

import com.soundcloud.android.events.EntityStateChangedEvent;
import com.soundcloud.android.model.EntityProperty;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.testsupport.AndroidUnitTest;
import com.soundcloud.android.users.UserProperty;
import com.soundcloud.java.collections.PropertySet;
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
        final EntityStateChangedEvent event = createEvent(item.creator().urn(), false);

        suggestedCreatorsAdapter.onFollowingEntityChange(event);

        assertThat(item.following).isFalse();
    }

    @Test
    public void doNotUpdateUnrelatedItemOnFollowEvent() throws Exception {
        final SuggestedCreatorItem item = suggestedCreatorItems.get(0);
        final EntityStateChangedEvent event = createEvent(Urn.forUser(104918), false);

        suggestedCreatorsAdapter.onFollowingEntityChange(event);

        assertThat(item.following).isTrue();
    }

    private EntityStateChangedEvent createEvent(Urn urn, boolean followed) {
        return fromFollowing(PropertySet.create()
                                        .put(EntityProperty.URN, urn)
                                        .put(UserProperty.IS_FOLLOWED_BY_ME, followed));
    }
}
