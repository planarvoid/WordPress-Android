package com.soundcloud.groupie;

import javax.lang.model.element.TypeElement;
import javax.lang.model.element.VariableElement;

class ExperimentAnnotatedClass {

    private final VariableElement configuration;
    private final TypeElement element;

    ExperimentAnnotatedClass(TypeElement element, VariableElement configuration) {
        this.element = element;
        this.configuration = configuration;
    }

    VariableElement getConfiguration() {
        return configuration;
    }

    TypeElement getElement() {
        return element;
    }
}
