package org.springframework.cache.ehcache;

import net.sf.ehcache.Cache;
import net.sf.ehcache.CacheException;
import net.sf.ehcache.CacheManager;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.FactoryBean;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.core.io.Resource;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;

/**
 * @author Alex Snaps
 */
public class TcEhCacheManagerFactoryBean implements FactoryBean, InitializingBean, DisposableBean {

  protected final Log  logger             = LogFactory.getLog(getClass());
  private Resource     configLocation;
  private boolean      shared             = false;

  private String       cacheManagerName;
  private CacheManager cacheManager;

  private boolean      standalone         = false;
  private String       expressTerracottaUrl = "localhost:9510";

  private String       concurrency        = "2048";
  private String       coherent           = "true";
  private String       consistency        = "strong";
  private String       synchronousWrites  = "false";
  private String       valueMode          = "identity";
  private String       localKeyCache      = "true";
  private String       localKeyCacheSize  = "150000";
  private String       storageStrategy    = "classic";
  private String       statistics         = "false";

  private String       owners_tti         = "0";
  private String       owners_ttl         = "0";
  private String       owners_capacity    = "0";
  private String       owners_inMemory    = "0";
  private String       pets_tti           = "0";
  private String       pets_ttl           = "0";
  private String       pets_inMemory      = "0";
  private String       pets_capacity      = "0";
  private String       visits_tti         = "0";
  private String       visits_ttl         = "0";
  private String       visits_inMemory    = "0";
  private String       visits_capacity    = "0";
  private String       petVisits_tti      = "0";
  private String       petVisits_ttl      = "0";
  private String       petVisits_inMemory = "0";
  private String       petVisits_capacity = "0";
  private String       ownerPets_tti      = "0";
  private String       ownerPets_ttl      = "0";
  private String       ownerPets_inMemory = "0";
  private String       ownerPets_capacity = "0";
  private String       petTypes_tti       = "0";
  private String       petTypes_ttl       = "0";
  private String       petTypes_capacity  = "0";
  private String       petTypes_inMemory  = "0";
  private String       clustered          = "true";
  private String       transactionalMode  = "xa";
  private String       copyStrategy       = "net.sf.ehcache.store.compound.SerializationCopyStrategy";
  private String       copyOnRead         = "false";
  private String       copyOnWrite        = "false";
  private String       maxWriteDelay      = "1";
  private String       writeBatching      = "true";
  private String       writeBatchSize     = "1000";
  private String       writeCoalescing    = "true";
  private String       writeMode          = "write-behind";

  private String       nonstopEnabled          = "false";
  private String       nonstopTimeoutMillis    = "15000";
  private String       nonstopImmediateTimeout = "true";
  private String       nonstopTimeoutBehavior  = "exception";
  private String       rejoin                  = "false";

  /**
   * Set the location of the EHCache config file. A typical value is "/WEB-INF/ehcache.xml".
   * <p>
   * Default is "ehcache.xml" in the root of the class path, or if not found, "ehcache-failsafe.xml" in the EHCache jar
   * (default EHCache initialization).
   * 
   * @see net.sf.ehcache.CacheManager#create(java.io.InputStream)
   * @see net.sf.ehcache.CacheManager#CacheManager(java.io.InputStream)
   */
  public void setConfigLocation(Resource configLocation) {
    this.configLocation = configLocation;
  }

  /**
   * Set whether the EHCache CacheManager should be shared (as a singleton at the VM level) or independent (typically
   * local within the application). Default is "false", creating an independent instance.
   * 
   * @see net.sf.ehcache.CacheManager#create()
   * @see net.sf.ehcache.CacheManager#CacheManager()
   */
  public void setShared(boolean shared) {
    this.shared = shared;
  }

  /**
   * Set the name of the EHCache CacheManager (if a specific name is desired).
   * 
   * @see net.sf.ehcache.CacheManager#setName(String)
   */
  public void setCacheManagerName(String cacheManagerName) {
    this.cacheManagerName = cacheManagerName;
  }

