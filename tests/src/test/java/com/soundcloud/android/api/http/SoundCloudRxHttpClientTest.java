package com.soundcloud.android.api.http;

import static com.google.common.collect.Lists.newArrayList;
import static com.soundcloud.android.Expect.expect;
import static com.soundcloud.android.api.http.SoundCloudRxHttpClient.WrapperFactory;
import static com.soundcloud.api.CloudAPI.InvalidTokenException;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.initMocks;
import static rx.android.ErrorRaisingObserver.errorRaisingObserver;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Sets;
import com.google.common.reflect.TypeToken;
import com.soundcloud.android.api.http.json.JsonTransformer;
import com.soundcloud.android.model.CollectionHolder;
import com.soundcloud.android.model.UnknownResource;
import com.soundcloud.android.model.User;
import com.soundcloud.android.robolectric.SoundCloudTestRunner;
import com.soundcloud.api.Request;
import org.apache.http.Header;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.StatusLine;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.util.EntityUtils;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import rx.Observable;
import rx.Observer;
import rx.concurrency.Schedulers;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.List;
import java.util.Set;

@RunWith(SoundCloudTestRunner.class)
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
        when(wrapperFactory.createWrapper(any(APIRequest.class))).thenReturn(wrapper);
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
        rxHttpClient.fetchModels(apiRequest).subscribe(observer);
        verify(observer).onError(any(APIRequestException.class));
    }


    @Test
    public void shouldMakeGetRequestWithAPIWrapper() throws IOException {
        when(apiRequest.getMethod()).thenReturn("get");
        rxHttpClient.fetchModels(apiRequest).subscribe(errorRaisingObserver());
        verify(wrapper).get(any(Request.class));
    }

    @Test
    public void shouldMakePostRequestWithAPIWrapper() throws IOException {
        when(apiRequest.getMethod()).thenReturn("post");
        when(wrapper.post(any(Request.class))).thenReturn(httpResponse);
        rxHttpClient.fetchModels(apiRequest).subscribe(errorRaisingObserver());
        verify(wrapper).post(any(Request.class));
    }

    @Test
    public void shouldMakeGetRequestWithSpecifiedURI() throws IOException {
        rxHttpClient.fetchModels(apiRequest).subscribe(errorRaisingObserver());
        ArgumentCaptor<Request> argumentCaptor = ArgumentCaptor.forClass(Request.class);
        verify(wrapper).get(argumentCaptor.capture());
        Request scRequest = argumentCaptor.getValue();
        expect(scRequest.toUrl()).toEqual(URI);
    }

    @Test
    public void shouldMakePostRequestWithSpecifiedURI() throws IOException {
        when(apiRequest.getMethod()).thenReturn("post");
        when(wrapper.post(any(Request.class))).thenReturn(httpResponse);
        rxHttpClient.fetchModels(apiRequest).subscribe(errorRaisingObserver());
        ArgumentCaptor<Request> argumentCaptor = ArgumentCaptor.forClass(Request.class);
        verify(wrapper).post(argumentCaptor.capture());
        Request scRequest = argumentCaptor.getValue();
        expect(scRequest.toUrl()).toEqual(URI);
    }

    @Test
    public void shouldRaiseAPIExceptionIfInvalidTokenExists() throws IOException {
        when(wrapper.get(any(Request.class))).thenThrow(InvalidTokenException.class);
        rxHttpClient.fetchModels(apiRequest).subscribe(observer);
        verify(observer).onError(any(APIRequestException.class));
    }

    @Test
    public void shouldRaiseAPIExceptionIfIOExceptionOccurs() throws IOException {
        when(wrapper.get(any(Request.class))).thenThrow(IOException.class);
        rxHttpClient.fetchModels(apiRequest).subscribe(observer);
        verify(observer).onError(any(APIRequestException.class));
    }

    @Test
    public void shouldRaiseAPIExceptionIfResponseStatusCodeIs400() throws IOException {
        when(statusLine.getStatusCode()).thenReturn(400);
        rxHttpClient.fetchModels(apiRequest).subscribe(observer);
        verify(observer).onError(any(APIRequestException.class));
    }

    @Test
    public void shouldRaiseAPIExceptionIfResponseStatusCodeIs199() throws IOException {
        when(statusLine.getStatusCode()).thenReturn(199);
        rxHttpClient.fetchModels(apiRequest).subscribe(observer);
        verify(observer).onError(any(APIRequestException.class));
    }

    @Test
    public void shouldParseJsonResponseWithSpecifiedTypeInRequest() throws Exception {
        when(apiRequest.getResourceType()).thenReturn(TypeToken.of(User.class));
        when(httpEntity.getContent()).thenReturn(new ByteArrayInputStream(STREAM_DATA.getBytes()));
        rxHttpClient.fetchModels(apiRequest).subscribe(errorRaisingObserver());
        verify(jsonTransformer).fromJson(STREAM_DATA, TypeToken.of(User.class));
    }

    @Test
    public void shouldThrowBadResponseExceptionIfParsingJsonResponseThrowsException() throws Exception {
        when(apiRequest.getResourceType()).thenReturn(TypeToken.of(User.class));
        when(httpEntity.getContent()).thenReturn(new ByteArrayInputStream(STREAM_DATA.getBytes()));
        when(jsonTransformer.fromJson(STREAM_DATA, TypeToken.of(User.class))).thenThrow(Exception.class);
        rxHttpClient.fetchModels(apiRequest).subscribe(observer);
        verify(observer).onError(any(APIRequestException.class));
    }

    @Test
     public void shouldThrowBadResponseExceptionIfParsingJsonResponseReturnsNull() throws Exception {
        when(apiRequest.getResourceType()).thenReturn(TypeToken.of(User.class));
        when(httpEntity.getContent()).thenReturn(new ByteArrayInputStream(STREAM_DATA.getBytes()));
        when(jsonTransformer.fromJson(STREAM_DATA, TypeToken.of(User.class))).thenReturn(null);
        rxHttpClient.fetchModels(apiRequest).subscribe(observer);
        verify(observer).onError(any(APIRequestException.class));
    }

    @Test
    public void shouldThrowBadResponseExceptionIfParsingJsonReturnsUnknownResource() throws Exception {
        when(apiRequest.getResourceType()).thenReturn(TypeToken.of(User.class));
        when(httpEntity.getContent()).thenReturn(new ByteArrayInputStream(STREAM_DATA.getBytes()));
        when(jsonTransformer.fromJson(STREAM_DATA, TypeToken.of(User.class))).thenReturn(mock(UnknownResource.class));
        rxHttpClient.fetchModels(apiRequest).subscribe(observer);
        verify(observer).onError(any(APIRequestException.class));
    }

    @Test
    public void shouldCallOnNextOnceIfSingleModelRequested() throws Exception {
        User mock = mock(User.class);
        when(apiRequest.getResourceType()).thenReturn(TypeToken.of(User.class));
        when(httpEntity.getContent()).thenReturn(new ByteArrayInputStream(STREAM_DATA.getBytes()));
        when(jsonTransformer.fromJson(STREAM_DATA, TypeToken.of(User.class))).thenReturn(mock);
        rxHttpClient.fetchModels(apiRequest).subscribe(observer);
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
        rxHttpClient.fetchModels(apiRequest).subscribe(observer);
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
        rxHttpClient.fetchModels(apiRequest).subscribe(observer);
        verify(observer).onNext(userOne);
        verify(observer).onNext(userTwo);
        verify(observer).onCompleted();
    }

    @Test
    public void shouldNotTryToParseResponseJsonIfNoResponseBodyExists() throws IOException {
        when(httpEntity.getContent()).thenReturn(new ByteArrayInputStream("".getBytes()));
        rxHttpClient.fetchModels(apiRequest).subscribe(observer);
        verifyZeroInteractions(jsonTransformer);
        verify(observer, never()).onNext(any());
        verify(observer).onCompleted();
    }

    @Test
    public void shouldNotTryToDeserialiseResponseIfNoResourceTypeSpecifiedInRequest(){
        when(apiRequest.getResourceType()).thenReturn(null);
        rxHttpClient.fetchModels(apiRequest).subscribe(observer);
        verifyZeroInteractions(jsonTransformer);
        verify(observer, never()).onNext(any());
        verify(observer, never()).onError((Exception)any());
        verify(observer).onCompleted();
    }

    @Test
    public void shouldThrowExceptionIfResourceTypeSpecifiedButResponseBodyIsEmpty() throws IOException {
        when(apiRequest.getResourceType()).thenReturn(TypeToken.of(User.class));
        when(httpEntity.getContent()).thenReturn(new ByteArrayInputStream("".getBytes()));
        rxHttpClient.fetchModels(apiRequest).subscribe(observer);
        verify(observer).onError(any(APIRequestException.class));
    }

    @Test
    public void shouldMakeRequestWithSingleValueQueryParameter() throws IOException {
        ArrayListMultimap map = ArrayListMultimap.create();
        map.put("key","value");
        when(apiRequest.getQueryParameters()).thenReturn(map);

        rxHttpClient.fetchModels(apiRequest).subscribe(errorRaisingObserver());
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

        rxHttpClient.fetchModels(apiRequest).subscribe(errorRaisingObserver());
        ArgumentCaptor<Request> argumentCaptor = ArgumentCaptor.forClass(Request.class);
        verify(wrapper).get(argumentCaptor.capture());
        Request request = argumentCaptor.getValue();

        expect(request.getParams().get("key")).toEqual("value1,value2");
        expect(request.getParams().get("key2")).toEqual("value3");

    }

    @Test
    public void shouldMakePostRequestWithJsonContent() throws IOException {
        final Object jsonSource = new Object();
        final String jsonContent = "{\"data\": \"I Am Json Content\"}";

        when(apiRequest.getContent()).thenReturn(jsonSource);
        when(apiRequest.getMethod()).thenReturn("post");
        when(wrapper.post(any(Request.class))).thenReturn(httpResponse);
        when(jsonTransformer.toJson(jsonSource)).thenReturn(jsonContent);

        rxHttpClient.fetchModels(apiRequest).subscribe(errorRaisingObserver());
        ArgumentCaptor<Request> argumentCaptor = ArgumentCaptor.forClass(Request.class);
        verify(wrapper).post(argumentCaptor.capture());
        Request request = argumentCaptor.getValue();

        final HttpPost httpPost = request.buildRequest(HttpPost.class);
        expect(EntityUtils.toString(httpPost.getEntity())).toEqual(jsonContent);
        expect(httpPost.getFirstHeader("Content-Type").getValue()).toEqual("application/json; charset=utf-8");
    }

    @Test
    public void shouldThrowExceptionOnJsonContentParsingError() throws IOException {
        final Object jsonSource = new Object();

        when(apiRequest.getContent()).thenReturn(jsonSource);
        when(apiRequest.getMethod()).thenReturn("post");
        when(wrapper.post(any(Request.class))).thenReturn(httpResponse);
        when(jsonTransformer.toJson(jsonSource)).thenThrow(new IOException());

        rxHttpClient.fetchModels(apiRequest).subscribe(observer);
        verify(observer).onError(any(APIRequestException.class));
    }

    @Test
    public void fetchPagedModelsShouldReturnFirstPageAndComplete() throws Exception {
        User userOne = mock(User.class);
        User userTwo = mock(User.class);
        List<User> users = newArrayList(userOne, userTwo);
        TypeToken<CollectionHolder<User>> resourceType = new TypeToken<CollectionHolder<User>>() {};
        when(apiRequest.getResourceType()).thenReturn(resourceType);
        when(httpEntity.getContent()).thenReturn(new ByteArrayInputStream(STREAM_DATA.getBytes()));
        when(jsonTransformer.fromJson(STREAM_DATA, resourceType)).thenReturn(new CollectionHolder(users));

        rxHttpClient.fetchPagedModels(apiRequest).subscribe(observer);

        ArgumentCaptor<Observable> argumentCaptor = ArgumentCaptor.forClass(Observable.class);
        verify(observer).onNext(argumentCaptor.capture());
        Observable observable = argumentCaptor.getValue();

        Observer itemObserver = mock(Observer.class);
        observable.subscribe(itemObserver);
        verify(itemObserver).onNext(userOne);
        verify(itemObserver).onNext(userTwo);
        verify(itemObserver).onCompleted();
        verify(observer).onCompleted();
    }

    @Test
    public void fetchPagedModelsShouldReturnTwoPageObservables() throws Exception {
        TypeToken<CollectionHolder<User>> resourceType = new TypeToken<CollectionHolder<User>>() {};
        when(apiRequest.getResourceType()).thenReturn(resourceType);
        when(httpEntity.getContent()).thenReturn(new ByteArrayInputStream(STREAM_DATA.getBytes()));

        final CollectionHolder collectionHolder = new CollectionHolder();
        collectionHolder.next_href = URI;
        when(jsonTransformer.fromJson(STREAM_DATA, resourceType)).thenReturn(collectionHolder);

        rxHttpClient.fetchPagedModels(apiRequest).subscribe(observer);

        ArgumentCaptor<Observable> argumentCaptor = ArgumentCaptor.forClass(Observable.class);
        verify(observer).onNext(argumentCaptor.capture());

        Observable<Observable<User>> pageObservable = argumentCaptor.getValue();
        Observer<User> userObserver = mock(Observer.class);
        pageObservable.subscribe(userObserver);

        verify(observer, times(2)).onNext(any(Observable.class));
        verify(observer, never()).onCompleted();
    }

    @Test
    public void fetchPagedModelsShouldReturnFirstPageAndError(){
        when(statusLine.getStatusCode()).thenReturn(400);
        rxHttpClient.fetchPagedModels(apiRequest).subscribe(observer);

        ArgumentCaptor<Observable> argumentCaptor = ArgumentCaptor.forClass(Observable.class);
        verify(observer).onNext(argumentCaptor.capture());

        Observable<Observable<User>> pageObservable = argumentCaptor.getValue();
        Observer<User> userObserver = mock(Observer.class);
        pageObservable.subscribe(userObserver);

        verify(userObserver).onError(any(APIRequestException.class));
        verify(observer).onError(any(APIRequestException.class));

    }
}
