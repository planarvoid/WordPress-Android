package com.soundcloud.android.configuration.experiments;


import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.soundcloud.java.strings.Strings;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import android.content.res.Configuration;
import android.content.res.Resources;

import java.util.Locale;

@RunWith(MockitoJUnitRunner.class)
public class ItalianExperimentTest {

    @Mock private ExperimentOperations experimentOperations;
    @Mock private Resources resources;
    @Mock private Configuration configuration;
    @Mock private android.util.DisplayMetrics displayMetrics;

    @Captor private ArgumentCaptor<Configuration> configurationArgumentCaptor;

    private ItalianExperiment experiment;

    @Before
    public void setUp() throws Exception {
        experiment = new ItalianExperiment(experimentOperations);
    }

    @Test
    public void shouldRevertToEnglishWhenInControlVariantAndItalianLocale() {
        configureForLocale(Locale.ITALY);
        when(experimentOperations.getExperimentVariant(ItalianExperiment.CONFIGURATION)).thenReturn(ItalianExperiment.VARIANT_CONTROL);

        experiment.configure(resources);

        Locale assignedLocale = captureConfiguredLocale();
        assertThat(assignedLocale.getLanguage()).isEqualTo("en");
        assertThat(assignedLocale.getCountry()).isEqualTo("IT");
    }

    @Test
    public void shouldRevertToEnglishWhenNotInTheExperimentAndItalianLocale() {
        configureForLocale(Locale.ITALY);
        when(experimentOperations.getExperimentVariant(ItalianExperiment.CONFIGURATION)).thenReturn(Strings.EMPTY);

        experiment.configure(resources);

        Locale assignedLocale = captureConfiguredLocale();
        assertThat(assignedLocale.getLanguage()).isEqualTo("en");
        assertThat(assignedLocale.getCountry()).isEqualTo("IT");
    }

    @Test
    public void shouldRevertToEnglishWhenNotInTheExperimentAndItalianLanguageRegardlessOfCountry() {
        configureForLocale(new Locale("it", "CH"));
        when(experimentOperations.getExperimentVariant(ItalianExperiment.CONFIGURATION)).thenReturn(Strings.EMPTY);

        experiment.configure(resources);

        Locale assignedLocale = captureConfiguredLocale();
        assertThat(assignedLocale.getLanguage()).isEqualTo("en");
        assertThat(assignedLocale.getCountry()).isEqualTo("CH");
    }

    @Test
    public void shouldNotRevertToEnglishWhenItalianVariantAndItalianLocale() {
        configureForLocale(Locale.ITALY);
        when(experimentOperations.getExperimentVariant(ItalianExperiment.CONFIGURATION)).thenReturn(ItalianExperiment.VARIANT_ITALIAN);

        experiment.configure(resources);

        verify(resources, never()).updateConfiguration(any(), any());
    }

    @Test
    public void shouldNotChangeLocaleWhenNotItalian() {
        configureForLocale(Locale.FRANCE);
        when(experimentOperations.getExperimentVariant(ItalianExperiment.CONFIGURATION)).thenReturn(ItalianExperiment.VARIANT_CONTROL);

        experiment.configure(resources);

        verify(resources, never()).updateConfiguration(any(), any());
    }

    private void configureForLocale(Locale locale) {
        Configuration configuration = new Configuration();
        configuration.locale = locale;
        when(resources.getConfiguration()).thenReturn(configuration);
        when(resources.getDisplayMetrics()).thenReturn(displayMetrics);
    }

    private Locale captureConfiguredLocale() {
        verify(resources).updateConfiguration(configurationArgumentCaptor.capture(), eq(displayMetrics));
        return configurationArgumentCaptor.getValue().locale;
    }
}
