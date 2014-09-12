package com.soundcloud.android.view.adapters;

import static com.soundcloud.android.Expect.expect;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.soundcloud.android.api.legacy.model.PublicApiResource;
import com.soundcloud.android.api.legacy.model.PublicApiTrack;
import com.soundcloud.android.api.legacy.model.PublicApiUser;
import com.soundcloud.android.associations.FollowingOperations;
import com.soundcloud.android.users.UserProperty;
import com.soundcloud.android.robolectric.SoundCloudTestRunner;
import com.soundcloud.android.testsupport.TestHelper;
import com.soundcloud.propeller.PropertySet;
import com.tobedevoured.modelcitizen.CreateModelException;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;

import android.view.View;

import java.util.Arrays;
import java.util.List;

@RunWith(SoundCloudTestRunner.class)
public class PropertySetSourceProxyPresenterTest {
    @Mock private PublicApiTrack track;
    @Mock private View itemView;

    @Mock private TrackItemPresenter trackItemPresenter;
    @Mock private FollowingOperations followingOperations;

    @InjectMocks private PropertySetSourceProxyPresenter presenter;

    @Captor ArgumentCaptor<List<PropertySet>> captor;

    @Test
    public void shouldConvertTracksToPropertySets() {
        presenter.bindItemView(0, itemView, Arrays.asList((PublicApiResource) track));

        verify(trackItemPresenter).bindItemView(eq(0), eq(itemView), captor.capture());
        expect(captor.getValue().size()).toEqual(1);
        verify(track).toPropertySet();
    }

    @Test
    public void shouldPutIsFollowerPropertyOnUserItems() throws CreateModelException {
        PublicApiUser user = TestHelper.getModelFactory().createModel(PublicApiUser.class);
        when(followingOperations.isFollowing(user.getUrn())).thenReturn(true);

        presenter.bindItemView(0, itemView, Arrays.asList((PublicApiResource) user));

        verify(trackItemPresenter).bindItemView(eq(0), eq(itemView), captor.capture());
        PropertySet propertySet = captor.getValue().get(0);
        expect(propertySet.get(UserProperty.IS_FOLLOWED_BY_ME)).toEqual(true);
    }

    @Test (expected = IllegalArgumentException.class)
    public void shouldSendIllegalArgumentExceptionWhenResourceDoNotImplementPropertySetSource() {
        presenter.bindItemView(0, itemView, Arrays.asList(mock(PublicApiResource.class)));
    }
}