package com.soundcloud.android.facebookinvites;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.soundcloud.android.R;
import com.soundcloud.android.facebookapi.FacebookApi;
import com.soundcloud.android.image.ImageOperations;
import com.soundcloud.android.stream.StreamItem;
import com.soundcloud.android.testsupport.AndroidUnitTest;
import com.soundcloud.android.view.CircularBorderImageView;
import org.assertj.core.util.Lists;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import rx.Observable;

import android.view.LayoutInflater;
import android.view.View;
import android.widget.FrameLayout;

import java.util.Collections;

public class FacebookListenerInvitesItemRendererTest extends AndroidUnitTest {

    private FacebookListenerInvitesItemRenderer renderer;

    @Mock private LayoutInflater inflater;
    @Mock private FacebookApi facebookApi;
    @Mock private ImageOperations imageOperations;
    @Mock private FacebookInvitesStorage invitesStorage;

    private View itemView;

    @Before
    public void setup() {
        renderer = new FacebookListenerInvitesItemRenderer(imageOperations, invitesStorage, facebookApi);
        itemView = LayoutInflater.from(context())
                                 .inflate(R.layout.facebook_invites_notification_card,
                                          new FrameLayout(context()),
                                          false);
    }

    @Test
    public void shouldSetFriendPictures() {
        StreamItem invitesItem = StreamItem.forFacebookListenerInvites();
        when(facebookApi.friendPictureUrls()).thenReturn(Observable.just(Collections.singletonList("url1")));
        renderer.bindItemView(0, itemView, Lists.newArrayList(invitesItem));

        assertThat(getFriendsLayoutVisibility()).isEqualTo(View.VISIBLE);
        assertThat(getIntroductionTextVisibility()).isEqualTo(View.GONE);
        verify(imageOperations).displayCircular("url1", getFirstFriendImageView());
    }

    @Test
    public void shouldHideFriendPictures() {
        StreamItem invitesItem = StreamItem.forFacebookListenerInvites();
        when(facebookApi.friendPictureUrls()).thenReturn(Observable.just(Collections.<String>emptyList()));
        renderer.bindItemView(0, itemView, Collections.singletonList(invitesItem));

        assertThat(getFriendsLayoutVisibility()).isEqualTo(View.GONE);
        assertThat(getIntroductionTextVisibility()).isEqualTo(View.VISIBLE);
    }

    private int getFriendsLayoutVisibility() {
        return itemView.findViewById(R.id.friends).getVisibility();
    }

    private int getIntroductionTextVisibility() {
        return itemView.findViewById(R.id.facebook_invite_introduction_text).getVisibility();
    }

    private CircularBorderImageView getFirstFriendImageView() {
        return (CircularBorderImageView) itemView.findViewById(R.id.friend_1);
    }

}
