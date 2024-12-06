/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.esa.snap.rcp.about;

import org.openide.filesystems.annotations.LayerBuilder.File;
import org.openide.filesystems.annotations.LayerGeneratingProcessor;
import org.openide.filesystems.annotations.LayerGenerationException;
import org.openide.util.lookup.ServiceProvider;

import javax.annotation.processing.Processor;
import javax.annotation.processing.RoundEnvironment;
import javax.annotation.processing.SupportedAnnotationTypes;
import javax.annotation.processing.SupportedSourceVersion;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.Element;
import javax.lang.model.element.TypeElement;
import javax.lang.model.util.Elements;
import java.util.Set;

/**
 * Processor for the {@link AboutBox} annotations. This class generates the actual {@code layer.xml} entries.
 *
 * @author Norman Fomferra
 */
@ServiceProvider(service = Processor.class)
@SupportedAnnotationTypes("org.esa.snap.rcp.about.AboutBox")
@SupportedSourceVersion(SourceVersion.RELEASE_21)
public class AboutBoxProcessor extends LayerGeneratingProcessor {

    @Override
    protected boolean handleProcess(Set<? extends TypeElement> set, RoundEnvironment env) throws LayerGenerationException {
        Elements elements = processingEnv.getElementUtils();
        for (Element element : env.getElementsAnnotatedWith(AboutBox.class)) {
            TypeElement clazz = (TypeElement) element;
            AboutBox aboutBox = clazz.getAnnotation(AboutBox.class);
            String teName = elements.getBinaryName(clazz).toString();
            File file = layer(element)
                    .file("AboutBox/" + teName.replace('.', '-') + ".instance")
                    .intvalue("position", aboutBox.position())
                    .bundlevalue("displayName", aboutBox.displayName());
            if (!aboutBox.iconPath().isEmpty()) {
                file.bundlevalue("iconPath", aboutBox.iconPath());
            }
            file.write();
        }
        return true;
    }
}

