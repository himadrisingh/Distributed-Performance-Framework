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
public class CacheGenerator {

	private static CacheManager cacheMgr;

	static{
		Configuration config = new Configuration();

		TerracottaClientConfiguration terracottaConfiguration = new TerracottaClientConfiguration();
		terracottaConfiguration.setUrl(TC_CONFIG_URL);
		terracottaConfiguration.setRejoin(false);
		config.addTerracottaConfig(terracottaConfiguration);

		cacheMgr = new CacheManager(config);
	}

	public static Cache getCache(String cacheName){

		Cache cache = cacheMgr.getCache(cacheName);
		if ( cache != null){
			return cache;
		}
		CacheConfiguration config = new CacheConfiguration(cacheName, 10000);
		config.setEternal(true);

		TerracottaConfiguration terracottaConfiguration = new TerracottaConfiguration();
		terracottaConfiguration.setConsistency(Consistency.STRONG);
		NonstopConfiguration nonstopConfiguration = new NonstopConfiguration();
		nonstopConfiguration.setEnabled(false);
		terracottaConfiguration.addNonstop(nonstopConfiguration);
		config.addTerracotta(terracottaConfiguration);

		cache = new Cache(config);
		cacheMgr.addCache(cache);

		return cache;
	}

}
