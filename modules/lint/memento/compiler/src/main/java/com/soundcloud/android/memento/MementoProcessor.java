package com.soundcloud.android.memento;

import com.android.tools.lint.client.api.IssueRegistry;
import com.android.tools.lint.detector.api.Detector;
import com.android.tools.lint.detector.api.Issue;
import com.google.auto.service.AutoService;
import com.google.common.collect.Sets;
import com.soundcloud.android.memento.annotation.Exclude;
import com.soundcloud.android.memento.annotation.LintDetector;
import com.soundcloud.android.memento.annotation.LintRegistry;
import com.soundcloud.android.memento.utils.Logger;
import org.jetbrains.annotations.Nullable;

import javax.annotation.processing.AbstractProcessor;
import javax.annotation.processing.Processor;
import javax.annotation.processing.RoundEnvironment;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.*;
import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeMirror;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

@AutoService(Processor.class)
public class MementoProcessor extends AbstractProcessor {

    private boolean processed;

    @Override
    public boolean process(Set<? extends TypeElement> set, RoundEnvironment roundEnvironment) {
        if (processed) {
            return true;
        }
        processed = true;
        final List<LintDetectorAnnotatedClass> annotatedClasses = processLintDetectors(roundEnvironment);
        final TypeElement registryElement = processLintRegistry(roundEnvironment);
        MementoCodeGenerator.generate(processingEnv, registryElement, annotatedClasses);
        return false;
    }

    @Nullable
    private TypeElement processLintRegistry(RoundEnvironment roundEnvironment) {
        TypeElement registryElement = null;
        for (Element element : roundEnvironment.getElementsAnnotatedWith(LintRegistry.class)) {
            if (element.getKind() != ElementKind.CLASS) {
                Logger.error(processingEnv, element, "LintRegistry Annotation can only be used on Classes");
                return null;
            }
            final TypeElement typeElement = (TypeElement) element;
            if (!typeElement.getModifiers().contains(Modifier.ABSTRACT)) {
                Logger.error(processingEnv, typeElement, "Custom Lint Registry must be abstract");
                return null;
            }
            if (isSubtype(typeElement, IssueRegistry.class)) {
                Logger.error(processingEnv, typeElement, "LintRegistry Annotation must be on a class extending IssueRegistry");
                return null;
            }
            if (registryElement != null) {
                Logger.fatalError(processingEnv, typeElement, "Multiple LintRegistries are not supported");
                return null;
            }
            if (isGetIssuesImplemented(typeElement)) {
                Logger.log(processingEnv, typeElement, "getIssues() is already implemented and will be ignored");
            }
            registryElement = typeElement;
        }
        return registryElement;
    }

    private boolean isGetIssuesImplemented(TypeElement typeElement) {
        final TypeElement detectorElement = processingEnv.getElementUtils().getTypeElement(IssueRegistry.class.getCanonicalName());
        if (detectorElement == null) {
            return false;
        }
        final ExecutableElement detectorGetIssuesMethod = findGetIssuesMethod(detectorElement);
        final ExecutableElement elementGetIssuesMethod = findGetIssuesMethod(typeElement);
        if (detectorGetIssuesMethod == null || elementGetIssuesMethod == null) {
            return false;
        }
        return processingEnv.getElementUtils().overrides(elementGetIssuesMethod, detectorGetIssuesMethod, typeElement);
    }

    @Nullable
    private ExecutableElement findGetIssuesMethod(TypeElement typeElement) {
        for (Element element : typeElement.getEnclosedElements()) {
            if (element.getKind() == ElementKind.METHOD) {
                ExecutableElement executableElement = (ExecutableElement) element;
                if (executableElement.getSimpleName().contentEquals("getIssues")) {
                    if (executableElement.getParameters().isEmpty()) {
                        TypeMirror returnType = executableElement.getReturnType();
                        if (returnType.getKind() == TypeKind.DECLARED) {
                            return executableElement;
                        }
                    }
                }
            }
        }
        return null;
    }

    private List<LintDetectorAnnotatedClass> processLintDetectors(RoundEnvironment roundEnvironment) {
        final List<LintDetectorAnnotatedClass> annotatedClasses = new ArrayList<>();
        for (Element element : roundEnvironment.getElementsAnnotatedWith(LintDetector.class)) {
            if (element.getKind() != ElementKind.CLASS) {
                Logger.error(processingEnv, element, "LintDetector Annotation can only be used on Classes");
                continue;
            }
            final TypeElement typeElement = (TypeElement) element;
            if (isSubtype(typeElement, Detector.class)) {
                Logger.error(processingEnv, typeElement, "LintDetector must be on a class extending Detector");
                continue;
            }
            final List<VariableElement> configuration = findIssueFields(typeElement);
            if (configuration == null || configuration.isEmpty()) {
                Logger.log(processingEnv, typeElement, "Detector contains no issues");
                continue;
            }
            annotatedClasses.add(new LintDetectorAnnotatedClass(typeElement, configuration));
        }
        return annotatedClasses;
    }

    private boolean isSubtype(TypeElement typeElement, Class<?> clazz) {
        return !processingEnv.getTypeUtils().isSubtype(typeElement.asType(),
                processingEnv.getElementUtils().getTypeElement(clazz.getCanonicalName()).asType());
    }

    @Override
    public Set<String> getSupportedAnnotationTypes() {
        return Sets.newHashSet(
                LintDetector.class.getCanonicalName(),
                LintRegistry.class.getCanonicalName()
        );
    }

    @Override
    public SourceVersion getSupportedSourceVersion() {
        return SourceVersion.latestSupported();
    }

    @Nullable
    private List<VariableElement> findIssueFields(Element element) {
        List<VariableElement> configurations = new ArrayList<>();
        for (Element child : element.getEnclosedElements()) {
            if (child.getKind() == ElementKind.FIELD) {
                VariableElement field = (VariableElement) child;
                if (isIssue(field)) {
                    if (field.getModifiers().contains(Modifier.PRIVATE)
                            || field.getModifiers().contains(Modifier.PROTECTED)
                            || !field.getModifiers().contains(Modifier.STATIC)) {
                        Logger.error(processingEnv, field, "Each Issue must be at least a package private constant");
                        return null;
                    }
                    if (field.getAnnotation(Exclude.class) != null) {
                        continue;
                    }
                    configurations.add(field);
                }
            }
        }
        return configurations;
    }

    private boolean isIssue(VariableElement field) {
        return processingEnv.getTypeUtils().isSameType(field.asType(),
                processingEnv.getElementUtils().getTypeElement(Issue.class.getCanonicalName()).asType());
    }
}
