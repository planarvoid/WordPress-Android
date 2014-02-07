package com.soundcloud.android.playback.streaming;

import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyInt;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.initMocks;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import rx.Observer;
import rx.schedulers.Schedulers;

import android.content.Context;
import android.content.SharedPreferences;

public class UMGCacheBusterTest {

    public static final String URL = "url";
    private UMGCacheBuster umgCacheBuster;
    @Mock
    private Context context;
    @Mock
    private SharedPreferences sharedPrefs;
    @Mock
    private StreamStorage streamStorage;
    @Mock
    private SharedPreferences.Editor editor;
    @Mock
    private Observer<Object> observer;

    @Before
    public void setUp(){
        initMocks(this);
        when(context.getSharedPreferences(anyString(),anyInt())).thenReturn(sharedPrefs);
        when(sharedPrefs.edit()).thenReturn(editor);
        umgCacheBuster = new UMGCacheBuster(Schedulers.currentThread(), observer, context, streamStorage);
    }

    @Test
    public void shouldUseLastStoredUrlIfNotPreviouslyProvided(){
        when(sharedPrefs.getString(UMGCacheBuster.LAST_TRACK_PREFERENCE, URL)).thenReturn("url2");
        umgCacheBuster.bustIt(URL);
        verify(streamStorage).removeAllDataForItem("url2");
    }

    @Test
    public void shouldNotRemoveDataIfLastStoredUrlIfSameAsOneProvided(){
        when(sharedPrefs.getString(UMGCacheBuster.LAST_TRACK_PREFERENCE, URL)).thenReturn(URL);
        umgCacheBuster.bustIt(URL);
        verifyZeroInteractions(streamStorage);
    }

    @Test
    public void shouldUseProvidedUrlIfOnePreviouslyProvided(){
        when(sharedPrefs.getString(UMGCacheBuster.LAST_TRACK_PREFERENCE, URL)).thenReturn(URL);
        umgCacheBuster.bustIt(URL);
        umgCacheBuster.bustIt("url3");
        verify(streamStorage).removeAllDataForItem(URL);
        verify(sharedPrefs, never()).getString(UMGCacheBuster.LAST_TRACK_PREFERENCE, "url3");
    }

    @Test
    public void shouldNotRemoveDataIfProvidedUrlIsSameAsOneProvided(){
        when(sharedPrefs.getString(UMGCacheBuster.LAST_TRACK_PREFERENCE, URL)).thenReturn(URL);
        umgCacheBuster.bustIt(URL);
        umgCacheBuster.bustIt(URL);
        verifyZeroInteractions(streamStorage);
        verify(sharedPrefs, times(1)).getString(UMGCacheBuster.LAST_TRACK_PREFERENCE, URL);
    }

    @Test
    public void shouldUpdateStoredLastURLWhenRemovingData(){
        when(sharedPrefs.getString(anyString(), anyString())).thenReturn(URL);
        umgCacheBuster.bustIt("url2");
        verify(editor).putString(UMGCacheBuster.LAST_TRACK_PREFERENCE, "url2");
        verify(editor).commit();
    }

    @Test
    public void shouldCallOnCompletedIfURLsAretheSame(){
        when(sharedPrefs.getString(anyString(), anyString())).thenReturn(URL);
        umgCacheBuster.bustIt(URL);
        verify(observer).onCompleted();
        verify(observer, never()).onError(any(Exception.class));
    }

    @Test
    public void shouldCallOnCompletedAfterSuccessfullyClearingData(){
        when(sharedPrefs.getString(anyString(), anyString())).thenReturn(URL);
        umgCacheBuster.bustIt("url2");
        verify(observer).onCompleted();
        verify(observer, never()).onError(any(Exception.class));
    }
}
