package com.soundcloud.android.stream;

import static org.mockito.Matchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import com.soundcloud.android.events.EntityStateChangedEvent;
import com.soundcloud.android.model.EntityProperty;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.testsupport.AndroidUnitTest;
import com.soundcloud.java.collections.PropertySet;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;

public class UpdateStreamEntitySubscriberTest extends AndroidUnitTest {

    @Mock StreamAdapter mockAdapter;

    private EntityStateChangedEvent followEvent;
    private EntityStateChangedEvent nonFollowEvent;

    UpdateStreamEntitySubscriber testSubscriber;

    @Before
    public void setUp() throws Exception {
        final Urn urn = Urn.forUser(1234567788);
        followEvent = EntityStateChangedEvent.fromFollowing(PropertySet.create()
                                                                       .put(EntityProperty.URN,
                                                                            urn));
        nonFollowEvent = EntityStateChangedEvent.fromEntityCreated(urn);

        testSubscriber = new UpdateStreamEntitySubscriber(mockAdapter);
    }

    @Test
    public void propagateFollowingEventToAdapter() throws Exception {
        testSubscriber.onNext(followEvent);

        verify(mockAdapter).onFollowingEntityChange(followEvent);
    }

    @Test
    public void doesNotPropagateOtherEventToAdapter() throws Exception {
        testSubscriber.onNext(nonFollowEvent);

        verify(mockAdapter, never()).onFollowingEntityChange(any(EntityStateChangedEvent.class));
    }
}