  public void afterPropertiesSet() throws IOException, CacheException {
    logger.info("Initializing EHCache CacheManager");
    if (this.shared) {
      // Shared CacheManager singleton at the VM level.
      if (this.configLocation != null) {
        this.cacheManager = CacheManager.create(getConfigInputStream());
      } else {
        this.cacheManager = CacheManager.create();
      }
    } else {
      // Independent CacheManager instance (the default).
      if (this.configLocation != null) {
        this.cacheManager = new CacheManager(getConfigInputStream());
      } else {
        this.cacheManager = new CacheManager();
      }
    }
    if (this.cacheManagerName != null) {
      this.cacheManager.setName(this.cacheManagerName);
    }

  }

  public void setCopyStrategy(final String copyStrategy) {
    this.copyStrategy = copyStrategy;
  }

  private InputStream getConfigInputStream() throws IOException {
    logger.info("Reading ehcache config from: " + configLocation);
    InputStream inputStream = this.configLocation.getInputStream();
    StringBuilder buff = new StringBuilder();
    int read;
    for (byte[] b = new byte[1000]; (read = inputStream.read(b)) != -1;) {
      buff.append(new String(b, 0, read));
    }

    String tempString = buff.toString();
    tempString = replaceAll(tempString, "\\$\\{pets_tti\\}", pets_tti);
    tempString = replaceAll(tempString, "\\$\\{pets_ttl\\}", pets_ttl);
    tempString = replaceAll(tempString, "\\$\\{pets_capacity\\}", pets_capacity);
    tempString = replaceAll(tempString, "\\$\\{pets_inMemory\\}", pets_inMemory);

    tempString = replaceAll(tempString, "\\$\\{visits_tti\\}", visits_tti);
    tempString = replaceAll(tempString, "\\$\\{visits_ttl\\}", visits_ttl);
    tempString = replaceAll(tempString, "\\$\\{visits_capacity\\}", visits_capacity);
    tempString = replaceAll(tempString, "\\$\\{visits_inMemory\\}", visits_inMemory);

    tempString = replaceAll(tempString, "\\$\\{ownerPets_tti\\}", ownerPets_tti);
    tempString = replaceAll(tempString, "\\$\\{ownerPets_ttl\\}", ownerPets_ttl);
    tempString = replaceAll(tempString, "\\$\\{ownerPets_inMemory\\}", ownerPets_inMemory);
    tempString = replaceAll(tempString, "\\$\\{ownerPets_capacity\\}", ownerPets_capacity);

    tempString = replaceAll(tempString, "\\$\\{petTypes_tti\\}", petTypes_tti);
    tempString = replaceAll(tempString, "\\$\\{petTypes_ttl\\}", petTypes_ttl);
    tempString = replaceAll(tempString, "\\$\\{petTypes_inMemory\\}", petTypes_inMemory);
    tempString = replaceAll(tempString, "\\$\\{petTypes_capacity\\}", petTypes_capacity);

    tempString = replaceAll(tempString, "\\$\\{petVisits_tti\\}", petVisits_tti);
    tempString = replaceAll(tempString, "\\$\\{petVisits_ttl\\}", petVisits_ttl);
    tempString = replaceAll(tempString, "\\$\\{petVisits_inMemory\\}", petVisits_inMemory);
    tempString = replaceAll(tempString, "\\$\\{petVisits_capacity\\}", petVisits_capacity);

    tempString = replaceAll(tempString, "\\$\\{owners_tti\\}", owners_tti);
    tempString = replaceAll(tempString, "\\$\\{owners_ttl\\}", owners_ttl);
    tempString = replaceAll(tempString, "\\$\\{owners_inMemory\\}", owners_inMemory);
    tempString = replaceAll(tempString, "\\$\\{owners_capacity\\}", owners_capacity);

    tempString = replaceAll(tempString, "\\$\\{consistency\\}", consistency);
    tempString = replaceAll(tempString, "\\$\\{concurrency\\}", concurrency);
    tempString = replaceAll(tempString, "\\$\\{coherent\\}", coherent);
    tempString = replaceAll(tempString, "\\$\\{synchronousWrites\\}", synchronousWrites);
    tempString = replaceAll(tempString, "\\$\\{clustered\\}", clustered);
    tempString = replaceAll(tempString, "\\$\\{transactionalMode\\}", transactionalMode);
    tempString = replaceAll(tempString, "\\$\\{valueMode\\}", valueMode);
    tempString = replaceAll(tempString, "\\$\\{localKeyCache\\}", localKeyCache);
    tempString = replaceAll(tempString, "\\$\\{localKeyCacheSize\\}", localKeyCacheSize);
    tempString = replaceAll(tempString, "\\$\\{storageStrategy\\}", storageStrategy);
    tempString = replaceAll(tempString, "\\$\\{statistics\\}", statistics);

    tempString = replaceAll(tempString, "\\$\\{copyOnRead\\}", copyOnRead);
    tempString = replaceAll(tempString, "\\$\\{copyOnWrite\\}", copyOnWrite);
    tempString = replaceAll(tempString, "\\$\\{maxWriteDelay\\}", maxWriteDelay);
    tempString = replaceAll(tempString, "\\$\\{writeBatching\\}", writeBatching);
    tempString = replaceAll(tempString, "\\$\\{writeBatchSize\\}", writeBatchSize);
    tempString = replaceAll(tempString, "\\$\\{writeCoalescing\\}", writeCoalescing);
    tempString = replaceAll(tempString, "\\$\\{writeMode\\}", writeMode);

    tempString = replaceAll(tempString, "\\$\\{nonstopEnabled\\}", nonstopEnabled);
    tempString = replaceAll(tempString, "\\$\\{nonstopTimeoutMillis\\}", nonstopTimeoutMillis);
    tempString = replaceAll(tempString, "\\$\\{nonstopImmediateTimeout\\}", nonstopImmediateTimeout);
    tempString = replaceAll(tempString, "\\$\\{nonstopTimeoutBehavior\\}", nonstopTimeoutBehavior);

    String copyStrategyStr = (copyStrategy != "") ? "<copyStrategy class=\"" + copyStrategy + "\"/>" : "";
    tempString = replaceAll(tempString, "\\$\\{copyStrategy\\}", copyStrategyStr);

    if (isStandalone())
      tempString = replaceAll(tempString, "\\$\\{expressTerracottaUrl\\}", "<terracottaConfig url=\""
                              + expressTerracottaUrl
                              + "\" rejoin=\"" + rejoin
                              + "\"/>");
    else
      tempString = replaceAll(tempString, "\\$\\{expressTerracottaUrl\\}", "");

    logger.info("ehcache config:\n" + tempString);
    return new ByteArrayInputStream(tempString.getBytes());

  }

