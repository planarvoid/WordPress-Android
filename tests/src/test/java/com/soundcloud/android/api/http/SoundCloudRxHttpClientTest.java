package com.soundcloud.android.api.http;

import static com.google.common.collect.Lists.newArrayList;
import static com.soundcloud.android.Expect.expect;
import static com.soundcloud.android.api.http.SoundCloudRxHttpClient.WrapperFactory;
import static com.soundcloud.android.rx.android.ErrorRaisingObserver.errorRaisingObserver;
import static com.soundcloud.api.CloudAPI.InvalidTokenException;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.initMocks;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Sets;
import com.google.common.reflect.TypeToken;
import com.soundcloud.android.api.http.json.JsonTransformer;
import com.soundcloud.android.model.UnknownResource;
import com.soundcloud.android.model.User;
import com.soundcloud.api.Request;
import org.apache.http.Header;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.StatusLine;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import rx.Observer;
import rx.concurrency.Schedulers;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.List;
import java.util.Set;

public class SoundCloudRxHttpClientTest {
    private static final String URI = "/uri";
    public static final String STREAM_DATA = "stream";
    private SoundCloudRxHttpClient rxHttpClient;
    @Mock
    private JsonTransformer jsonTransformer;
    @Mock
    private WrapperFactory wrapperFactory;
    @Mock
    private Wrapper wrapper;
    @Mock
    private APIRequest apiRequest;
    @Mock
    private HttpResponse httpResponse;
    @Mock
    private HttpEntity httpEntity;
    @Mock
    private StatusLine statusLine;
    @Mock
    private User resource;
    @Mock
    private Observer observer;

    @Before
    public void setUp() throws Exception {
        initMocks(this);
        rxHttpClient = new SoundCloudRxHttpClient(jsonTransformer, wrapperFactory).subscribeOn(Schedulers.immediate());
        when(apiRequest.getUriPath()).thenReturn(URI);
        when(apiRequest.getMethod()).thenReturn("get");
        when(apiRequest.getQueryParameters()).thenReturn(ArrayListMultimap.create());
        when(wrapperFactory.createWrapper(apiRequest)).thenReturn(wrapper);
        when(wrapper.get(any(Request.class))).thenReturn(httpResponse);
        when(httpResponse.getEntity()).thenReturn(httpEntity);
        when(httpResponse.getStatusLine()).thenReturn(statusLine);
        when(httpResponse.getAllHeaders()).thenReturn(new Header[]{});
        when(statusLine.getStatusCode()).thenReturn(HttpStatus.SC_OK);
        when(jsonTransformer.fromJson(anyString(),any(TypeToken.class))).thenReturn(resource);
    }

    @Test
    public void shouldThrowExceptionIfResponseIsRateLimited(){
        when(statusLine.getStatusCode()).thenReturn(429);
        rxHttpClient.executeAPIRequest(apiRequest).subscribe(observer);
        verify(observer).onError(any(APIRequestException.class));
    }


    @Test
    public void shouldMakeGetRequestWithAPIWrapper() throws IOException {
        when(apiRequest.getMethod()).thenReturn("get");
        rxHttpClient.executeAPIRequest(apiRequest).subscribe(errorRaisingObserver());
        verify(wrapper).get(any(Request.class));
    }

    @Test
    public void shouldMakePostRequestWithAPIWrapper() throws IOException {
        when(apiRequest.getMethod()).thenReturn("post");
        when(wrapper.post(any(Request.class))).thenReturn(httpResponse);
        rxHttpClient.executeAPIRequest(apiRequest).subscribe(errorRaisingObserver());
        verify(wrapper).post(any(Request.class));
    }

    @Test
    public void shouldMakeGetRequestWithSpecifiedURI() throws IOException {
        rxHttpClient.executeAPIRequest(apiRequest).subscribe(errorRaisingObserver());
        ArgumentCaptor<Request> argumentCaptor = ArgumentCaptor.forClass(Request.class);
        verify(wrapper).get(argumentCaptor.capture());
        Request scRequest = argumentCaptor.getValue();
        expect(scRequest.toUrl()).toEqual(URI);
    }

    @Test
    public void shouldMakePostRequestWithSpecifiedURI() throws IOException {
        when(apiRequest.getMethod()).thenReturn("post");
        when(wrapper.post(any(Request.class))).thenReturn(httpResponse);
        rxHttpClient.executeAPIRequest(apiRequest).subscribe(errorRaisingObserver());
        ArgumentCaptor<Request> argumentCaptor = ArgumentCaptor.forClass(Request.class);
        verify(wrapper).post(argumentCaptor.capture());
        Request scRequest = argumentCaptor.getValue();
        expect(scRequest.toUrl()).toEqual(URI);
    }

