package com.terracotta.ehcache.perf.test;

import net.sf.ehcache.Ehcache;

import org.apache.log4j.Logger;
import org.springframework.samples.petclinic.ClinicJdbcImpl;
import org.springframework.samples.petclinic.Owner;
import org.springframework.samples.petclinic.Pet;
import org.springframework.samples.petclinic.Visit;

import com.terracotta.ehcache.perf.FakeWriteBehindFactory;

import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * @author Alex Snaps
 */
public class ReadWriteBehindTest extends PartitionedReadWriteTest {

  private static final Logger log            = Logger.getLogger(ReadWriteBehindTest.class);
  private static final AtomicInteger  visits     = new AtomicInteger();
  private long                start;
  private long                lastRun        = 0;
  private long                previousPeriod = 0;
  private Ehcache ownerCache;

  @Override
  public void doL2WarmUp() {
    ownerCache = ((ClinicJdbcImpl)clinic).getCacheManager().getEhcache("owners");
    start = System.nanoTime();
    super.doL2WarmUp();
  }

  @Override
  public void doTestBody() {
    int key = partitionStart + this.random.nextInt((partitionEnd - partitionStart));
    // log.info("Key = " + key + " partitionStart# " + partitionStart + " partitionEnd# " + partitionEnd );
    Owner owner = clinic.loadOwner(key + 1);
    if (owner == null) { throw new RuntimeException("Key '" + key + "' has no value in the cache"); }
    // This action now mutates the Owner and adds a visit.
    if (doWrite()) {
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
    }
    clinic.storeOwner(owner);
    clinic.refreshCache(owner);
  }

  @Override
  public void doPeriodReport() {
    long total = FakeWriteBehindFactory.counter.get();
    long now = System.nanoTime();
    log.info("Period avg. writer throughput: " + (total - lastRun)
             / (float) TimeUnit.NANOSECONDS.toSeconds(now - previousPeriod));
    log.info("Total avg. writer throughput: " + total / (float) TimeUnit.NANOSECONDS.toSeconds(now - start));

    // TODO: Enabled this stats when write-behind changes are in trunk
    if(ownerCache.isStatisticsEnabled()){
      //      log.info("Length of the write-behind queue: " + ownerCache.getStatistics().getWriterQueueSize());
    }
    lastRun = total;
    previousPeriod = now;
  }
}
