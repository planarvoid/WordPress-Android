package com.soundcloud.android.api;

import static org.mockito.MockitoAnnotations.initMocks;

import com.soundcloud.android.api.http.SoundCloudRxHttpClient;
import com.soundcloud.android.model.CategoryGroup;
import org.junit.Before;
import org.mockito.Mock;
import rx.Observer;

public class SuggestedUsersOperationsTest {

    private SuggestedUsersOperations suggestedUsersOperations;
    @Mock
    private SoundCloudRxHttpClient soundCloudRxHttpClient;
    @Mock
    private CategoryGroup categoryGroupOne;
    @Mock
    private CategoryGroup categoryGroupTwo;
    @Mock
    private Observer<CategoryGroup> observer;

    @Before
    public void setUp(){
        initMocks(this);
        suggestedUsersOperations = new SuggestedUsersOperations(soundCloudRxHttpClient);
    }



}
