package com.soundcloud.android.api;

import static com.soundcloud.android.api.WebServices.APIRequestException;
import static org.mockito.Matchers.isA;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.initMocks;

import com.google.common.collect.Lists;
import com.soundcloud.android.model.CategoryGroup;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import rx.Observer;

import java.util.ArrayList;
import java.util.Collections;

public class SuggestedUsersOperationsTest {

    private SuggestedUsersOperations suggestedUsersOperations;
    @Mock
    private WebServices webServices;
    @Mock
    private APIResponse apiResponse;
    @Mock
    private CategoryGroup categoryGroupOne;
    @Mock
    private CategoryGroup categoryGroupTwo;
    @Mock
    private Observer<CategoryGroup> observer;

    @Before
    public void setUp(){
        initMocks(this);
        suggestedUsersOperations = new SuggestedUsersOperations(webServices);
    }

    @Test
    public void shouldPassEachCategoryReturnedToTheObserver(){
        when(webServices.get(WebServiceEndPoint.SUGGESTED_CATEGORIES)).thenReturn(apiResponse);
        ArrayList<CategoryGroup> categories = Lists.newArrayList(categoryGroupOne, categoryGroupTwo);
        when(apiResponse.<CategoryGroup>getCollection()).thenReturn(categories);
        suggestedUsersOperations.getCategories().subscribe(observer);
        verify(observer).onNext(categoryGroupOne);
        verify(observer).onNext(categoryGroupTwo);
        verify(observer).onCompleted();
    }

    @Test
    public void shouldCallOnErrorIfNoCategoriesHaveBeenReturned(){
        when(webServices.get(WebServiceEndPoint.SUGGESTED_CATEGORIES)).thenReturn(apiResponse);
        when(apiResponse.<CategoryGroup>getCollection()).thenReturn(Collections.<CategoryGroup>emptyList());
        suggestedUsersOperations.getCategories().subscribe(observer);
        verify(observer).onError(isA(APIRequestException.class));
        verify(observer, never()).onCompleted();
    }

}
