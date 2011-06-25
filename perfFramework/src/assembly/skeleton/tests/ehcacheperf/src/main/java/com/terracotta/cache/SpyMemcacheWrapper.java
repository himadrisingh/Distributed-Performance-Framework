package com.terracotta.cache;

import net.spy.memcached.MemcachedClient;

import org.springframework.samples.petclinic.CacheEntryAdapter;

import java.io.FileInputStream;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.Properties;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

public class SpyMemcacheWrapper<K, V> extends AbstractCacheWrapper<K, V> {

  private final String                 cacheName;
  private final static String          MEMCACHE_PROPS_FILE = System.getProperty("memcached.properties","src/main/resources/memcache-props.properties");
  private static MemcachedClient       client;

  public SpyMemcacheWrapper(final String cacheName) {
    this.cacheName = cacheName;
    Properties props = loadProperties(MEMCACHE_PROPS_FILE);
    String[] server = props.getProperty("hibernate.memcached.servers","localhost:11211").split(":");
    int port = Integer.parseInt(server[1]);
    try {
      client = new MemcachedClient(new InetSocketAddress(server[0], port));
    } catch (IOException e) {
      e.printStackTrace();
    }
  }

  @Override
  public void putInCache(final K key, final V value, final CacheEntryAdapter<V> adapter) {
    Future f = client.set(cacheName + key, 0, value);
    try {
      f.get();
    } catch (InterruptedException e) {
      e.printStackTrace();
    } catch (ExecutionException e) {
      e.printStackTrace();
    }
  }

  @Override
  public V getFromCache(final K key, CacheEntryAdapter<V> adapter) {
    Future f = client.asyncGet(cacheName + key);
    Object obj = null;
    try {
      obj = f.get();
    } catch (InterruptedException e) {
      e.printStackTrace();
    } catch (ExecutionException e) {
      e.printStackTrace();
    }
    return (V) obj;
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
