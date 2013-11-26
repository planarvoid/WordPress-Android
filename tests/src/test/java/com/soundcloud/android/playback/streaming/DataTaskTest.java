package com.soundcloud.android.playback.streaming;

import static com.soundcloud.android.Expect.expect;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.soundcloud.android.api.PublicApi;
import com.soundcloud.android.robolectric.SoundCloudTestRunner;
import org.apache.http.HttpStatus;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;

import java.io.IOException;
import java.net.URL;
import java.nio.ByteBuffer;

@RunWith(SoundCloudTestRunner.class)
public class DataTaskTest {

    @Mock
    StreamItem streamItem;
    @Mock
    PublicApi publicApi;

    @Before
    public void setUp() throws Exception {
        when(streamItem.redirectUrl()).thenReturn(new URL("http", "domain.com", "file"));
    }

    @Test
    public void shoudlReturnSuccessBundleOnOkCode() throws Exception {
        expect(createDataTask(HttpStatus.SC_OK).execute().getBoolean(DataTask.SUCCESS_KEY)).toBeTrue();
    }

    @Test
    public void shoudlReturnSuccessBundleOnPartialContentCode() throws Exception {
        expect(createDataTask(HttpStatus.SC_PARTIAL_CONTENT).execute().getBoolean(DataTask.SUCCESS_KEY)).toBeTrue();
    }

    @Test
    public void shoudlInvalidateRedirectUrlAndSetHttpErrorOnStreamItemOnBadRequest() throws Exception {
        createDataTask(HttpStatus.SC_BAD_REQUEST).execute();
        verify(streamItem).invalidateRedirectUrl();
        verify(streamItem).setHttpError(HttpStatus.SC_BAD_REQUEST);
    }

    @Test
    public void shoudlInvalidateRedirectUrlAndSetHttpErrorOnStreamItemOnForbidden() throws Exception {
        createDataTask(HttpStatus.SC_FORBIDDEN).execute();
        verify(streamItem).invalidateRedirectUrl();
        verify(streamItem).setHttpError(HttpStatus.SC_FORBIDDEN);
    }

    @Test
    public void shoudlInvalidateRedirectUrlAndSetHttpErrorOnStreamItemOnNotFound() throws Exception {
        createDataTask(HttpStatus.SC_NOT_FOUND).execute();
        verify(streamItem).invalidateRedirectUrl();
        verify(streamItem).setHttpError(HttpStatus.SC_NOT_FOUND);
    }

    @Test(expected = IOException.class)
    public void shoudlThrowIOExceptionOnServerErrorCode() throws Exception {
        createDataTask(HttpStatus.SC_INTERNAL_SERVER_ERROR).execute();
    }

    private DataTask createDataTask(final int statusCode){
        return new DataTask(streamItem, new Range(0, 10), new Range(0, 10240), publicApi) {
            @Override
            protected int getData(URL url, int start, int end, ByteBuffer dst) throws IOException {
                return statusCode;
            }
        };
    }
}
