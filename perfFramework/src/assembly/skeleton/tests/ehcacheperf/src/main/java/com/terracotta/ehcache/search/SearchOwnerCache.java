/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.terracotta.ehcache.search;

import net.sf.ehcache.Cache;
import net.sf.ehcache.search.Attribute;
import net.sf.ehcache.search.expression.And;
import net.sf.ehcache.search.expression.Criteria;

public class SearchOwnerCache extends SearchCache {

  private final Attribute<Integer> account;
  private final Attribute<Integer> petCount;
  private final Attribute<String> address;
  private final Attribute<String> telephone;
  private final Attribute<String> lastName;
  private final Attribute<String> firstName;
  private final Attribute<String> city;

  public SearchOwnerCache(Cache owners) {
    super(owners);
    account = owners.getSearchAttribute("account");
    petCount = owners.getSearchAttribute("petCount");
    address = owners.getSearchAttribute("address");
    telephone = owners.getSearchAttribute("telephone");
    city = owners.getSearchAttribute("city");
    lastName = owners.getSearchAttribute("lastName");
    firstName = owners.getSearchAttribute("firstName");

    smallQuery1.addCriteria(new And(account.between(10, 1700),petCount.between(0,3)));
    smallQuery1.includeAggregator(account.min(), account.max(), account.average());
    smallQuery1.includeAggregator(petCount.average(), petCount.min(),petCount.max());
    smallQuery1.includeAggregator(telephone.count());

    Criteria criteria = new And(account.between(0, 1230),lastName.ilike("Owner_Last_Name_"));
    criteria.or(city.eq("City Name 0")).or(city.eq("City Name 1")).or(city.eq("City Name 2")).or(city.eq("City Name 3"));
    smallQuery2.addCriteria(criteria);

    smallQuery2.includeAggregator(account.min(), account.max(), account.average());
    smallQuery2.includeAggregator(petCount.average(), petCount.min(),petCount.max());
    smallQuery2.includeAggregator(telephone.count());

    hugeQuery1.addCriteria(account.gt(10));
    Criteria or1 = address.ilike("Street Number 1*");
    for (int i = 1; i < 50; i += 2) {
      or1 = or1.or(address.ilike("Street Number " + i + "*"));
    }
    hugeQuery1.addCriteria(or1);

    hugeQuery1.includeAggregator(account.min(), account.max(), account.average());
    hugeQuery1.includeAggregator(petCount.average(), petCount.min(),petCount.max());
    hugeQuery1.includeAggregator(telephone.count());

    Criteria or = firstName.ilike("Owner_First_Name_1");
    for (int i = 2; i < 2000; i += 2) {
      or = or.or(firstName.ilike("Owner_First_Name_" + i));
    }
    hugeQuery2.addCriteria(or);

    hugeQuery2.includeAggregator(account.min(), account.max(), account.average());
    hugeQuery2.includeAggregator(petCount.average(), petCount.min(),petCount.max());
    hugeQuery2.includeAggregator(telephone.count());
  }
}
