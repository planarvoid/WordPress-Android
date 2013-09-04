package com.soundcloud.android.properties;

import android.content.res.Resources;
import com.soundcloud.android.R.string;
import com.soundcloud.android.robolectric.SoundCloudTestRunner;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;

import static com.soundcloud.android.Expect.expect;
import static org.mockito.Mockito.when;

@RunWith(SoundCloudTestRunner.class)
public class ApplicationPropertiesTest {
    @Mock
    private Resources resources;

    @Test(expected = NullPointerException.class)
    public void shouldThrowExceptionIfNullResourcesProvided(){
        new ApplicationProperties(null);
    }

    @Test(expected = IllegalArgumentException.class)
    public void shouldThrowExceptionIfBuildTypeIsEmpty(){
        when(resources.getString(string.build_type)).thenReturn("  ");
        new ApplicationProperties(resources);
    }

    @Test(expected = IllegalArgumentException.class)
    public void shouldThrowExceptionIfBuildTypeIsNull(){
        when(resources.getString(string.build_type)).thenReturn(null);
        new ApplicationProperties(resources);
    }

    @Test(expected = IllegalArgumentException.class)
    public void shouldThrowExceptionIfBuildTypeIsUnrecognised(){
        when(resources.getString(string.build_type)).thenReturn("omgbuild");
        new ApplicationProperties(resources);
    }

    @Test
    public void shouldSpecifyThatBuildIsDebug(){
        when(resources.getString(string.build_type)).thenReturn("debug");
        ApplicationProperties applicationProperties = new ApplicationProperties(resources);
        expect(applicationProperties.isDebugBuild()).toBeTrue();
        expect(applicationProperties.isBetaBuild()).toBeFalse();
        expect(applicationProperties.isReleaseBuild()).toBeFalse();

    }

    @Test
    public void shouldSpecifyThatBuildIsBeta(){
        when(resources.getString(string.build_type)).thenReturn("beta");
        ApplicationProperties applicationProperties = new ApplicationProperties(resources);
        expect(applicationProperties.isDebugBuild()).toBeFalse();
        expect(applicationProperties.isBetaBuild()).toBeTrue();
        expect(applicationProperties.isReleaseBuild()).toBeFalse();

    }

    @Test
    public void shouldSpecifyThatBuildIsRelease(){
        when(resources.getString(string.build_type)).thenReturn("RELEASE");
        ApplicationProperties applicationProperties = new ApplicationProperties(resources);
        expect(applicationProperties.isDebugBuild()).toBeFalse();
        expect(applicationProperties.isBetaBuild()).toBeFalse();
        expect(applicationProperties.isReleaseBuild()).toBeTrue();
    }

    @Test
    public void shouldDetectThatItsNotRunningOnDalvikVM(){
        expect(ApplicationProperties.mIsRunningOnDalvik).toBeFalse();
    }

    @Test
    public void shouldDetectThatItsNotRunningOnEmulator(){
        expect(ApplicationProperties.mIsRunningOnEmulator).toBeFalse();
    }

}
