package com.soundcloud.groupie;

import com.squareup.javapoet.CodeBlock;
import com.squareup.javapoet.FieldSpec;
import com.squareup.javapoet.JavaFile;
import com.squareup.javapoet.ParameterizedTypeName;
import com.squareup.javapoet.TypeSpec;

import javax.annotation.processing.Filer;
import javax.lang.model.element.Modifier;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;

class CodeGenerator {

    private static final String FIELD_ACTIVE_EXPERIMENTS = "ACTIVE_EXPERIMENTS";

    private static FieldSpec activeExperimentsList(List<ExperimentAnnotatedClass> annotatedClasses) {
        CodeBlock.Builder experimentList = CodeBlock.builder();
        experimentList.add("$T.asList(\n", Arrays.class);

        int size = annotatedClasses.size();
        String format = "  $T.$L,\n";
        for (int i = 0; i < size; i++) {
            ExperimentAnnotatedClass annotatedClass = annotatedClasses.get(i);
            if (i == size - 1) {
                format = "  $T.$L\n";
            }

            experimentList.add(format, annotatedClass.getElement(), annotatedClass.getConfiguration().getSimpleName());
        }
        experimentList.add(")");

        ParameterizedTypeName listType = ParameterizedTypeName.get(List.class, ExperimentConfiguration.class);
        return FieldSpec.builder(listType, FIELD_ACTIVE_EXPERIMENTS, Modifier.PUBLIC, Modifier.STATIC, Modifier.FINAL)
                        .initializer(experimentList.build())
                        .build();
    }

    static void generate(Filer filer, List<ExperimentAnnotatedClass> annotatedClasses) {
        TypeSpec activeExperiments = TypeSpec.classBuilder("ActiveExperiments")
                                             .addModifiers(Modifier.PUBLIC, Modifier.FINAL)
                                             .addField(activeExperimentsList(annotatedClasses))
                                             .build();

        JavaFile file = JavaFile.builder("com.soundcloud.android.experiments", activeExperiments)
                                .build();

        try {
            file.writeTo(filer);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
