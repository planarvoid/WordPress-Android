package com.soundcloud.android.api;

import static com.soundcloud.android.Expect.expect;
import static com.soundcloud.android.api.WebServices.APIRequestException;
import static com.soundcloud.android.api.WebServices.APIRequestException.APIErrorReason;
import static org.junit.Assert.fail;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.initMocks;

import com.soundcloud.android.AndroidCloudAPI;
import com.soundcloud.api.CloudAPI;
import com.soundcloud.api.Request;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.StatusLine;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;

import java.io.IOException;

public class WebServicesTest {
    private WebServices webServices;
    @Mock
    private AndroidCloudAPI androidCloudAPI;
    @Mock
    private HttpResponse httpResponse;
    @Mock
    private StatusLine statusLine;

    @Before
    public void setUp(){
        initMocks(this);
        webServices = new WebServices(androidCloudAPI);
    }

    @Test
    public void shouldThrowAPIRequestExceptionIfCloudAPIThrowsIOException() throws IOException {
        when(androidCloudAPI.get(any(Request.class))).thenThrow(IOException.class);
        checkThrownExceptionForReason(APIErrorReason.IO_ERROR);
    }

    @Test
    public void shouldThrowAPIRequestExceptionIfCloudAPIThrowsTokenException() throws IOException {
        when(androidCloudAPI.get(any(Request.class))).thenThrow(CloudAPI.InvalidTokenException.class);
        checkThrownExceptionForReason(APIErrorReason.TOKEN_AUTH_ERROR);
    }

    @Test
    public void shouldThrowAPIRequestExceptionIfCloudAPIThrowsUnexpectedException() throws IOException {
        when(androidCloudAPI.get(any(Request.class))).thenThrow(ArrayIndexOutOfBoundsException.class);
        checkThrownExceptionForReason(APIErrorReason.UNKNOWN_ERROR);
    }

    @Test
    public void shouldThrowAPIRequestExceptionIfResponseIsNotSuccess() throws IOException {
        when(androidCloudAPI.get(any(Request.class))).thenReturn(httpResponse);
        when(httpResponse.getStatusLine()).thenReturn(statusLine);
        when(statusLine.getStatusCode()).thenReturn(HttpStatus.SC_BAD_REQUEST);
        checkThrownExceptionForReason(APIErrorReason.BAD_RESPONSE);
    }

    @Test
    public void shouldMakeRequestToSpecifiedEndpoint() throws IOException {
        ArgumentCaptor<Request> argumentCaptor = ArgumentCaptor.forClass(Request.class);
        when(androidCloudAPI.get(argumentCaptor.capture())).thenReturn(httpResponse);
        when(httpResponse.getStatusLine()).thenReturn(statusLine);
        when(statusLine.getStatusCode()).thenReturn(HttpStatus.SC_OK);

        webServices.get(WebServiceEndPoint.SUGGESTED_CATEGORIES);
        Request request = argumentCaptor.getValue();
        expect(request.toUrl()).toBe(WebServiceEndPoint.SUGGESTED_CATEGORIES.path());
    }

    @Test
    public void shouldReturnResponseInstanceWhichWrapsSuccessfulAPIResponse() throws IOException {
        when(androidCloudAPI.get(any(Request.class))).thenReturn(httpResponse);
        when(httpResponse.getStatusLine()).thenReturn(statusLine);
        when(statusLine.getStatusCode()).thenReturn(HttpStatus.SC_OK);

        APIResponse response = webServices.get(WebServiceEndPoint.SUGGESTED_CATEGORIES);
        expect(response.getWrappedResponse()).toBe(httpResponse);
    }


    private void checkThrownExceptionForReason(APIErrorReason reason) {
        try{
            webServices.get(WebServiceEndPoint.SUGGESTED_CATEGORIES);
            fail("Y U NO THROW EXCEPTION?");
        } catch(APIRequestException e){
            expect(e.reason()).toBe(reason);
        }
    }
}
