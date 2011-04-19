/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.terracotta.ehcache.search;

import net.sf.ehcache.Cache;
import net.sf.ehcache.search.Attribute;
import net.sf.ehcache.search.Direction;

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;

public class SearchPetCache extends SearchCache {

  private final Attribute<Integer> visitCount;
  private final Attribute<Date> birthDate;

  public SearchPetCache(Cache pets){
    super(pets);

    visitCount = pets.getSearchAttribute("visitCount");
    birthDate = pets.getSearchAttribute("birthDate");
    DateFormat fmt = new SimpleDateFormat("yyyy/MM/dd");

    try {
      smallQuery1.addCriteria(visitCount.ge(2));
      smallQuery1.addCriteria(birthDate.between(fmt.parse("2007/03/01"), fmt.parse("2008/09/01")));
      smallQuery1.includeAggregator(birthDate.count());
      smallQuery1.includeAggregator(visitCount.average(), visitCount.min(), visitCount.count(), visitCount.sum());

      smallQuery2.addCriteria(visitCount.lt(4)).addOrderBy(birthDate, Direction.ASCENDING);
      smallQuery2.addCriteria(birthDate.between(fmt.parse("2007/03/01"), fmt.parse("2008/09/01")));
      smallQuery2.includeAggregator(birthDate.count());
      smallQuery2.includeAggregator(visitCount.average(), visitCount.min(), visitCount.count(), visitCount.sum());


      hugeQuery1.addCriteria(birthDate.between(fmt.parse("2008/01/01"), fmt.parse("2008/12/01")));
      hugeQuery1.includeAttribute(visitCount);
      hugeQuery1.includeAggregator(birthDate.count());
      hugeQuery1.includeAggregator(visitCount.average(), visitCount.min(), visitCount.count(), visitCount.sum());

      hugeQuery2.includeAggregator(birthDate.count());
      hugeQuery2.includeAggregator(visitCount.average(), visitCount.min(), visitCount.count(), visitCount.sum());
      hugeQuery2.addCriteria(visitCount.ge(0));

    } catch (ParseException e1) {
      // swallow;
    }

  }

}
