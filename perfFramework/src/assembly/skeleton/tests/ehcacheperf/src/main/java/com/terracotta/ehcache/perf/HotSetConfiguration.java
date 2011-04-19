package com.terracotta.ehcache.perf;

import java.util.Properties;

public class HotSetConfiguration {

  private final int           hotSetDataPercent;
  private final int           hotPercent;
  private final String        hotSetDistributionType;
  private final boolean       debugDistribution;
  private final Configuration config;
  private final int           movingHotsetPeriod;

  public static HotSetConfiguration getHotSetConfig(Configuration config, Properties properties) {
    return new HotSetConfiguration(config, getStringProperty(properties, "hotset.distribution-type","simple"),
                                   getBooleanProperty(properties, "hotset.debug-distribution", true),
                                   getIntProperty(properties, "hotset.simple-distribution.hotPercent", 90),
                                   getIntProperty(properties, "hotset.simple-distribution.hotSetDataPercent", 70),
                                   getIntProperty(properties, "hotset.simple-moving-distribution.hotsetPeriodInSecs", 5)
    );
  }

  public HotSetConfiguration(Configuration config, String hotSetDistributionType, boolean debugDistribution,
                             int hotPercent, int hotSetDataPercent, int hotsetPeriod) {
    super();
    this.config = config;
    this.hotSetDistributionType = hotSetDistributionType;
    this.debugDistribution = debugDistribution;
    this.hotPercent = hotPercent;
    this.hotSetDataPercent = hotSetDataPercent;
    this.movingHotsetPeriod = hotsetPeriod;
  }

  public int getTotalDataSetSize() {
    return config.getElementNum();
  }

  public String getDistributionType() {
    return hotSetDistributionType;
  }

  public int getSimpleDistributionHotPercent() {
    return hotPercent;
  }

  public int getSimpleDistributionHotSetDataPercent() {
    return hotSetDataPercent;
  }

  public int getHotsetPeriod() {
    return movingHotsetPeriod;
  }

  public boolean isDebugDistributionEnabled() {
    return debugDistribution;
  }

  public static boolean getBooleanProperty(Properties props, String name, boolean def) {
    String result = getStringProperty(props, name, def + "");
    return Boolean.valueOf(result);
  }

  public static int getIntProperty(Properties props, String name, int def) {
    String val = getStringProperty(props, name, String.valueOf(def));
    int result = (val == null) ? def : Integer.parseInt(val);
    return result;
  }

  public static String getStringProperty(Properties props, String name, String def) {
    String result = props.getProperty(name, def).trim();
    return result;
  }

  /**
   * property must be an int between 0 and 100.
   */
  public static int getPercentageProperty(Properties props, String name, int def) {
    int p = getIntProperty(props, name, def);
    if (p < 0 || p > 100) { throw new IllegalArgumentException("not a percentage: " + p); }
    return p;
  }

  @Override
  public String toString(){
    StringBuilder sb = new  StringBuilder("HotSet Configuration = {")
    .append("\n\thotset.distribution-type =   ").append(hotSetDistributionType)
    .append("\n\thotset.debug-distribution =  ").append(debugDistribution)
    .append("\n\thotset.simple-distribution.hotPercent =  ").append(hotPercent)
    .append("\n\thotset.simple-distribution.hotSetDataPercent =   ").append(hotSetDataPercent)
    .append("\n\thotset.simple-moving-distribution.hotsetPeriodInSecs =  ").append(movingHotsetPeriod)
    .append("\n}");
    return sb.toString();
  }
}