package com.terracotta.ehcache.perf;

import net.sf.ehcache.config.CacheConfiguration.TransactionalMode;
import net.sf.ehcache.config.TerracottaConfiguration.Consistency;
import net.sf.ehcache.config.TerracottaConfiguration.StorageStrategy;
import net.sf.ehcache.config.TerracottaConfiguration.ValueMode;

import org.apache.log4j.Logger;

import com.terracotta.ehcache.perf.test.TestCase;

import java.util.Properties;
import java.util.concurrent.TimeUnit;

/**
 * @author Alex Snaps
 */
public class Configuration {

  private static final Logger log = Logger.getLogger(Configuration.class);

  private final boolean    standalone;
  private final int        nodesNum;
  private final int        threadNum;
  private final Properties props;
  private final String     expressTerracottaUrl;
  private final TestCase   testCase;
  private final long       testDuration;
  private final int        reportInterval;
  private final String     cacheType;
  private final String     environmentType;
  private final int        writePercentage;
  private final boolean    logMisses;
  private final boolean    l1Enabled;
  private final boolean    jtaEnabled;
  private final boolean    searchEnabled;
  private final boolean    bulkLoadEnabled;
  private final String     transactionManager;
  private final boolean    serverMap;
  private final boolean    noDB;

  private final int       addOwnersPeriodInSecs;
  private final int       addOwnersCount;
  private final boolean   addOwnersEnabled;

  private HotSetConfiguration       hotSetConfiguration;

  private final int elementNum;

  public Configuration(Properties props) {
    this.props = props;
    this.standalone = getBoolean("standalone", true);
    this.serverMap = getBoolean("serverMap", false);
    this.noDB = getBoolean("noDB", true);
    this.nodesNum = getInteger("numOfNodes", 1);
    this.threadNum = getInteger("numOfThreads", 5);
    this.expressTerracottaUrl = getString("expressTerracottaUrl", "localhost:9510").trim();
    this.testCase = TestCase.valueOf(getString("testCase", "readOnlyTest"));
    this.testDuration = getLong("duration", 60);
    this.reportInterval = getInteger("reportInterval", 5);
    this.cacheType = getString("cache", "ehcache");
    this.environmentType = getString("env", "local");
    this.writePercentage = getInteger("readwrite.write.percentage", 2);
    this.logMisses = getBoolean("logMisses", false);
    this.l1Enabled = getBoolean("l1WarmupEnabled", true);
    this.jtaEnabled = getBoolean("jtaEnabled", false);
    this.bulkLoadEnabled = getBoolean("bulkLoad.enabled", true);
    this.transactionManager = getString("transactionManager", "btm");
    this.elementNum = getInteger("elementNum", 1000);

    this.addOwnersCount = getInteger("addOwnersCount", 10);
    this.addOwnersEnabled = getBoolean("addOwners.enabled", false);
    this.addOwnersPeriodInSecs = getInteger("addOwnersPeriodInSecs", 20);

    this.searchEnabled = getBoolean("search.enabled", false);
    com.terracotta.ehcache.perf.FakeWriteBehindFactory.millisToSleep = TimeUnit.SECONDS.toMillis(getInteger("writer.maxWriteDelay", 0)) / 2;

    // Validating properties file for application context
    getInteger("keyPaddingInBytes", 100);
    getInteger("valuePaddingInBytes", 100);
    getBoolean("variableValue", false);
    getBoolean("singleThreadedWarmup", false);
    getInteger("warmUpThreads", 50);

    getBoolean("useNonStopCache" , false);
    getInteger("nonStopCache.timeoutMillis" , 1000);
    getBoolean("nonStopCache.immediateTimeout" , true);
    getString("nonStopCache.timeoutBehavior" , "exception");

    getString("expressTerracottaUrl" , "localhost:9510");
    getBoolean("ehcache.rejoin" , false);

    getInteger("ehcache.localKeyCacheSize" , 150000);
    getInteger("ehcache.concurrency" , 4096);

    getBoolean("ehcache.localKeyCache" , false);
    getBoolean("ehcache.coherent" , true);
    getBoolean("ehcache.coherentReads" , true);
    getBoolean("ehcache.synchronousWrites", false);
    getBoolean("ehcache.clustered" , true);
    getBoolean("ehcache.statistics" , false);

    getString("ehcache.valueMode" , ValueMode.SERIALIZATION.toString());
    getString("ehcache.consistency", Consistency.STRONG.toString());
    getString("ehcache.transactionalMode" , TransactionalMode.OFF.toString());
    //    getString("ehcache.copyStrategy" , "net.sf.ehcache.store.compound.SerializationCopyStrategy");
    getString("ehcache.copyStrategy" , "");
    getString("ehcache.storageStrategy" , StorageStrategy.DCV2.toString());
    boolean copyOnRead = getBoolean("ehcache.copyOnRead" , false);
    if (searchEnabled && !copyOnRead){
      log.warn("Search is enabled without copyOnRead. Enabling copyOnRead explictly.");
      this.props.setProperty("ehcache.copyOnRead" , "true");
    }

    if (jtaEnabled){
      log.warn("JTA enabled tests needs cache to be coherent and copyOnRead & copyOnWrite enabled.");
      this.props.setProperty("ehcache.copyOnRead" , "true");
      this.props.setProperty("ehcache.copyOnWrite" , "true");
      this.props.setProperty("ehcache.coherent" , "true");
    }

    getInteger("ehcache.owners.inMemory" , 0);
    getInteger("ehcache.owners.capacity" , 0);
    getInteger("ehcache.owners.tti" , 0);
    getInteger("ehcache.owners.ttl" , 0);

    getInteger("ehcache.pets.inMemory" , 0);
    getInteger("ehcache.pets.capacity" , 0);
    getInteger("ehcache.pets.tti" , 0);
    getInteger("ehcache.pets.ttl" , 0);

    getInteger("ehcache.visits.inMemory" , 0);
    getInteger("ehcache.visits.capacity" , 0);
    getInteger("ehcache.visits.tti" , 0);
    getInteger("ehcache.visits.ttl" , 0);

    getInteger("ehcache.petVisits.inMemory" , 0);
    getInteger("ehcache.petVisits.capacity" , 0);
    getInteger("ehcache.petVisits.tti" , 0);
    getInteger("ehcache.petVisits.ttl" , 0);

    getInteger("ehcache.ownerPets.inMemory" , 0);
    getInteger("ehcache.ownerPets.capacity" , 0);
    getInteger("ehcache.ownerPets.tti" , 0);
    getInteger("ehcache.ownerPets.ttl" , 0);

    getInteger("ehcache.petTypes.inMemory" , 0);
    getInteger("ehcache.petTypes.capacity" , 0);
    getInteger("ehcache.petTypes.tti" , 0);
    getInteger("ehcache.petTypes.ttl" , 0);

    getInteger("writer.maxWriteDelay" , 1);
    getBoolean("writer.writeBatching" , true);
    getInteger("writer.writeBatchSize" , 1000);
    getBoolean("writer.writeCoalescing" , true);

  }

