/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.terracotta.ehcache.search;

import net.sf.ehcache.CacheManager;
import net.sf.ehcache.constructs.nonstop.NonStopCacheException;
import net.sf.ehcache.search.Result;
import net.sf.ehcache.search.Results;

import org.apache.log4j.Logger;
import org.springframework.cache.ehcache.TcEhCacheManagerFactoryBean;

import com.terracotta.ehcache.perf.Configuration;
import com.terracotta.util.LongStat;

import java.util.List;
import java.util.Random;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class SearchExecutor{

  private final static Logger LOG = Logger.getLogger(SearchExecutor.class);

  private static int    searchThreads;
  private static int    searchInterval;
  private static int    hugeSearchRatio;
  private static int    iterateResults;

  private static LongStat smallSearches  = new LongStat(1024 * 1024);
  private static LongStat hugeSearches   = new LongStat(1024 * 1024);
  private static LongStat iterateSearches = new LongStat(1024 * 1024);

  private final SearchOwnerCache owners;
  private final SearchPetCache pets;
  private final SearchVisitCache visits;

  private final boolean includeValues;
  private final boolean includeKeys;

  public SearchExecutor(Configuration configuration, TcEhCacheManagerFactoryBean bean) {
    CacheManager cacheMgr = bean.getCacheManager();

    owners  = new SearchOwnerCache(cacheMgr.getCache("owners"));
    pets    = new SearchPetCache(cacheMgr.getCache("pets"));
    visits  = new SearchVisitCache(cacheMgr.getCache("visits"));

    searchInterval  = configuration.getInteger("search.interval", 1000);
    searchThreads   = configuration.getInteger("search.thread", 10);
    hugeSearchRatio = configuration.getInteger("search.huge.ratio", 20);
    iterateResults  = configuration.getInteger("search.iterate.results.ratio", 50);

    int maxResults = configuration.getInteger("search.maxResults", 50);
    owners.setMaxResults(maxResults);
    pets.setMaxResults(maxResults);
    visits.setMaxResults(maxResults);

    includeKeys = configuration.getBoolean("search.includeKeys", true);
    if (includeKeys){
      LOG.info("Enabling includeKeys() for all queries.");
      owners.enableIncludeKeys();
      pets.enableIncludeKeys();
      visits.enableIncludeKeys();
    }

    includeValues = configuration.getBoolean("search.includeValues", false);
    if (includeValues){
      LOG.info("Enabling includeValues() for all queries.");
      owners.enableIncludeValues();
      pets.enableIncludeValues();
      visits.enableIncludeValues();
    }

    LOG.info(String
             .format("Search intialized... Threads: %d, Interval: %d, HugeSearchRatio: %d, includeKeys: %b, includeValues: %b",
                     searchThreads, searchInterval, hugeSearchRatio, includeKeys, includeValues));
  }

  public void run(){
    ExecutorService service = Executors.newCachedThreadPool();
    for (int i=0; i<searchThreads; i++){
      service.execute(new SearchThread());
      try {
        Thread.sleep(searchInterval/searchThreads);
      } catch (InterruptedException e) {
        //
      }
    }
  }

  private static long now(){
    return System.currentTimeMillis();
  }

  class SearchThread implements Runnable{

    private final Random rnd = new Random();

    public void run() {
      while (true){
        try {
          Thread.sleep(searchInterval);
        } catch (InterruptedException e) {
          LOG.warn("Search Thread interupted.");
        }
        //        search(pets);

        switch(rnd.nextInt(100) % 3){
          case 0:
            search(pets);
            break;
          case 1:
            search(visits);
            break;
          case 2:
            search(owners);
            break;
        }
      }
    }

    private boolean isHugeSearch(){
      return rnd.nextInt(100) < hugeSearchRatio;
    }

    private void printStats(){
      hugeSearches.snapshot();
      smallSearches.snapshot();
      iterateSearches.snapshot();

      LOG.info(String.format("Huge Searches: %d , Avg: %.1f, Min: %d, Max: %d ", hugeSearches.size(), hugeSearches
                             .getAverage(), hugeSearches.getMin(), hugeSearches.getMax()));
      LOG.info(String.format("Small Searches: %d , Avg: %.1f, Min: %d, Max: %d ", smallSearches.size(), smallSearches
                             .getAverage(), smallSearches.getMin(), smallSearches.getMax()));
      LOG.info(String.format("Search Iteration: %d, Avg: %.1f, Min: %d, Max: %d ", iterateSearches.size(),
                             iterateSearches.getAverage(), iterateSearches.getMin(), iterateSearches.getMax()));
    }

    private void search(SearchCache cache){
      boolean isHuge = isHugeSearch();
      Results results = null;
      if (isHuge){
        try{
          long start = now();
          results = cache.searchHugeResultSet();
          long end = now();
          hugeSearches.add(end - start);
        } catch (NonStopCacheException e){
          e.printStackTrace();
        }
      }
      else{
        try{
          long start = now();
          results = cache.searchSmallResultSet();
          long end = now();
          smallSearches.add(end - start);
        } catch (NonStopCacheException e){
          e.printStackTrace();
        }
      }

      if (rnd.nextInt() < iterateResults && results != null){
        long start = now();
        LOG.info("Iterating through tests results.");
        for (Result res : results.all()){
          if (res != null){
            if(includeValues)
              res.getValue();
            else if(includeKeys)
              cache.get(res.getKey());

            List aggregate = res.getAggregatorResults();
            LOG.debug("Aggregator Size: " + aggregate.size());
          }
        }
        long end = now();
        iterateSearches.add(end - start);
      }

      printStats();
    }
  }

}
