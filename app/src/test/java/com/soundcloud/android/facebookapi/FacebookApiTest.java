package com.soundcloud.android.facebookapi;

import static org.assertj.core.api.Java6Assertions.assertThat;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.soundcloud.android.testsupport.AndroidUnitTest;
import io.reactivex.observers.TestObserver;
import io.reactivex.schedulers.Schedulers;
import org.json.JSONException;
import org.json.JSONObject;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;

import java.util.Arrays;
import java.util.List;

public class FacebookApiTest extends AndroidUnitTest {

    private FacebookApi facebookApi;

    @Mock private FacebookApiHelper facebookApiHelper;

    @Before
    public void setUp() throws Exception {
        facebookApi = new FacebookApi(facebookApiHelper, Schedulers.trampoline());
        when(facebookApiHelper.hasAccessToken()).thenReturn(true);
    }

    @Test
    public void shouldParseFriendsResponse() throws Exception {
        when(facebookApiHelper.graphRequest(FacebookApiEndpoints.ME_FRIEND_PICTURES)).thenReturn(validGraphResponse());

        final TestObserver<List<String>> picturesSubscriber = facebookApi.friendPictureUrls().test();
        picturesSubscriber.assertValue(Arrays.asList("url1", "url3", "url4"));
    }

    @Test
    public void shouldHandleInvalidJsonGracefully() throws Exception {
        when(facebookApiHelper.graphRequest(FacebookApiEndpoints.ME_FRIEND_PICTURES)).thenReturn(unexpectedGraphResponse());
        final TestObserver<List<String>> picturesSubscriber = facebookApi.friendPictureUrls().test();

        expectEmptyPictureUrlsList(picturesSubscriber);
    }

    @Test
    public void shouldHandleSessionsWithoutFacebookAccessToken() throws Exception {
        when(facebookApiHelper.hasAccessToken()).thenReturn(false);
        final TestObserver<List<String>> picturesSubscriber = facebookApi.friendPictureUrls().test();

        expectEmptyPictureUrlsList(picturesSubscriber);

        verify(facebookApiHelper, never()).graphRequest(FacebookApiEndpoints.ME_FRIEND_PICTURES);
    }

    private void expectEmptyPictureUrlsList(TestObserver<List<String>> picturesSubscriber) {
        assertThat(picturesSubscriber.values().get(0)).isEmpty();
    }

    @Test
    public void shouldHandleResponseErrorsGracefully() throws Exception {
        when(facebookApiHelper.graphRequest(FacebookApiEndpoints.ME_FRIEND_PICTURES)).thenReturn(errorGraphResponse());
        final TestObserver<List<String>> picturesSubscriber = facebookApi.friendPictureUrls().test();

        expectEmptyPictureUrlsList(picturesSubscriber);
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
