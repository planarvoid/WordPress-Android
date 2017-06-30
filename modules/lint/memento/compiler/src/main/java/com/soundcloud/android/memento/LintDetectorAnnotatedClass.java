package com.soundcloud.android.memento;

import javax.lang.model.element.TypeElement;
import javax.lang.model.element.VariableElement;
import java.util.List;

class LintDetectorAnnotatedClass {
    private final List<VariableElement> configurations;
    private final TypeElement element;

    LintDetectorAnnotatedClass(TypeElement element, List<VariableElement> configurations) {
        this.element = element;
        this.configurations = configurations;
    }

    List<VariableElement> getConfigurations() {
        return configurations;
    }

    TypeElement getElement() {
        return element;
    }
}