  private String replaceAll(String inputString, String regex, String replacement) {
    try {
      return inputString.replaceAll(regex, replacement);
    } catch (Exception e) {
      throw new RuntimeException("Failed to replace '" + regex + "' with '" + replacement + "'. Is the property '"
                                 + replacement + "' present in the configuration file?");
    }
  }

  public void setBulkLoad(boolean c){
    if (isClustered()) {
      String[] caches = cacheManager.getCacheNames();
      for (String name : caches){
        Cache cache = cacheManager.getCache(name);
        logger.info("Setting cache [" + name + "] bulk load: " + c);
        cache.setNodeBulkLoadEnabled(c);
      }
    }
  }

  public void waitUntilBulkLoadComplete(){
    if (isClustered()) {
      String[] caches = cacheManager.getCacheNames();
      for (String name : caches){
        Cache cache = cacheManager.getCache(name);
        logger.info("Waiting for cache [" + name + "] to complete bulk load.");
        cache.waitUntilClusterBulkLoadComplete();
      }
    }
  }


  public String getStorageStrategy() {
    return storageStrategy;
  }

  public void setStorageStrategy(String storageStrategy) {
    this.storageStrategy = storageStrategy;
  }

  public String getConcurrency() {
    return concurrency;
  }

  public void setConcurrency(String concurrency) {
    this.concurrency = concurrency;
  }

  public void setCoherent(final String coherent) {
    this.coherent = coherent;
  }

  public void setValueMode(String valueMode) {
    this.valueMode = valueMode;
  }

