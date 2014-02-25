package com.soundcloud.android.playback.streaming;

import static com.soundcloud.android.Expect.expect;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.mockito.MockitoAnnotations.initMocks;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import rx.Observer;
import rx.schedulers.Schedulers;

public class UMGCacheBusterTest {

    public static final String URL = "url";
    private UMGCacheBuster umgCacheBuster;
    @Mock
    private StreamStorage streamStorage;
    @Mock
    private Observer<Object> observer;

    @Before
    public void setUp(){
        initMocks(this);
        umgCacheBuster = new UMGCacheBuster(Schedulers.currentThread(), observer, streamStorage);
    }

    @Test
    public void shouldNotDoAnythingIfNoLastUrlExists(){
        expect(umgCacheBuster.bustIt(URL)).toBeFalse();
        verifyZeroInteractions(streamStorage, observer);
    }

    @Test
    public void shouldNotDoAnythingIfComparisonUrlIsSameAsPreviousUrl(){
        expect(umgCacheBuster.bustIt(URL)).toBeFalse();
        expect(umgCacheBuster.bustIt(URL)).toBeFalse();
        verifyZeroInteractions(streamStorage, observer);
    }

    @Test
    public void shouldClearCacheIfLastUrlIfDifferentFromComparisonUrl(){
        expect(umgCacheBuster.bustIt(URL)).toBeFalse();
        expect(umgCacheBuster.bustIt("diffUrl")).toBeTrue();
        verify(streamStorage).removeAllDataForItem(URL);
    }

    @Test
    public void shouldClearCacheIfLastUrlIfDifferentFromComparisonUrlMultipleTimes(){
        expect(umgCacheBuster.bustIt(URL)).toBeFalse();
        expect(umgCacheBuster.bustIt("diffUrl")).toBeTrue();
        expect(umgCacheBuster.bustIt("diffUrl")).toBeFalse();
        expect(umgCacheBuster.bustIt("diffUrl2")).toBeTrue();
        verify(streamStorage).removeAllDataForItem(URL);
    }

    @Test(expected = IllegalArgumentException.class)
    public void shouldNotAcceptEmptyComparisonUrl(){
        umgCacheBuster.bustIt("  ");
    }

    @Test(expected = IllegalArgumentException.class)
    public void shouldNotAcceptNullComparisonUrl(){
        umgCacheBuster.bustIt(null);
    }

    @Test
    public void shouldReturnLastComparisonUrlAfterMultipleInvocations(){
        umgCacheBuster.bustIt(URL);
        umgCacheBuster.bustIt("diffUrl2");
        expect(umgCacheBuster.getLastUrl()).toEqual("diffUrl2");
    }

    @Test
    public void shouldReturnLastComparisonUrl(){
        umgCacheBuster.bustIt(URL);
        expect(umgCacheBuster.getLastUrl()).toEqual(URL);
    }

}
