package com.soundcloud.android.configuration.experiments;

import static com.soundcloud.android.configuration.experiments.ExperimentOperations.LISTENING_LAYER;
import static java.util.Arrays.asList;

import com.soundcloud.groupie.ActiveExperiment;
import com.soundcloud.groupie.ExperimentConfiguration;

import android.content.res.Configuration;
import android.content.res.Resources;

import javax.inject.Inject;
import java.util.Locale;

@ActiveExperiment
public class ItalianExperiment {

    private static final String NAME = "italian_localization_android";

    static final String VARIANT_CONTROL = "control";
    static final String VARIANT_ITALIAN = "italian";

    public static final ExperimentConfiguration CONFIGURATION = ExperimentConfiguration
            .fromName(LISTENING_LAYER, NAME, asList(VARIANT_CONTROL, VARIANT_ITALIAN));

    private final ExperimentOperations experimentOperations;

    @Inject
    ItalianExperiment(ExperimentOperations experimentOperations) {
        this.experimentOperations = experimentOperations;
    }

    public void configure(Resources resources) {
        switch (getVariant()) {
            case VARIANT_ITALIAN:
                break;
            case VARIANT_CONTROL:
            default:
                enforceEnglishLocaleForItalian(resources);
                break;
        }
    }

    private void enforceEnglishLocaleForItalian(Resources resources) {
        Locale currentLocale = resources.getConfiguration().locale;

        if (currentLocale.getLanguage().equals(Locale.ITALIAN.getLanguage())) {
            Locale locale = new Locale(Locale.ENGLISH.getLanguage(), currentLocale.getCountry(), currentLocale.getVariant());
            Locale.setDefault(locale);
            setLocale(resources, locale);
        }
    }

    private void setLocale(Resources resources, Locale locale) {
        Configuration configuration = new Configuration();
        configuration.locale = locale;
        resources.updateConfiguration(configuration, resources.getDisplayMetrics());
    }

    private String getVariant() {
        return experimentOperations.getExperimentVariant(CONFIGURATION);
    }

}
