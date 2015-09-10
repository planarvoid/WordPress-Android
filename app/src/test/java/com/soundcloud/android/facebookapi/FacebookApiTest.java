package com.soundcloud.android.facebookapi;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.soundcloud.android.testsupport.AndroidUnitTest;
import org.json.JSONException;
import org.json.JSONObject;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import rx.observers.TestSubscriber;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class FacebookApiTest extends AndroidUnitTest {

    private FacebookApi facebookApi;
    private TestSubscriber<List<String>> picturesSubscriber;

    @Mock private FacebookApiHelper facebookApiHelper;

    @Before
    public void setUp() throws Exception {
        facebookApi = new FacebookApi(facebookApiHelper);
        picturesSubscriber = new TestSubscriber<>();
        when(facebookApiHelper.hasAccessToken()).thenReturn(true);
    }

    @Test
    public void shouldParseFriendsResponse() throws Exception {
        when(facebookApiHelper.graphRequest(FacebookApiEndpoints.ME_FRIEND_PICTURES)).thenReturn(validGraphResponse());

        facebookApi.friendPictureUrls().subscribe(picturesSubscriber);
        picturesSubscriber.assertReceivedOnNext(Collections.singletonList(
                Arrays.asList("url1", "url3", "url4")));
    }

    @Test
    public void shouldHandleInvalidJsonGracefully() throws Exception {
        when(facebookApiHelper.graphRequest(FacebookApiEndpoints.ME_FRIEND_PICTURES)).thenReturn(unexpectedGraphResponse());
        facebookApi.friendPictureUrls().subscribe(picturesSubscriber);

        expectEmptyPictureUrlsList();
    }

    @Test
    public void shouldHandleSessionsWithoutFacebookAccessToken() throws Exception {
        when(facebookApiHelper.hasAccessToken()).thenReturn(false);
        facebookApi.friendPictureUrls().subscribe(picturesSubscriber);

        expectEmptyPictureUrlsList();

        verify(facebookApiHelper, never()).graphRequest(FacebookApiEndpoints.ME_FRIEND_PICTURES);
    }

    private void expectEmptyPictureUrlsList() {
        assertThat(picturesSubscriber.getOnNextEvents().get(0)).isEmpty();
    }

    @Test
    public void shouldHandleResponseErrorsGracefully() throws Exception {
        when(facebookApiHelper.graphRequest(FacebookApiEndpoints.ME_FRIEND_PICTURES)).thenReturn(errorGraphResponse());
        facebookApi.friendPictureUrls().subscribe(picturesSubscriber);

        expectEmptyPictureUrlsList();
    }

    private FacebookApiResponse validGraphResponse() throws JSONException {
        JSONObject response = new JSONObject(
                "{\"data\":[" +
                        "{\"picture\":{\"data\":{\"url\":\"url1\",\"is_silhouette\":false}}}, " +
                        "{\"picture\":{\"data\":{\"url\":\"url2\",\"is_silhouette\":true}}}, " +
                        "{\"picture\":{\"data\":{\"url\":\"url3\"}}}, " +
                        "{\"picture\":{\"data\":{\"url\":\"url4\",\"is_silhouette\":false}}}" +
                        "]}");

        return new FacebookApiResponse(response);
    }

    private FacebookApiResponse unexpectedGraphResponse() throws JSONException {
        JSONObject response = new JSONObject(
                "{\"data\":[" +
                        "{\"photo\":{\"data\":{\"url\":\"url1\",\"is_silhouette\":false}}}, " +
                        "{\"picture\":{\"data\":{\"url\":\"url2\",\"is_silhouette\":false}}}" +
                        "]}");

        return new FacebookApiResponse(response);
    }

    private FacebookApiResponse errorGraphResponse() {
        return new FacebookApiResponse(true);
    }

}
