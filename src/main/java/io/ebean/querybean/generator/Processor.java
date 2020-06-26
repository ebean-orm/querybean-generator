package io.ebean.querybean.generator;

import javax.annotation.processing.AbstractProcessor;
import javax.annotation.processing.ProcessingEnvironment;
import javax.annotation.processing.RoundEnvironment;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.Element;
import javax.lang.model.element.TypeElement;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.Set;

/**
 * Process compiled entity beans and generates 'query beans' for them.
 */
public class Processor extends AbstractProcessor implements Constants {

  private ProcessingContext processingContext;

  public Processor() {
  }

  @Override
  public synchronized void init(ProcessingEnvironment processingEnv) {
    super.init(processingEnv);
    this.processingContext = new ProcessingContext(processingEnv);
  }

  @Override
  public Set<String> getSupportedAnnotationTypes() {
    Set<String> annotations = new LinkedHashSet<>();
    annotations.add(ENTITY);
    annotations.add(EMBEDDABLE);
    annotations.add(CONVERTER);
    annotations.add(EBEAN_COMPONENT);
    annotations.add(MODULEINFO);
    return annotations;
  }

  @Override
  public SourceVersion getSupportedSourceVersion() {
    return SourceVersion.latest();
  }

  @Override
  public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment roundEnv) {
    processingContext.readModuleInfo();
    int count = processEntities(roundEnv);
    processOthers(roundEnv);
    final int loaded = processingContext.complete();
    if (roundEnv.processingOver()) {
      writeModuleInfoBean();
    }
    if (count > 0) {
      String msg = "Ebean APT generated %s query beans, loaded %s others - META-INF/ebean-generated-info.mf entity-packages: %s";
      processingContext.logNote(msg, count, loaded, processingContext.getAllEntityPackages());
    }
    return true;
  }

  private int processEntities(RoundEnvironment roundEnv) {
    int count = 0;
    for (Element element : roundEnv.getElementsAnnotatedWith(processingContext.embeddableAnnotation())) {
      generateQueryBeans(element);
      count++;
    }
    for (Element element : roundEnv.getElementsAnnotatedWith(processingContext.entityAnnotation())) {
      generateQueryBeans(element);
      count++;
    }
    return count;
  }

  private void processOthers(RoundEnvironment roundEnv) {
    for (Element element : roundEnv.getElementsAnnotatedWith(processingContext.converterAnnotation())) {
      processingContext.addOther(element);
    }
    for (Element element : roundEnv.getElementsAnnotatedWith(processingContext.componentAnnotation())) {
      processingContext.addOther(element);
    }
  }

  private void writeModuleInfoBean() {
    try {
      SimpleModuleInfoWriter writer = new SimpleModuleInfoWriter(processingContext);
      writer.write();
    } catch (Throwable e) {
      e.printStackTrace();
      processingContext.logError(null, "Failed to write ModuleInfoLoader error:" + e + " stack:" + Arrays.toString(e.getStackTrace()));
    }
  }

  private void generateQueryBeans(Element element) {
    try {
      SimpleQueryBeanWriter beanWriter = new SimpleQueryBeanWriter((TypeElement) element, processingContext);
      beanWriter.writeRootBean();
      beanWriter.writeAssocBean();
    } catch (Throwable e) {
      e.printStackTrace();
      processingContext.logError(element, "Error generating query beans: " + e);
    }
  }

}
