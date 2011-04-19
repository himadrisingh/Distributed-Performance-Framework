package com.terracotta.ehcache.perf;

import net.sf.ehcache.CacheException;
import net.sf.ehcache.Ehcache;
import net.sf.ehcache.Element;
import net.sf.ehcache.writer.AbstractCacheWriter;
import net.sf.ehcache.writer.CacheWriter;
import net.sf.ehcache.writer.CacheWriterFactory;

import java.util.Collection;
import java.util.Properties;
import java.util.concurrent.atomic.AtomicLong;

/**
 * @author Alex Snaps
 */
public class FakeWriteBehindFactory extends CacheWriterFactory {

  public static final AtomicLong counter = new AtomicLong();
  public static volatile long millisToSleep = 0;

  @Override
  public CacheWriter createCacheWriter(final Ehcache ehcache, final Properties properties) {
    return new FakeCacheWriter(0);
  }

  private static class FakeCacheWriter extends AbstractCacheWriter {

    private final long tts;

    private FakeCacheWriter(long tts) {
      this.tts = tts;
    }

    @Override
    public void write(final Element element) throws CacheException {
      counter.incrementAndGet();
    }

    @Override
    public void writeAll(final Collection<Element> elements) throws CacheException {
      if(tts > 0) {
        try {
          Thread.sleep(tts);
        } catch (InterruptedException e) {
          Thread.currentThread().interrupt();
        }
      }
      for (Element element : elements) {
        element.getKey();
        element.getValue();
      }
      counter.addAndGet(elements.size());
    }
  }
}
