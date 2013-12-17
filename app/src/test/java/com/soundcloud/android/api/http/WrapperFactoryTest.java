package com.soundcloud.android.api.http;

import static com.soundcloud.android.Expect.expect;
import static com.soundcloud.android.api.http.SoundCloudRxHttpClient.WrapperFactory;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.initMocks;

import com.soundcloud.android.accounts.AccountOperations;
import com.soundcloud.android.properties.ApplicationProperties;
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
    @Mock
    private ApplicationProperties applicationProperties;

    @Before
    public void setUp() {
        initMocks(this);
        wrapperFactory = new WrapperFactory(Robolectric.application, httpProperties, accountOperations, applicationProperties);
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
        // do not use MediaType.JSON_UTF8; the public API does not accept qualified media types that include charsets
        expect(wrapper.getDefaultContentType()).toEqual("application/json");
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
