package com.soundcloud.groupie;

import com.google.auto.service.AutoService;
import com.google.common.collect.Sets;

import javax.annotation.processing.AbstractProcessor;
import javax.annotation.processing.Filer;
import javax.annotation.processing.Messager;
import javax.annotation.processing.ProcessingEnvironment;
import javax.annotation.processing.Processor;
import javax.annotation.processing.RoundEnvironment;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.TypeElement;
import javax.lang.model.element.VariableElement;
import javax.tools.Diagnostic;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

@AutoService(Processor.class)
public class ExperimentProcessor extends AbstractProcessor {

    private boolean processed;
    private Messager messager;
    private Filer filer;

    @Override
    public synchronized void init(ProcessingEnvironment processingEnvironment) {
        super.init(processingEnvironment);

        messager = processingEnvironment.getMessager();
        filer = processingEnvironment.getFiler();
    }

    @Override
    public boolean process(Set<? extends TypeElement> set, RoundEnvironment roundEnvironment) {
        if (processed) {
            return true;
        }
        processed = true;

        List<ExperimentAnnotatedClass> annotatedClasses = new ArrayList<>();

        for (Element element : roundEnvironment.getElementsAnnotatedWith(ActiveExperiment.class)) {
            if (element.getKind() != ElementKind.CLASS) {
                error(element, "ActiveExperiment Annotation should only be used on Classes");
                return true;
            }

            VariableElement configuration = findConfigurationField(element);
            if (configuration == null) {
                return true;
            }

            annotatedClasses.add(new ExperimentAnnotatedClass((TypeElement) element, configuration));
        }

        CodeGenerator.generate(filer, annotatedClasses);
        return false;
    }

    @Override
    public Set<String> getSupportedAnnotationTypes() {
        return Sets.newHashSet(
                ActiveExperiment.class.getCanonicalName(),
                "*" // Will cause the processor to run even if no @ActiveExperiment annotations are present in the codebase
        );
    }

    @Override
    public SourceVersion getSupportedSourceVersion() {
        return SourceVersion.latestSupported();
    }

    private VariableElement findConfigurationField(Element element) {
        VariableElement configuration = null;
        for (Element child : element.getEnclosedElements()) {
            if (child.getKind() == ElementKind.FIELD) {
                VariableElement field = (VariableElement) child;

                if (ExperimentConfiguration.class.getCanonicalName().equals(field.asType().toString())) {
                    if (configuration != null) {
                        error(element, "Only one Field of type ExperimentConfiguration allowed");
                        return null;
                    }

                    configuration = field;
                }
            }
        }

        if (configuration == null) {
            error(element, "No Field with type ExperimentConfiguration found");
            return null;
        }

        if (!configuration.getModifiers().contains(Modifier.PUBLIC)) {
            error(configuration, "Field annotated with ExperimentConfiguration should be public");
            return null;
        }

        if (!configuration.getModifiers().contains(Modifier.STATIC)) {
            error(configuration, "Field annotated with ExperimentConfiguration should be static");
            return null;
        }

        return configuration;
    }

    private void error(Element element, String message, Object... args) {
        if (args.length > 0) {
            message = String.format(message, args);
        }

        messager.printMessage(Diagnostic.Kind.ERROR, message, element);
    }
}
