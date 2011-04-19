/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.terracotta.ehcache.search;

import net.sf.ehcache.Cache;
import net.sf.ehcache.search.Attribute;
import net.sf.ehcache.search.expression.And;
import net.sf.ehcache.search.expression.Criteria;
import net.sf.ehcache.search.expression.Or;

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;

public class SearchVisitCache extends SearchCache {

  private final Attribute<Date> date;
  private final Attribute<String> visitDescription;

  public SearchVisitCache(Cache visits) {
    super(visits);
    date = visits.getSearchAttribute("date");
    visitDescription = visits.getSearchAttribute("description");
    DateFormat fmt = new SimpleDateFormat("yyyy/MM/dd");
    try {
      smallQuery1.addCriteria(new And(date.between(fmt.parse("2005/01/01"), fmt.parse("2008/01/01")),visitDescription.eq("PROCEDURE_3")));
      smallQuery1.includeAggregator(visitDescription.count());
      smallQuery1.includeAggregator(date.min(), date.max());
    } catch (ParseException e) {
      e.printStackTrace();
    }

    smallQuery2.addCriteria(new Or(visitDescription.eq("PROCEDURE_6"),visitDescription.eq("PROCEDURE_2")));
    smallQuery2.includeAggregator(visitDescription.count());
    smallQuery2.includeAggregator(date.min(), date.max());

    Criteria criteria = visitDescription.eq("PERFTEST");
    for (int i = 1 ; i <= 20 ; i+=2)
      criteria = criteria.or(visitDescription.eq("PROCEDURE_" + i));
    hugeQuery1.addCriteria(criteria);
    hugeQuery1.includeAggregator(visitDescription.count());
    hugeQuery1.includeAggregator(date.min(), date.max());

    try {
      Criteria crit = date.between(fmt.parse("2004/05/01"), fmt.parse("2005/06/01"));
      for (int i = 2005; i < 2009; i++){
        for (int j = 1; j < 12; j++)
          crit = crit.or(date.between(fmt.parse(i + "/" + j + "/01"), fmt.parse(i + "/" + (j + 1) + "/01")));
      }

      hugeQuery2.addCriteria(crit);
      hugeQuery2.includeAggregator(visitDescription.count());
      hugeQuery2.includeAggregator(date.min(), date.max());

    } catch (ParseException e) {
      e.printStackTrace();
    }
  }

}
