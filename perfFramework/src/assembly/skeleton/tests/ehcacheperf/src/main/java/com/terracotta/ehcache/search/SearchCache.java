/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.terracotta.ehcache.search;

import net.sf.ehcache.Cache;
import net.sf.ehcache.constructs.nonstop.NonStopCacheException;
import net.sf.ehcache.search.Query;
import net.sf.ehcache.search.Results;

import org.apache.log4j.Logger;

import java.util.Random;

public abstract class SearchCache {

  protected static final Logger LOG = Logger.getLogger(SearchCache.class);
  protected final Cache cache;
  protected final Query smallQuery1, smallQuery2, hugeQuery1, hugeQuery2;
  private final Random rnd = new Random();

  public SearchCache(Cache cache) {
    this.cache = cache;
    smallQuery1 = cache.createQuery();
    smallQuery2 = cache.createQuery();
    hugeQuery1 = cache.createQuery();
    hugeQuery2 = cache.createQuery();
  }

  public String getCacheName(){
    return (cache != null)?cache.getName():"";
  }

  protected Results executeQuery(Query query) throws NonStopCacheException{
    LOG.debug("[" + getCacheName()+ "] Executing query.");
    Results res = null;
    res = query.execute();
    if (res != null)
      LOG.debug("[" + getCacheName()+ "] Query completed. Results Size: " + res.size());
    else
      LOG.error("Results set is NULL.");
    return res;
  }

  public Results searchSmallResultSet() throws NonStopCacheException {
    if (rnd.nextInt(100) < 50){
      LOG.debug("Running small query 1");
      return executeQuery(smallQuery1);
    }
    else{
      LOG.debug("Running small query 1");
      return executeQuery(smallQuery2);
    }
  }

  public Results searchHugeResultSet() throws NonStopCacheException {
    if (rnd.nextInt(100) < 50){
      LOG.debug("Running large query 1");
      return executeQuery(hugeQuery1);
    }
    else{
      LOG.debug("Running large query 1");
      return executeQuery(hugeQuery2);
    }
  }

  public Object get(Object key){
    return cache.get(key);
  }

  public void setMaxResults(int maxResults) {
    smallQuery1.maxResults(maxResults);
    smallQuery2.maxResults(maxResults);
  }

  public void enableIncludeKeys() {
    smallQuery1.includeKeys();
    smallQuery2.includeKeys();
    hugeQuery1.includeKeys();
    hugeQuery1.includeKeys();
  }

  public void enableIncludeValues() {
    smallQuery1.includeValues();
    smallQuery2.includeValues();
    hugeQuery1.includeValues();
    hugeQuery2.includeValues();
  }

}

