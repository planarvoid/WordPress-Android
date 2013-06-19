package com.soundcloud.android.api.http;

import static com.soundcloud.android.Expect.expect;
import static com.soundcloud.android.api.http.SoundCloudRxHttpClient.WrapperFactory;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.initMocks;

import com.google.common.net.MediaType;
import com.soundcloud.android.accounts.AccountOperations;
import com.soundcloud.android.robolectric.SoundCloudTestRunner;
import com.soundcloud.api.ApiWrapper;
import com.xtremelabs.robolectric.Robolectric;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;

@RunWith(SoundCloudTestRunner.class)
public class WrapperFactoryTest {

    private WrapperFactory wrapperFactory;
    @Mock
    private HttpProperties httpProperties;
    @Mock
    private AccountOperations accountOperations;
    @Mock
    private APIRequest apiRequest;

    @Before
    public void setUp() {
        initMocks(this);
        wrapperFactory = new WrapperFactory(Robolectric.application, httpProperties, accountOperations);
    }

    @Test
    public void shouldSetContentTypeIfRequestIsForPrivateAPI() {
        when(apiRequest.isPrivate()).thenReturn(true);
        when(apiRequest.getVersion()).thenReturn(22);
        ApiWrapper wrapper = wrapperFactory.createWrapper(apiRequest);
        expect(wrapper.getDefaultContentType()).toEqual("application/vnd.com.soundcloud.mobile.v22+json");
    }

    @Test
    public void shouldHaveDefaultContentTypeIfRequestIsForPublicAPI() {
        when(apiRequest.isPrivate()).thenReturn(false);
        ApiWrapper wrapper = wrapperFactory.createWrapper(apiRequest);
        expect(wrapper.getDefaultContentType()).toEqual(MediaType.JSON_UTF_8.toString());
    }

    @Test
    public void shouldAcceptGZipEncodedTypeForPrivateAPI() {
        when(apiRequest.isPrivate()).thenReturn(true);
        ApiWrapper wrapper = wrapperFactory.createWrapper(apiRequest);
        expect(wrapper.getDefaultAcceptEncoding()).toEqual("gzip");
    }

    @Test
    public void shouldAcceptGZipEncodedTypeForPublicAPI() {
        when(apiRequest.isPrivate()).thenReturn(true);
        ApiWrapper wrapper = wrapperFactory.createWrapper(apiRequest);
        expect(wrapper.getDefaultAcceptEncoding()).toEqual("gzip");
    }


}
