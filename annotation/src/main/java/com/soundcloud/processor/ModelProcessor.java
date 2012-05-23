package com.soundcloud.processor;

import com.sun.source.tree.ClassTree;
import com.sun.source.tree.Tree;
import com.sun.source.tree.TreeVisitor;
import com.sun.source.tree.VariableTree;
import com.sun.source.util.Trees;
import org.apache.velocity.Template;
import org.apache.velocity.VelocityContext;
import org.apache.velocity.app.VelocityEngine;

import javax.annotation.processing.AbstractProcessor;
import javax.annotation.processing.ProcessingEnvironment;
import javax.annotation.processing.RoundEnvironment;
import javax.annotation.processing.SupportedAnnotationTypes;
import javax.annotation.processing.SupportedSourceVersion;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.Element;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.PackageElement;
import javax.lang.model.element.TypeElement;
import javax.lang.model.element.VariableElement;
import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.util.ElementFilter;
import javax.lang.model.util.Elements;
import javax.tools.Diagnostic;
import javax.tools.JavaFileObject;
import java.io.IOException;
import java.io.Writer;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

@SuppressWarnings("UnusedDeclaration")
@SupportedAnnotationTypes("*")
@SupportedSourceVersion(SourceVersion.RELEASE_6)
public class ModelProcessor extends AbstractProcessor {

    private Trees trees;
    private ProcessingEnvironment env;

    @Override
    public synchronized void init(ProcessingEnvironment env) {
        super.init(env);
        trees = Trees.instance(env);
        this.env = env;
    }

    @Override
    public boolean process(Set<? extends TypeElement> typeElements, RoundEnvironment renv) {
        if (!renv.processingOver()) {
            Set<? extends Element> elements =
                    renv.getRootElements();

            for (Element elt : elements) {
                Tree tree = trees.getTree(elt);
                if (tree != null && Tree.Kind.CLASS.equals(tree.getKind()) && isModel(elt)) {
                    try {
                        process((ClassTree) tree, (TypeElement) elt);
                    } catch (IOException e) {
                        error(e.getMessage());
                    }
                }
            }
        }
        return true;
    }


    public static class ModelField {
        public VariableTree v;

        public ModelField(VariableTree v) {
            this.v = v;
        }

        public String getName() { return v.getName().toString(); }
        public String getType() {
            String t = v.getType().toString();
            return t.substring(0, 1).toUpperCase() + t.substring(1, t.length());
        }
    }

    private void process(ClassTree tree, TypeElement e) throws IOException {
        PackageElement packageElement =
                (PackageElement) e.getEnclosingElement();

        final String newClass = e.getSimpleName() + "Helper";

        List<ModelField> variables = new ArrayList<ModelField>();
        addFields(e, variables);

        TypeMirror superclass = e.getSuperclass();
        if (superclass.getKind() != TypeKind.NONE) {
            TypeElement te = env.getElementUtils().getTypeElement(superclass.toString());
            addFields(te, variables);
        }

        VelocityEngine ve = getEngine();
        VelocityContext vc = new VelocityContext();

        vc.put("modelClass", e.getSimpleName());
        vc.put("className", newClass);
        vc.put("packageName", packageElement.getQualifiedName());
        vc.put("variables", variables);

        Template vt = ve.getTemplate("/templates/helper.vm");

        log("creating "+newClass);
        JavaFileObject jf = env.getFiler().createSourceFile(packageElement.getQualifiedName()+"."+newClass);
        final Writer writer = jf.openWriter();
        vt.merge(vc, writer);
        writer.close();
    }

    private void addFields(TypeElement e, List<ModelField> variables) {
        outer:
        for (VariableElement ve : ElementFilter.fieldsIn(e.getEnclosedElements())) {

            for (AnnotationMirror am : ve.getAnnotationMirrors()) {
                TypeElement te = (TypeElement) am.getAnnotationType().asElement();
                if ("com.fasterxml.jackson.annotation.JsonIgnore".contentEquals(te.getQualifiedName())) {
                    continue outer;
                }
            }

            if (ve.getModifiers().contains(Modifier.PUBLIC) &&
               !ve.getModifiers().contains(Modifier.FINAL)) {

                variables.add(new ModelField((VariableTree) trees.getTree(ve)));
            }
        }
    }

    private VelocityEngine getEngine() throws IOException {
        VelocityEngine ve = new VelocityEngine();
        ve.setProperty("resource.loader", "classpath");
        ve.setProperty("classpath.resource.loader.class", "org.apache.velocity.runtime.resource.loader.ClasspathResourceLoader");
        ve.init();
        return ve;
    }

    private void log(String s) {
        env.getMessager().printMessage(Diagnostic.Kind.NOTE, s);
    }

    private void error(String s) {
        env.getMessager().printMessage(Diagnostic.Kind.ERROR, s);
    }

    private boolean isModel(Element e) {
        List<? extends AnnotationMirror> annos =  e.getAnnotationMirrors();
        for (AnnotationMirror a : annos) {
            TypeElement te = (TypeElement) a.getAnnotationType().asElement();
            if ("com.soundcloud.android.model.Model".contentEquals(te.getQualifiedName())) {
                return true;
            }
        }
        return false;
    }
}
