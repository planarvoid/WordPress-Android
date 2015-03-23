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
public class SignupDetailsLayoutTest {

    @Mock SignupDetailsLayout.UserDetailsHandler userDetailsHandler;

    private SignupDetailsLayout signupDetailsLayout;
    private Button saveButton;


    @Before
    public void setUp() throws Exception {
        signupDetailsLayout = prepareUserDetailsLayout();
        signupDetailsLayout.setUserDetailsHandler(userDetailsHandler);

        saveButton = (Button) signupDetailsLayout.findViewById(R.id.btn_save);
    }

    @Test
    public void onImageTakeResetsTemporaryImageFileWhenResultCodeIsNotOK() {
        signupDetailsLayout.setAvatarTemporaryFile(mock(File.class));

        signupDetailsLayout.onImageTake(Activity.RESULT_CANCELED);
        saveButton.performClick();

        verify(userDetailsHandler).onSubmitUserDetails(anyString(), isNull(File.class));
    }

    @Test
    public void onImageDoesNotResetTemporaryImageWhenActivityResultIsOK() {
        signupDetailsLayout.setAvatarTemporaryFile(mock(File.class));

        signupDetailsLayout.onImageTake(Activity.RESULT_OK);
        saveButton.performClick();

        verify(userDetailsHandler).onSubmitUserDetails(anyString(), isNotNull(File.class));
    }

    private SignupDetailsLayout prepareUserDetailsLayout() {
        // static ImageUtils require activity to be passed
        Activity sampleActivity = new Activity();
        ViewStub viewStub = new ViewStub(sampleActivity);
        viewStub.setLayoutResource(R.layout.signup_user_details);

        // View stub needs a parent layout
        LinearLayout layout = new LinearLayout(sampleActivity);
        layout.addView(viewStub);

        return (SignupDetailsLayout) viewStub.inflate();
    }

}
