package com.soundcloud.android.properties;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import com.soundcloud.android.R.string;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import android.content.res.Resources;

@RunWith(MockitoJUnitRunner.class)
public class ApplicationPropertiesTest {

    @Mock private Resources resources;

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
        assertThat(applicationProperties.isDebugBuild()).isTrue();
        assertThat(applicationProperties.isReleaseBuild()).isFalse();
    }

    @Test
    public void shouldSpecifyThatBuildIsAlpha(){
        when(resources.getString(string.build_type)).thenReturn("alpha");
        ApplicationProperties applicationProperties = new ApplicationProperties(resources);
        assertThat(applicationProperties.isAlphaBuild()).isTrue();
        assertThat(applicationProperties.isReleaseBuild()).isFalse();
    }

    @Test
    public void shouldSpecifyThatBuildIsRelease(){
        when(resources.getString(string.build_type)).thenReturn("RELEASE");
        ApplicationProperties applicationProperties = new ApplicationProperties(resources);
        assertThat(applicationProperties.isDebugBuild()).isFalse();
        assertThat(applicationProperties.isReleaseBuild()).isTrue();
    }

    @Test
    public void shouldDetectThatItsNotRunningOnDevice(){
        assertThat(ApplicationProperties.IS_RUNNING_ON_DEVICE).isFalse();
    }

    @Test
    public void shouldDetectThatItsNotRunningOnEmulator(){
        assertThat(ApplicationProperties.IS_RUNNING_ON_EMULATOR).isFalse();
    }

    @Test
    public void shouldReturnCastReceiverAppId(){
        when(resources.getString(string.build_type)).thenReturn("debug");
        when(resources.getString(string.cast_receiver_app_id)).thenReturn("123");
        assertThat(new ApplicationProperties(resources).getCastReceiverAppId()).isEqualTo("123");
    }

}
