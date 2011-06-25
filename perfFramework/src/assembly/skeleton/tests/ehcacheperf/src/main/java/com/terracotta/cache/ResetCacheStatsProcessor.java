/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.terracotta.cache;

public class ResetCacheStatsProcessor implements CacheProcessor {

  public void processCache(CacheWrapper cacheWrapper) {
    if (cacheWrapper instanceof AbstractCacheWrapper){
      AbstractCacheWrapper wrapper = (AbstractCacheWrapper) cacheWrapper;
      wrapper.reset();
    }
  }
}