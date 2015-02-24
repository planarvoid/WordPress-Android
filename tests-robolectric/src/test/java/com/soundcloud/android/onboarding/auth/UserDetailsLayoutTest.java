package com.soundcloud.android.onboarding.auth;

import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.isNotNull;
import static org.mockito.Matchers.isNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import com.soundcloud.android.R;
import com.soundcloud.android.robolectric.SoundCloudTestRunner;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;

import android.app.Activity;
import android.view.ViewStub;
import android.widget.Button;
import android.widget.LinearLayout;

import java.io.File;

@RunWith(SoundCloudTestRunner.class)
public class UserDetailsLayoutTest {

    @Mock UserDetailsLayout.UserDetailsHandler userDetailsHandler;

    private UserDetailsLayout userDetailsLayout;
    private Button saveButton;


    @Before
    public void setUp() throws Exception {
        userDetailsLayout = prepareUserDetailsLayout();
        userDetailsLayout.setUserDetailsHandler(userDetailsHandler);

        saveButton = (Button) userDetailsLayout.findViewById(R.id.btn_save);
    }

    @Test
    public void onImageTakeResetsTemporaryImageFileWhenResultCodeIsNotOK() {
        userDetailsLayout.setAvatarTemporaryFile(mock(File.class));

        userDetailsLayout.onImageTake(Activity.RESULT_CANCELED);
        saveButton.performClick();

        verify(userDetailsHandler).onSubmitDetails(anyString(), isNull(File.class));
    }

    @Test
    public void onImageDoesNotResetTemporaryImageWhenActivityResultIsOK() {
        userDetailsLayout.setAvatarTemporaryFile(mock(File.class));

        userDetailsLayout.onImageTake(Activity.RESULT_OK);
        saveButton.performClick();

        verify(userDetailsHandler).onSubmitDetails(anyString(), isNotNull(File.class));
    }

    private UserDetailsLayout prepareUserDetailsLayout() {
        // static ImageUtils require activity to be passed
        Activity sampleActivity = new Activity();
        ViewStub viewStub = new ViewStub(sampleActivity);
        viewStub.setLayoutResource(R.layout.signup_details);

        // View stub needs a parent layout
        LinearLayout layout = new LinearLayout(sampleActivity);
        layout.addView(viewStub);

        return (UserDetailsLayout) viewStub.inflate();
    }

}