  public void setLocalKeyCache(String localKeyCache) {
    this.localKeyCache = localKeyCache;
  }

  public void setLocalKeyCacheSize(String localKeyCacheSize) {
    this.localKeyCacheSize = localKeyCacheSize;
  }

  public String getVisits_tti() {
    return visits_tti;
  }

  public void setVisits_tti(String visitsTti) {
    visits_tti = visitsTti;
  }

  public String getVisits_ttl() {
    return visits_ttl;
  }

  public void setVisits_ttl(String visitsTtl) {
    visits_ttl = visitsTtl;
  }

  public String getOwners_tti() {
    return owners_tti;
  }

  public void setOwners_tti(String ownersTti) {
    owners_tti = ownersTti;
  }

  public String getOwners_ttl() {
    return owners_ttl;
  }

  public void setOwners_ttl(String ownersTtl) {
    owners_ttl = ownersTtl;
  }

  public String getPets_tti() {
    return pets_tti;
  }

  public void setPets_tti(String petsTti) {
    pets_tti = petsTti;
  }

  public String getPets_ttl() {
    return pets_ttl;
  }

  public void setPets_ttl(String petsTtl) {
    pets_ttl = petsTtl;
  }

  public String getPetVisits_tti() {
    return petVisits_tti;
  }

  public void setPetVisits_tti(String petVisitsTti) {
    petVisits_tti = petVisitsTti;
  }

  public String getPetVisits_ttl() {
    return petVisits_ttl;
  }

  public void setPetVisits_ttl(String petVisitsTtl) {
    petVisits_ttl = petVisitsTtl;
  }

  public String getOwnerPets_tti() {
    return ownerPets_tti;
  }

  public void setOwnerPets_tti(String ownerPetsTti) {
    ownerPets_tti = ownerPetsTti;
  }

  public String getOwnerPets_ttl() {
    return ownerPets_ttl;
  }

  public void setOwnerPets_ttl(String ownerPetsTtl) {
    ownerPets_ttl = ownerPetsTtl;
  }

  public String getPetTypes_tti() {
    return petTypes_tti;
  }

  public void setPetTypes_tti(String petTypesTti) {
    petTypes_tti = petTypesTti;
  }

  public String getPetTypes_ttl() {
    return petTypes_ttl;
  }

  public void setPetTypes_ttl(String petTypesTtl) {
    petTypes_ttl = petTypesTtl;
  }

  public String getOwners_capacity() {
    return owners_capacity;
  }

  public void setOwners_capacity(String ownersCapacity) {
    owners_capacity = ownersCapacity;
  }

  public String getPets_capacity() {
    return pets_capacity;
  }

  public void setPets_capacity(String petsCapacity) {
    pets_capacity = petsCapacity;
  }

  public String getVisits_capacity() {
    return visits_capacity;
  }

  public void setVisits_capacity(String visitsCapacity) {
    visits_capacity = visitsCapacity;
  }

  public String getPetVisits_capacity() {
    return petVisits_capacity;
  }

  public void setPetVisits_capacity(String petVisitsCapacity) {
    petVisits_capacity = petVisitsCapacity;
  }

  public String getOwnerPets_capacity() {
    return ownerPets_capacity;
  }

  public void setOwnerPets_capacity(String ownerPetsCapacity) {
    ownerPets_capacity = ownerPetsCapacity;
  }

  public String getPetTypes_capacity() {
    return petTypes_capacity;
  }

  public void setPetTypes_capacity(String petTypesCapacity) {
    petTypes_capacity = petTypesCapacity;
  }

  public String getOwners_inMemory() {
    return owners_inMemory;
  }

  public void setOwners_inMemory(String ownersInMemory) {
    owners_inMemory = ownersInMemory;
  }

  public String getPets_inMemory() {
    return pets_inMemory;
  }

  public void setPets_inMemory(String petsInMemory) {
    pets_inMemory = petsInMemory;
  }

  public String getVisits_inMemory() {
    return visits_inMemory;
  }

  public void setVisits_inMemory(String visitsInMemory) {
    visits_inMemory = visitsInMemory;
  }

  public String getStatistics() {
    return statistics;
  }

