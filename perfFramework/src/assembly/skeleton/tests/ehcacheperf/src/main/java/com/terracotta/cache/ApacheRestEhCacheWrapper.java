package com.terracotta.cache;

import org.apache.commons.httpclient.HostConfiguration;
import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.HttpException;
import org.apache.commons.httpclient.HttpStatus;
import org.apache.commons.httpclient.MultiThreadedHttpConnectionManager;
import org.apache.commons.httpclient.methods.ByteArrayRequestEntity;
import org.apache.commons.httpclient.methods.GetMethod;
import org.apache.commons.httpclient.methods.PutMethod;
import org.apache.commons.httpclient.params.HttpConnectionManagerParams;
import org.springframework.samples.petclinic.CacheEntryAdapter;

import java.io.ByteArrayOutputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.Properties;

/**
 * A simple example Java client which uses the commons HTTPClient.
 */
public class ApacheRestEhCacheWrapper<K, V> implements CacheWrapper<K, V> {

  private final static String     EHCACHE_SERVER_PROPS_FILE = "ehcache-server.properties";

  private static final Properties properties;

  // shared across all caches and threads
  private final static HttpClient client;

  static {
    properties = loadProperties(EHCACHE_SERVER_PROPS_FILE);

    client = new HttpClient();
    MultiThreadedHttpConnectionManager manager = new MultiThreadedHttpConnectionManager();
    HttpConnectionManagerParams conParams = new HttpConnectionManagerParams();
    manager.setParams(conParams);
    int maxConnPerHost = Integer.parseInt(properties.getProperty("ehcache.server.max.host.connections", "20"));
    conParams.setMaxConnectionsPerHost(HostConfiguration.ANY_HOST_CONFIGURATION, maxConnPerHost);
    int maxTotalConn = Integer.parseInt(properties.getProperty("ehcache.server.max.total.connections", "100"));
    conParams.setMaxTotalConnections(maxTotalConn);
    conParams.setStaleCheckingEnabled(false);
    client.setHttpConnectionManager(manager);
  }

  private final String            URL;
  private final String            cacheName;

  /**
   * Creates a new instance of EHCacheREST
   */
  public ApacheRestEhCacheWrapper(final String cacheName) {
    this.cacheName = cacheName;
    // client = new HttpClient(new MultiThreadedHttpConnectionManager());
    URL = properties.getProperty("ehcache.server.rest.url");
    createCache();
  }

  private void createCache() {
    // Create a method instance.
    PutMethod method = new PutMethod(URL + cacheName);
    try {
      int statusCode = client.executeMethod(method);
      if (statusCode != HttpStatus.SC_OK) {
        System.err.println("Method failed: " + method.getStatusLine());
      }
    } catch (HttpException e) {
      System.err.println("Fatal protocol violation: " + e.getMessage());
      e.printStackTrace();
    } catch (IOException e) {
      System.err.println("Fatal transport error: " + e.getMessage());
      e.printStackTrace();
    } finally {
      method.releaseConnection();
    }
  }

  public void put(final K key, final V value, final CacheEntryAdapter<V> adapter) {
    PutMethod put = new PutMethod(URL + cacheName + "/" + key);
    try {
      ByteArrayOutputStream baos = new ByteArrayOutputStream();
      ObjectOutputStream oos = new ObjectOutputStream(baos);
      oos.writeObject(value);
      oos.close();

      byte[] toSend = baos.toByteArray();
      put.setRequestEntity(new ByteArrayRequestEntity(toSend));
      int statusCode = client.executeMethod(put);

      if (statusCode != HttpStatus.SC_OK) {
        // System.err.println("Method failed: " + method.getStatusLine());
      }
    } catch (HttpException e) {
      System.err.println("Fatal protocol violation: " + e.getMessage());
      e.printStackTrace();
    } catch (IOException e) {
      System.err.println("Fatal transport error: " + e.getMessage());
      e.printStackTrace();
    } finally {
      put.releaseConnection();
    }
  }

  @SuppressWarnings("unchecked")
  public V get(final K key, CacheEntryAdapter<V> adapter) {
    GetMethod method = new GetMethod(URL + cacheName + "/" + key);
    try {
      // Execute the method.
      int statusCode = client.executeMethod(method);

      if (statusCode != HttpStatus.SC_OK) {
        // System.err.println("Method failed: " + method.getStatusLine());
        return null;
      }

      ObjectInputStream ois = new ObjectInputStream(method.getResponseBodyAsStream());
      return (V) ois.readObject();

    } catch (HttpException e) {
      System.err.println("Fatal protocol violation: " + e.getMessage());
      e.printStackTrace();
    } catch (IOException e) {
      System.err.println("Fatal transport error: " + e.getMessage());
      e.printStackTrace();
    } catch (ClassNotFoundException e) {
      e.printStackTrace();
    } finally {
      method.releaseConnection();
    }
    return null;
  }

  public Object getUnderlyingCache() {
    return this;
  }

  private static Properties loadProperties(final String location) {
    Properties props = new Properties();
    try {
      props.load(new FileInputStream(location));
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
    return props;
  }

  public int getSize() {
    throw new UnsupportedOperationException();
  }
}