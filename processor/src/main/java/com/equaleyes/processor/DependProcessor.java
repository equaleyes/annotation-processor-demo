package com.equaleyes.processor;

import com.equaleyes.annotations.Depend;
import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.JavaFile;
import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.TypeSpec;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.annotation.processing.AbstractProcessor;
import javax.annotation.processing.Filer;
import javax.annotation.processing.Messager;
import javax.annotation.processing.ProcessingEnvironment;
import javax.annotation.processing.RoundEnvironment;
import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.PackageElement;
import javax.lang.model.element.TypeElement;
import javax.lang.model.util.Elements;

/**
 * Created by Žan Skamljič on 8/15/17.
 */

public class DependProcessor extends AbstractProcessor {
    private final String SUFFIX = "Provider";

    private Elements elementUtils;
    private Filer filer;
    private Messager messager;

    @Override
    public synchronized void init(ProcessingEnvironment processingEnvironment) {
        super.init(processingEnvironment);
        elementUtils = processingEnvironment.getElementUtils();
        filer = processingEnvironment.getFiler();
        messager = processingEnvironment.getMessager();
    }

    @Override
    public boolean process(Set<? extends TypeElement> set, RoundEnvironment roundEnvironment) {
        Map<Element, List<Element>> dependants = new HashMap<>();

        Set<? extends Element> fields = roundEnvironment.getElementsAnnotatedWith(Depend.class);
        for (Element field : fields) {
            try {
                validateField(field);
            } catch (ProcessingException e) {
                e.print(messager);
                continue;
            }

            // Get the class that contains this field
            Element parent = field.getEnclosingElement();

            // Add the field to list of dependencies for this class
            List<Element> dependantFields =
                    dependants.getOrDefault(parent, new ArrayList<Element>());
            dependantFields.add(field);

            dependants.put(parent, dependantFields);
        }

        try {
            generateCode(dependants);
        } catch (ProcessingException e) {
            e.print(messager);
        }

        return true;
    }

    @Override
    public Set<String> getSupportedAnnotationTypes() {
        return new LinkedHashSet<String>() {{
            add(Depend.class.getCanonicalName());
        }};
    }

    private void validateField(Element field) throws ProcessingException {
        Element enclosing = field.getEnclosingElement();
        if (enclosing == null || enclosing.getKind() != ElementKind.CLASS) {
            throw new ProcessingException(field, "Only classes can have dependencies.");
        }

        if (field.getModifiers().contains(Modifier.PRIVATE)) {
            throw new ProcessingException(field, "protected or higher access is required.");
        }

        if (field.getModifiers().contains(Modifier.FINAL)) {
            throw new ProcessingException(field, "final fields cannot be dependent.");
        }
    }

    private void generateCode(Map<Element, List<Element>> dependants) throws ProcessingException {
        for (Map.Entry<Element, List<Element>> dependant : dependants.entrySet()) {
            Element enclosing = dependant.getKey();
            List<Element> fields = dependant.getValue();

            // Create a provider name
            String providerName = enclosing.getSimpleName().toString() + SUFFIX;

            // Get the package name
            PackageElement pkg = elementUtils.getPackageOf(enclosing);
            String packageName = pkg.isUnnamed() ? "" : pkg.getQualifiedName().toString();

            // Generate the class
            TypeSpec.Builder classBuilder = TypeSpec.classBuilder(providerName)
                    //.addOriginatingElement(enclosing)
                    .addModifiers(Modifier.PUBLIC, Modifier.FINAL);

            MethodSpec.Builder provideFuncBuilder = MethodSpec.methodBuilder("provide")
                    .addModifiers(Modifier.PUBLIC, Modifier.STATIC)
                    .addParameter(ClassName.get(enclosing.asType()), "dependant");

            // Add fields to the parameters
            for (Element field : fields) {
                String fieldName = field.getSimpleName().toString();

                // Add the parameter to function
                provideFuncBuilder.addParameter(ClassName.get(field.asType()), fieldName);

                // Set the dependant field
                provideFuncBuilder.addStatement("dependant.$L = $L", fieldName, fieldName);
            }

            classBuilder.addMethod(provideFuncBuilder.build());

            try {
                JavaFile.builder(packageName, classBuilder.build()).build().writeTo(filer);
            } catch (IOException e) {
                throw new ProcessingException(enclosing, e.getMessage());
            }
        }
    }
}