  public void setStatistics(String statistics) {
    this.statistics = statistics;
  }

  public boolean isStandalone() {
    return standalone;
  }

  public void setStandalone(boolean standalone) {
    this.standalone = standalone;
  }

  public String getPetVisits_inMemory() {
    return petVisits_inMemory;
  }

  public void setPetVisits_inMemory(String petVisitsInMemory) {
    petVisits_inMemory = petVisitsInMemory;
  }

  public String getOwnerPets_inMemory() {
    return ownerPets_inMemory;
  }

  public void setOwnerPets_inMemory(String ownerPetsInMemory) {
    ownerPets_inMemory = ownerPetsInMemory;
  }

  public String getPetTypes_inMemory() {
    return petTypes_inMemory;
  }

  public boolean isClustered() {
    return Boolean.valueOf(clustered);
  }

  public void setClustered(final boolean clustered) {
    this.clustered = Boolean.toString(clustered);
  }

  public void setPetTypes_inMemory(String petTypesInMemory) {
    petTypes_inMemory = petTypesInMemory;
  }

  public void setExpressTerracottaUrl(String expressTerracottaUrl) {
    this.expressTerracottaUrl = expressTerracottaUrl;
  }

  public void setMaxWriteDelay(final String maxWriteDelay) {
    this.maxWriteDelay = maxWriteDelay;
  }

  public void setWriteMode(String writeMode) {
    this.writeMode = writeMode;
  }

  public void setWriteBatching(final String writeBatching) {
    this.writeBatching = writeBatching;
  }

  public void setWriteBatchSize(final String writeBatchSize) {
    this.writeBatchSize = writeBatchSize;
  }

  public void setWriteCoalescing(final String writeCoalescing) {
    this.writeCoalescing = writeCoalescing;
  }

  public CacheManager getCacheManager(){
    return this.cacheManager;
  }

  public Object getObject() {
    return this.cacheManager;
  }

  public Class getObjectType() {
    return (this.cacheManager != null ? this.cacheManager.getClass() : CacheManager.class);
  }

  public boolean isSingleton() {
    return true;
  }

  public void destroy() {
    logger.info("Shutting down EHCache CacheManager");
    this.cacheManager.shutdown();
  }

  public void setTransactionalMode(String transactionalMode) {
    this.transactionalMode = transactionalMode;
  }

  public String getTransactionalMode() {
    return transactionalMode;
  }

  public String getCopyOnWrite() {
    return copyOnWrite;
  }

  public void setCopyOnWrite(String copyOnWrite) {
    this.copyOnWrite = copyOnWrite;
  }

  public String getCopyOnRead() {
    return copyOnRead;
  }

  public void setCopyOnRead(String copyOnRead) {
    this.copyOnRead = copyOnRead;
  }

  public String getNonstopEnabled() {
    return nonstopEnabled;
  }

  public void setNonstopEnabled(String nonstopEnabled) {
    this.nonstopEnabled = nonstopEnabled;
  }

  public String getNonstopTimeoutMillis() {
    return nonstopTimeoutMillis;
  }

  public void setNonstopTimeoutMillis(String nonstopTimeoutMillis) {
    this.nonstopTimeoutMillis = nonstopTimeoutMillis;
  }

  public String getNonstopImmediateTimeout() {
    return nonstopImmediateTimeout;
  }

  public void setNonstopImmediateTimeout(String nonstopImmediateTimeout) {
    this.nonstopImmediateTimeout = nonstopImmediateTimeout;
  }

  public String getNonstopTimeoutBehavior() {
    return nonstopTimeoutBehavior;
  }

  public void setNonstopTimeoutBehavior(String nonstopTimeoutBehavior) {
    this.nonstopTimeoutBehavior = nonstopTimeoutBehavior;
  }

  public void setConsistency(String consistency) {
    this.consistency = consistency;
  }

  public void setSynchronousWrites(String synchronousWrites) {
    this.synchronousWrites = synchronousWrites;
  }

  public String getRejoin() {
    return rejoin;
  }

  public void setRejoin(String rejoin) {
    this.rejoin = rejoin;
  }
}