    @Test
    public void shouldRaiseAPIExceptionIfInvalidTokenExists() throws IOException {
        when(wrapper.get(any(Request.class))).thenThrow(InvalidTokenException.class);
        rxHttpClient.executeAPIRequest(apiRequest).subscribe(observer);
        verify(observer).onError(any(APIRequestException.class));
    }

    @Test
    public void shouldRaiseAPIExceptionIfIOExceptionOccurs() throws IOException {
        when(wrapper.get(any(Request.class))).thenThrow(IOException.class);
        rxHttpClient.executeAPIRequest(apiRequest).subscribe(observer);
        verify(observer).onError(any(APIRequestException.class));
    }

    @Test
    public void shouldRaiseAPIExceptionIfResponseStatusCodeIs400() throws IOException {
        when(statusLine.getStatusCode()).thenReturn(400);
        rxHttpClient.executeAPIRequest(apiRequest).subscribe(observer);
        verify(observer).onError(any(APIRequestException.class));
    }

    @Test
    public void shouldRaiseAPIExceptionIfResponseStatusCodeIs199() throws IOException {
        when(statusLine.getStatusCode()).thenReturn(199);
        rxHttpClient.executeAPIRequest(apiRequest).subscribe(observer);
        verify(observer).onError(any(APIRequestException.class));
    }

    @Test
    public void shouldParseJsonResponseWithSpecifiedTypeInRequest() throws Exception {
        when(apiRequest.getResourceType()).thenReturn(TypeToken.of(User.class));
        when(httpEntity.getContent()).thenReturn(new ByteArrayInputStream(STREAM_DATA.getBytes()));
        rxHttpClient.executeAPIRequest(apiRequest).subscribe(errorRaisingObserver());
        verify(jsonTransformer).fromJson(STREAM_DATA, TypeToken.of(User.class));
    }

    @Test
    public void shouldThrowBadResponseExceptionIfParsingJsonResponseThrowsException() throws Exception {
        when(apiRequest.getResourceType()).thenReturn(TypeToken.of(User.class));
        when(httpEntity.getContent()).thenReturn(new ByteArrayInputStream(STREAM_DATA.getBytes()));
        when(jsonTransformer.fromJson(STREAM_DATA, TypeToken.of(User.class))).thenThrow(Exception.class);
        rxHttpClient.executeAPIRequest(apiRequest).subscribe(observer);
        verify(observer).onError(any(APIRequestException.class));
    }

    @Test
     public void shouldThrowBadResponseExceptionIfParsingJsonResponseReturnsNull() throws Exception {
        when(apiRequest.getResourceType()).thenReturn(TypeToken.of(User.class));
        when(httpEntity.getContent()).thenReturn(new ByteArrayInputStream(STREAM_DATA.getBytes()));
        when(jsonTransformer.fromJson(STREAM_DATA, TypeToken.of(User.class))).thenReturn(null);
        rxHttpClient.executeAPIRequest(apiRequest).subscribe(observer);
        verify(observer).onError(any(APIRequestException.class));
    }

    @Test
    public void shouldThrowBadResponseExceptionIfParsingJsonReturnsUnknownResource() throws Exception {
        when(apiRequest.getResourceType()).thenReturn(TypeToken.of(User.class));
        when(httpEntity.getContent()).thenReturn(new ByteArrayInputStream(STREAM_DATA.getBytes()));
        when(jsonTransformer.fromJson(STREAM_DATA, TypeToken.of(User.class))).thenReturn(mock(UnknownResource.class));
        rxHttpClient.executeAPIRequest(apiRequest).subscribe(observer);
        verify(observer).onError(any(APIRequestException.class));
    }

    @Test
    public void shouldCallOnNextOnceIfSingleModelRequested() throws Exception {
        User mock = mock(User.class);
        when(apiRequest.getResourceType()).thenReturn(TypeToken.of(User.class));
        when(httpEntity.getContent()).thenReturn(new ByteArrayInputStream(STREAM_DATA.getBytes()));
        when(jsonTransformer.fromJson(STREAM_DATA, TypeToken.of(User.class))).thenReturn(mock);
        rxHttpClient.executeAPIRequest(apiRequest).subscribe(observer);
        verify(observer).onNext(mock);
        verify(observer).onCompleted();
    }

