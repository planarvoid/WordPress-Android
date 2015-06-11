package com.soundcloud.android.view;

import static com.soundcloud.android.Expect.expect;

import com.soundcloud.android.R;
import com.soundcloud.android.robolectric.SoundCloudTestRunner;
import com.xtremelabs.robolectric.Robolectric;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import android.view.LayoutInflater;
import android.view.View;

@RunWith(SoundCloudTestRunner.class)
public class ErrorViewTest {

    ErrorView errorView;

    @Before
    public void before(){
        errorView = (ErrorView) LayoutInflater.from(Robolectric.application).inflate(R.layout.error_view, null);
    }

    @Test
    public void shouldDefaultToNoMessage(){
        expect(errorView.findViewById(R.id.server_error).getVisibility()).toEqual(View.GONE);
        expect(errorView.findViewById(R.id.connection_error_1).getVisibility()).toEqual(View.GONE);
        expect(errorView.findViewById(R.id.connection_error_2).getVisibility()).toEqual(View.GONE);
    }

    @Test
    public void shouldSetServerErrorState(){
        errorView.setServerErrorState();
        expect(errorView.findViewById(R.id.server_error).getVisibility()).toEqual(View.VISIBLE);
        expect(errorView.findViewById(R.id.connection_error_1).getVisibility()).toEqual(View.GONE);
        expect(errorView.findViewById(R.id.connection_error_2).getVisibility()).toEqual(View.GONE);
    }

    @Test
    public void shouldSetClientErrorState() {
        errorView.setConnectionErrorState();
        expect(errorView.findViewById(R.id.server_error).getVisibility()).toEqual(View.GONE);
        expect(errorView.findViewById(R.id.connection_error_1).getVisibility()).toEqual(View.VISIBLE);
        expect(errorView.findViewById(R.id.connection_error_2).getVisibility()).toEqual(View.VISIBLE);
    }

    @Test
    public void shouldHideRetryButtonIfNoListener() {
        errorView.setOnRetryListener(null);
        expect(errorView.findViewById(R.id.btn_retry).getVisibility()).toEqual(View.GONE);
    }

    @Test
    public void shouldShowRetryButtonIfListener() {
        errorView.setOnRetryListener(new EmptyView.RetryListener() {
            @Override
            public void onEmptyViewRetry() {
            }
        });
        expect(errorView.findViewById(R.id.btn_retry).getVisibility()).toEqual(View.VISIBLE);
    }

}
