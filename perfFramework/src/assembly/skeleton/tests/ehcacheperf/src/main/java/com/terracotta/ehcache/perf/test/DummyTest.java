package com.terracotta.ehcache.perf.test;

import org.apache.log4j.Logger;

/**
 * @author Alex Snaps
 */
public class DummyTest extends AbstractTest {

  static final Logger log = Logger.getLogger(DummyTest.class);

  public void doTestBody() {
    log.warn("Dummy test: probably the testCase property was not set...");
  }
}
