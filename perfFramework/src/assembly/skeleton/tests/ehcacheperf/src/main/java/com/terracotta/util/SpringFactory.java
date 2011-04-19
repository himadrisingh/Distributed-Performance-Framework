package com.terracotta.util;

import org.apache.log4j.Logger;
import org.springframework.beans.factory.config.PropertyPlaceholderConfigurer;
import org.springframework.beans.factory.xml.XmlBeanDefinitionReader;
import org.springframework.context.ApplicationContext;
import org.springframework.context.support.GenericApplicationContext;
import org.springframework.core.io.ClassPathResource;

import com.terracotta.ehcache.perf.Configuration;

import java.util.Map;
import java.util.Properties;

/**
 * @author Alex Snaps
 */
public class SpringFactory {
  public static String              configPrefix       = "application";
  public static String              SPACER             = "-";

  // allow us to add an ooi_config_default.xml if we want to
  public static String              defaultEnvironment = "test";

  private static final Logger       log                = Logger.getLogger(SpringFactory.class);
  private static ApplicationContext appContext;

  /**
   * Get the default app context, making it a singleton. Any properties from the Configuration object will be passed in
   * here will be applied to the context via a PropertyPlaceholderConfigurer
   */
  public static synchronized ApplicationContext getApplicationContext(Configuration configuration) {

    if (appContext != null) { return appContext; }

    // Selecting ehcache xml file
    if (configuration.getCacheType().startsWith("ehcache")) {
      configuration.getProperties().setProperty(
                                                "ehcache.config",
                                                configuration.getCacheType()
                                                + ((configuration.isSearchEnabled()) ? "-search.xml"
                                                    : "-normal.xml"));
      log.info("Ehcache config template: " + configuration.getProperties().getProperty("ehcache.config"));
    }

    // Selecting application context file
    String config;
    config = configPrefix + SPACER + configuration.getCacheType() + SPACER;
    config += ((configuration.isJtaEnabled()) ? "jta" + SPACER + configuration.getTransactionManager() + SPACER : "");
    config += ((configuration.isNoDB()) ? "nodb" + SPACER : "");
    config += configuration.getEnvironmentType() + ".xml";
    log.info("application context: " + config );

    appContext = getApplicationContext(config, configuration.getProperties());
    return appContext;
  }

  /**
   * Get a named app context. Note: no caching is done here - each call will construct a new context.
   * 
   * @param props Any properties passed in here will be applied to the context via a PropertyPlaceholderConfigurer
   */
  public static synchronized ApplicationContext getApplicationContext(String configName, Properties props) {
    log.info("Constructing app context: " + configName);

    // Get the bean definitions
    ClassPathResource config = new ClassPathResource(configName);
    if (!config.exists()) throw new RuntimeException("Config not found for: " + configName);

    GenericApplicationContext context = new GenericApplicationContext();
    XmlBeanDefinitionReader xmlReader = new XmlBeanDefinitionReader(context);
    xmlReader.loadBeanDefinitions(config);

    PropertyPlaceholderConfigurer placeholderConfig = new PropertyPlaceholderConfigurer();
    placeholderConfig.setIgnoreUnresolvablePlaceholders(true);
    placeholderConfig.setProperties(props);
    placeholderConfig.postProcessBeanFactory(context.getDefaultListableBeanFactory());

    context.refresh();
    log.info("constructed app context:" + context);
    return context;
  }

  @SuppressWarnings("unchecked")
  public static <T> T getBean(Configuration configuration, String name) {
    return (T) getApplicationContext(configuration).getBean(name);
  }

  @SuppressWarnings("unchecked")
  public static <T> T getControllerBean(Configuration configuration, Class<T> clas) {
    Map map = getApplicationContext(configuration).getBeansOfType(clas);
    if (map.size() == 0) { return null; }
    if (map.size() == 1) { return (T) map.values().iterator().next(); }
    throw new RuntimeException("multiple definitions of bean: " + clas);
  }

  public static <T> T getBean(final String beanName) {
    return (T) appContext.getBean(beanName);
  }
}
