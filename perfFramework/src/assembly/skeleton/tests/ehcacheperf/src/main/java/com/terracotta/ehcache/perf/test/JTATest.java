package com.terracotta.ehcache.perf.test;

import net.sf.ehcache.CacheManager;
import net.sf.ehcache.TransactionController;
import net.sf.ehcache.config.CacheConfiguration.TransactionalMode;
import net.sf.ehcache.transaction.manager.TransactionManagerLookup;

import org.apache.log4j.Logger;
import org.springframework.samples.petclinic.Owner;
import org.springframework.samples.petclinic.Pet;
import org.springframework.samples.petclinic.Visit;

import com.terracotta.util.StatReporter;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

import javax.transaction.TransactionManager;

public class JTATest extends AbstractTest {

  private static final Logger      log             = Logger.getLogger(JTATest.class);
  public static final String       PERFTEST        = "PERFTEST";
  private static final AtomicInteger  visits     = new AtomicInteger();

  private CacheManager cacheManager;
  private TransactionManagerLookup transactionManagerLookup;

  public void setTransactionManagerLookup(TransactionManagerLookup transactionManagerLookup) {
    this.transactionManagerLookup = transactionManagerLookup;
  }

  private void doTransaction(){
    Owner owner = clinic.getOwner(random.nextInt(maxKeyValue) + 1);
    if (doWrite()) {
      // This action now mutates the Owner and adds a visit.
      owner.setTelephone(random.nextInt(999999999) + "");
      List<Pet> pets = owner.getPets();
      // add a visit
      if (pets.size() > 0) {
        Pet pet = pets.get(0);
        Visit visit = new Visit(PERFTEST);
        int id = (Integer.MAX_VALUE / numberOfNodes * nodeId)  + visits.incrementAndGet();
        visit.setId(id);
        pet.addVisit(visit);
      } else {
        log.error("owner with no pets: " + owner.getAccount());
      }
      clinic.storeOwner(owner);
      clinic.refreshCache(owner);
    }
  }

  private void doXATransaction(){
    TransactionManager txManager = transactionManagerLookup.getTransactionManager();
    try {
      txManager.begin();
      doTransaction();
      txManager.commit();
    } catch (Exception e) {
      e.printStackTrace();
      try {
        txManager.rollback();
      } catch (Exception e1) {
        throw new RuntimeException(e1);
      }
    }
  }


  private void doLocalTransaction(){
    TransactionController controller = cacheManager.getTransactionController();
    try {
      controller.begin();
      doTransaction();
      controller.commit();
    } catch (Exception e) {
      e.printStackTrace();
      try {
        controller.rollback();
      } catch (Exception e1) {
        throw new RuntimeException(e1);
      }
    }
  }

  @Override
  public void doTestBody() {
    if (cacheManager.getCache("owners").getCacheConfiguration().getTransactionalMode().equals(TransactionalMode.LOCAL))
      doLocalTransaction();
    else
      doXATransaction();
  }

  @Override
  protected void loadOwnerKeyRange(int start, int end, boolean isL1Warmup) {
    if (cacheManager.getCache("owners").getCacheConfiguration().getTransactionalMode().equals(TransactionalMode.LOCAL))
      loadOwnerKeyRangeLocal(start, end, isL1Warmup);
    else
      super.loadOwnerKeyRange(start, end, isL1Warmup);
  }

  protected void loadOwnerKeyRangeLocal(final int start, final int end, final boolean isL1Warmup) {
    if(start == end)
      return;

    log.info("Warming cache (using local transactional mode) with keys from " + start + " to " + end);

    final AtomicLong counter = new AtomicLong();
    StatReporter reporter = new StatReporter("Loading", counter);
    reporter.untilValue(end - start);
    reporter.doSummaryReport();

    final AtomicInteger readCount = new AtomicInteger(start);

    List<Thread> workers = new ArrayList<Thread>();
    for (int i = 0; i < threadNum; i++) {
      workers.add(new Thread(new Runnable() {
        public void run() {
          TransactionController txManager = cacheManager.getTransactionController();
          int key;
          while ((key = readCount.getAndIncrement()) <= end) {
            try {
              txManager.begin();
              if(isL1Warmup)
                clinic.getOwner(key);
              else
                clinic.loadOwner(key);
              counter.incrementAndGet();
              txManager.commit();
            }
            catch (Exception e) {
              e.printStackTrace();
              try{
                txManager.rollback();
              } catch (Exception e1) {
                throw new RuntimeException(e1);
              }
            }
          }
        }
      }, "WARM UP THREAD #" + i));
    }

    reporter.startReporting();

    for (Thread worker : workers) {
      worker.start();
    }
    for (Thread worker : workers) {
      try {
        worker.join();
      } catch (InterruptedException e) {
        throw new RuntimeException(e);
      }
    }
    reporter.completeWithSummary();
  }

  public CacheManager getCacheManager() {
    return cacheManager;
  }

  public void setCacheManager(CacheManager cacheManager) {
    this.cacheManager = cacheManager;
  }
}
