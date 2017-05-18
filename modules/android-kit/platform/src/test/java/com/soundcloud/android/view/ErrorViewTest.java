package com.soundcloud.android.view;

import static org.assertj.core.api.Java6Assertions.assertThat;

import com.soundcloud.android.testsupport.AndroidUnitTest;
import com.soundcloud.androidkit.R;
import org.junit.Before;
import org.junit.Test;

import android.view.LayoutInflater;
import android.view.View;

public class ErrorViewTest extends AndroidUnitTest {

    private ErrorView errorView;

    @Before
    public void before() {
        errorView = (ErrorView) LayoutInflater.from(context()).inflate(R.layout.ak_error_view, null);
    }

    @Test
    public void shouldDefaultToNoMessage() {
        assertThat(errorView.findViewById(R.id.ak_emptyview_error_message1).getVisibility()).isEqualTo(View.GONE);
        assertThat(errorView.findViewById(R.id.ak_emptyview_error_message2).getVisibility()).isEqualTo(View.GONE);
        assertThat(errorView.findViewById(R.id.ak_emptyview_error_message3).getVisibility()).isEqualTo(View.GONE);
    }

    @Test
    public void shouldSetServerErrorState() {
        errorView.setServerErrorState();
        assertThat(errorView.findViewById(R.id.ak_emptyview_error_message1).getVisibility()).isEqualTo(View.VISIBLE);
        assertThat(errorView.findViewById(R.id.ak_emptyview_error_message2).getVisibility()).isEqualTo(View.GONE);
        assertThat(errorView.findViewById(R.id.ak_emptyview_error_message3).getVisibility()).isEqualTo(View.GONE);
    }

    @Test
    public void shouldSetClientErrorState() {
        errorView.setConnectionErrorState();
        assertThat(errorView.findViewById(R.id.ak_emptyview_error_message1).getVisibility()).isEqualTo(View.GONE);
        assertThat(errorView.findViewById(R.id.ak_emptyview_error_message2).getVisibility()).isEqualTo(View.VISIBLE);
        assertThat(errorView.findViewById(R.id.ak_emptyview_error_message3).getVisibility()).isEqualTo(View.VISIBLE);
    }
}
