package org.tc.perf.util;

import static org.tc.perf.util.SharedConstants.TC_CONFIG_URL;
import net.sf.ehcache.Cache;
import net.sf.ehcache.CacheManager;
import net.sf.ehcache.config.CacheConfiguration;
import net.sf.ehcache.config.Configuration;
import net.sf.ehcache.config.NonstopConfiguration;
import net.sf.ehcache.config.TerracottaClientConfiguration;
import net.sf.ehcache.config.TerracottaConfiguration;
import net.sf.ehcache.config.TerracottaConfiguration.Consistency;

import org.apache.log4j.Logger;

/**
 * 
 * CacheGenerator creates the cacheManager statically. Adds/removes the
 * terracotta clustered cache to it.
 * 
 * @author Himadri Singh
 */

public class CacheGenerator {

	private static final Logger log = Logger.getLogger(CacheGenerator.class);
	private static CacheManager cacheMgr;

	static {
		Configuration config = new Configuration();
		config.setName("FW_CACHEMGR");

		TerracottaClientConfiguration terracottaConfiguration = new TerracottaClientConfiguration();
		terracottaConfiguration.setUrl(TC_CONFIG_URL);
		terracottaConfiguration.setRejoin(false);
		config.addTerracottaConfig(terracottaConfiguration);

		cacheMgr = new CacheManager(config);
	}

	/**
	 * Creates a terracotta clustered cache
	 * 
	 * @param cacheName
	 *            name of the cache to be added
	 * @return cache created
	 */
	public static synchronized Cache getCache(String cacheName) {

		Cache cache = cacheMgr.getCache(cacheName);
		if (cache != null) {
			return cache;
		}
		CacheConfiguration config = new CacheConfiguration(cacheName, 1);
		config.setEternal(false);

		TerracottaConfiguration terracottaConfiguration = new TerracottaConfiguration();
		terracottaConfiguration.setConsistency(Consistency.STRONG);
		terracottaConfiguration.setConcurrency(256);
		NonstopConfiguration nonstopConfiguration = new NonstopConfiguration();
		nonstopConfiguration.setEnabled(false);
		terracottaConfiguration.addNonstop(nonstopConfiguration);
		config.addTerracotta(terracottaConfiguration);

		log.info("Creating cache : " + cacheName);
		cache = new Cache(config);
		cacheMgr.addCache(cache);

		return cache;
	}

	/**
	 * removes the data and cache from the cacheManager.
	 * 
	 * @param cacheName
	 *            name of the cache to be removed
	 */
	public static synchronized void removeCache(String cacheName) {
		cacheMgr.removeCache(cacheName);
	}

}