    @Test
    public void shouldCallOnNextWithAllResourcesIfResourceTypeIsOfList() throws Exception {
        User userOne = mock(User.class);
        User userTwo = mock(User.class);
        List<User> users = newArrayList(userOne, userTwo);
        TypeToken<List<User>> resourceType = new TypeToken<List<User>>() {};
        when(apiRequest.getResourceType()).thenReturn(resourceType);
        when(httpEntity.getContent()).thenReturn(new ByteArrayInputStream(STREAM_DATA.getBytes()));
        when(jsonTransformer.fromJson(STREAM_DATA, resourceType)).thenReturn(users);
        rxHttpClient.executeAPIRequest(apiRequest).subscribe(observer);
        verify(observer).onNext(userOne);
        verify(observer).onNext(userTwo);
        verify(observer).onCompleted();
    }

    @Test
    public void shouldCallOnNextWithAllResourcesIfResourceTypeIsOfSet() throws Exception {
        User userOne = mock(User.class);
        User userTwo = mock(User.class);
        Set<User> users = Sets.newHashSet(userOne, userTwo);
        TypeToken<Set<User>> resourceType = new TypeToken<Set<User>>() {};
        when(apiRequest.getResourceType()).thenReturn(resourceType);
        when(httpEntity.getContent()).thenReturn(new ByteArrayInputStream(STREAM_DATA.getBytes()));
        when(jsonTransformer.fromJson(STREAM_DATA, resourceType)).thenReturn(users);
        rxHttpClient.executeAPIRequest(apiRequest).subscribe(observer);
        verify(observer).onNext(userOne);
        verify(observer).onNext(userTwo);
        verify(observer).onCompleted();
    }

    @Test
    public void shouldNotTryToParseResponseJsonIfNoResponseBodyExists() throws IOException {
        when(httpEntity.getContent()).thenReturn(new ByteArrayInputStream("".getBytes()));
        rxHttpClient.executeAPIRequest(apiRequest).subscribe(observer);
        verifyZeroInteractions(jsonTransformer);
        verify(observer, never()).onNext(any());
        verify(observer).onCompleted();
    }

    @Test
    public void shouldNotTryToDeserialiseResponseIfNoResourceTypeSpecifiedInRequest(){
        when(apiRequest.getResourceType()).thenReturn(null);
        rxHttpClient.executeAPIRequest(apiRequest).subscribe(observer);
        verifyZeroInteractions(jsonTransformer);
        verify(observer, never()).onNext(any());
        verify(observer, never()).onError((Exception)any());
        verify(observer).onCompleted();
    }

    @Test
    public void shouldThrowExceptionIfResourceTypeSpecifiedButResponseBodyIsEmpty() throws IOException {
        when(apiRequest.getResourceType()).thenReturn(TypeToken.of(User.class));
        when(httpEntity.getContent()).thenReturn(new ByteArrayInputStream("".getBytes()));
        rxHttpClient.executeAPIRequest(apiRequest).subscribe(observer);
        verify(observer).onError(any(APIRequestException.class));
    }

    @Test
    public void shouldMakeRequestWithSingleValueQueryParameter() throws IOException {
        ArrayListMultimap map = ArrayListMultimap.create();
        map.put("key","value");
        when(apiRequest.getQueryParameters()).thenReturn(map);

        rxHttpClient.executeAPIRequest(apiRequest).subscribe(errorRaisingObserver());
        ArgumentCaptor<Request> argumentCaptor = ArgumentCaptor.forClass(Request.class);
        verify(wrapper).get(argumentCaptor.capture());
        Request request = argumentCaptor.getValue();

        expect(request.getParams().get("key")).toEqual("value");

    }

    @Test
    public void shouldMakeRequestWithMultipleValueQueryParameter() throws IOException {
        ArrayListMultimap map = ArrayListMultimap.create();
        map.putAll("key", newArrayList("value1", "value2"));
        map.putAll("key2", newArrayList("value3"));
        when(apiRequest.getQueryParameters()).thenReturn(map);

        rxHttpClient.executeAPIRequest(apiRequest).subscribe(errorRaisingObserver());
        ArgumentCaptor<Request> argumentCaptor = ArgumentCaptor.forClass(Request.class);
        verify(wrapper).get(argumentCaptor.capture());
        Request request = argumentCaptor.getValue();

        expect(request.getParams().get("key")).toEqual("value1,value2");
        expect(request.getParams().get("key2")).toEqual("value3");

    }
}
