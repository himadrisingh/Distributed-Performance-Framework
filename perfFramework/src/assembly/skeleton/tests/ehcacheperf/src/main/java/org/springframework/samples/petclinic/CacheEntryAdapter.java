package org.springframework.samples.petclinic;

/**
 * @author Alex Snaps
 */
public interface CacheEntryAdapter<T> {

  T hydrate(Object[] data);

  Object[] dehydrate(T value);
}
