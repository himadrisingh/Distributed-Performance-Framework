package org.springframework.cache.ehcache;

import net.sf.ehcache.transaction.manager.TransactionManagerLookup;
import net.sf.ehcache.transaction.xa.EhcacheXAResource;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.lang.reflect.Method;
import java.util.Properties;

import javax.transaction.TransactionManager;
import javax.transaction.xa.XAResource;

/**
 * @author Alex Snaps
 */
public class SpringTransactionManagerLookup implements TransactionManagerLookup {
  private static final String                BTM_TM_CLASSNAME = "bitronix.tm.BitronixTransactionManager";

  protected static final Log                 LOG              = LogFactory.getLog(SpringTransactionManagerLookup.class);

  private static volatile TransactionManager transactionManager;

  public TransactionManager getTransactionManager() {
    return transactionManager;
  }

  public void setTransactionManager(final TransactionManager transactionManager) {
    SpringTransactionManagerLookup.transactionManager = transactionManager;
  }

  public void register(final EhcacheXAResource ehcacheXAResource) {
    if (transactionManager.getClass().getName().equals(BTM_TM_CLASSNAME)) {
      registerResourceWithBitronix(ehcacheXAResource.getCacheName(), ehcacheXAResource);
    }
  }

  public void unregister(final EhcacheXAResource ehcacheXAResource) {
    if (transactionManager.getClass().getName().equals(BTM_TM_CLASSNAME)) {
      unregisterResourceWithBitronix(ehcacheXAResource.getCacheName(), ehcacheXAResource);
    }
  }

  public void setProperties(final Properties properties) {
    //
  }

  private void registerResourceWithBitronix(String uniqueName, EhcacheXAResource resource) {
    ClassLoader cl = Thread.currentThread().getContextClassLoader();
    if (cl == null) {
      cl = ClassLoader.getSystemClassLoader();
    }
    try {
      // This requires BTM 2.0.0 at least
      Class producerClass = cl.loadClass("bitronix.tm.resource.ehcache.EhCacheXAResourceProducer");

      Class[] signature = new Class[] { String.class, XAResource.class };
      Object[] args = new Object[] { uniqueName, resource };
      Method method = producerClass.getMethod("registerXAResource", signature);
      method.invoke(null, args);
    } catch (Exception e) {
      LOG.error("unable to register resource of cache " + uniqueName + " with BTM", e);
    }
  }

  private void unregisterResourceWithBitronix(String uniqueName, EhcacheXAResource resource) {
    ClassLoader cl = Thread.currentThread().getContextClassLoader();
    if (cl == null) {
      cl = ClassLoader.getSystemClassLoader();
    }
    try {
      // This requires BTM 2.0.0 at least
      Class producerClass = cl.loadClass("bitronix.tm.resource.ehcache.EhCacheXAResourceProducer");

      Class[] signature = new Class[] { String.class, XAResource.class };
      Object[] args = new Object[] { uniqueName, resource };
      Method method = producerClass.getMethod("unregisterXAResource", signature);
      method.invoke(null, args);
    } catch (Exception e) {
      LOG.error("unable to unregister resource of cache " + uniqueName + " with BTM", e);
    }
  }
}