  public int getElementNum() {
    return elementNum;
  }

  public boolean getBoolean(String key, boolean defaultValue) {
    return Boolean.valueOf(getString(key, String.valueOf(defaultValue)));
  }

  public long getLong(String key, long defaultValue) {
    return Long.valueOf(getString(key, String.valueOf(defaultValue)));
  }

  public int getInteger(String key, int defaultValue) {
    return Integer.valueOf(getString(key, String.valueOf(defaultValue)));
  }

  public String getString(String key, String defaultValue) {
    String value = props.getProperty(key);
    if (value == null){
      log.warn("Key not found in Properties: " + key + " , Using defaults: " + defaultValue);
      props.setProperty(key, defaultValue);
      return defaultValue;
    }
    return value.trim();
  }

  public boolean isStandalone() {
    return standalone;
  }

  public boolean isServerMap() {
    return serverMap;
  }

  public boolean isNoDB() {
    return noDB;
  }

  public int getNodesNum() {
    return nodesNum;
  }

  public boolean isBulkLoadEnabled() {
    return bulkLoadEnabled;
  }

  public Properties getProperties() {
    return props;
  }

  public String getExpressTerracottaUrl() {
    return expressTerracottaUrl;
  }

  public TestCase getTestCase() {
    return testCase;
  }

  public int getThreadNum() {
    return threadNum;
  }

  public long getTestDuration() {
    return testDuration;
  }

  public int getReportInterval() {
    return reportInterval;
  }

  public String getCacheType() {
    return cacheType;
  }

  public String getEnvironmentType() {
    return environmentType;
  }

  public int getWritePercentage() {
    return writePercentage;
  }

  public boolean isLogMisses() {
    return logMisses;
  }

  public boolean isL1Enabled() {
    return l1Enabled;
  }

  public boolean isJtaEnabled() {
    return jtaEnabled;
  }

  public String getTransactionManager() {
    return transactionManager;
  }

  public HotSetConfiguration getHotSetConfiguration() {
    return hotSetConfiguration;
  }

  public void setHotSetConfiguration(HotSetConfiguration hotSetConfiguration) {
    this.hotSetConfiguration = hotSetConfiguration;
  }

  public int getAddOwnersPeriodInSecs() {
    return addOwnersPeriodInSecs;
  }

  public int getAddOwnersCount() {
    return addOwnersCount;
  }

  public boolean isAddOwnersEnabled() {
    return addOwnersEnabled;
  }

  @Override
  public String toString() {

    return new StringBuilder("Configuration = {")
    .append("\n  standalone \t\t= ").append(standalone)
    .append("\n  nodesNum \t\t= ").append(nodesNum)
    .append("\n  threadNum \t\t= ").append(threadNum)
    .append("\n  elementNum \t\t= ").append(elementNum)
    .append("\n  expressTerracottaUrl \t\t= ").append(expressTerracottaUrl)
    .append("\n  testCase \t\t= ").append(testCase)
    .append("\n  testDuration \t\t= ").append(testDuration)
    .append("\n  reportInterval \t= ").append(reportInterval)
    .append("\n  cacheType \t\t= ").append(cacheType)
    .append("\n  environmentType \t= ").append(environmentType)
    .append("\n  writePercentage \t= ").append(writePercentage)
    .append("\n  l1Enabled \t= ").append(l1Enabled)
    .append("\n  jtaEnabled \t= ").append(jtaEnabled)
    .append("\n  bulkLoad \t= ").append(bulkLoadEnabled)
    .append("\n  transactionManager \t= ").append(transactionManager)
    .append("\n  serverMap \t= ").append(serverMap)
    .append("\n  noDB \t= ").append(noDB)
    .append("\n}").append(hotSetConfiguration).toString();

  }

  public boolean isSearchEnabled() {
    return this.searchEnabled;
  }
}

