package com.terracotta.util;

/**
 * A utility to do range iteration, splitting, and subtraction. A range is always inclusive and min=max is a range of 1
 * number.
 *
 * @author pat
 */
public class RangeUtil {
  private long next, min, max;

  /**
   * @param min inclusive
   * @param max inclusive
   */
  public RangeUtil(long min, long max) {
    if (min < 0 || max < min) { throw new IllegalArgumentException("invalid args"); }
    this.min = min;
    this.max = max;
    this.next = min;
  }

  /**
   * Divid the range into totalSubranges sub-ranges and return a new subrange indicated by subrangeXOf. e.g. for a range
   * of 1-10 subrange(0, 2) would yield 1-5 and subrange(1,2) would yield 6-10; Note that for uneven division the final
   * sub-range may be of a differenet size than the previous.
   *
   * @param subrangeXOf a value from 0 to totalSubranges-1 indiciating which subdivision of the range is desired.
   * @param totalSubranges the number of partitions of the range
   * @return
   */
  public RangeUtil subrange(int subrangeXOf, int totalSubranges) {
    if (subrangeXOf >= totalSubranges || subrangeXOf < 0) { throw new IllegalArgumentException("illegal subrange"); }
    long subSize = size() / totalSubranges;
    long subMin = min + subrangeXOf * subSize;
    long subMax = Math.min(max, subMin + subSize - 1);
    // if last range, make sure we include max
    if (subrangeXOf == totalSubranges - 1) {
      subMax = max;
    }
    return new RangeUtil(subMin, subMax);
  }

  /**
   * Subtract the specified range from this range and return 1 or 2 sub-ranges covering the remaining space. If the
   * subtracted range coincides with the min or max value then a single range will be returned, otherwise the 2 disjoint
   * remaining ranges of values will be returned.
   *
   * @param toSubtract no values in the toSubtract range will appear in the new range(s).
   */
  public RangeUtil[] subtract(RangeUtil toSubtract) {
    // subtracted range must be within range
    if (toSubtract.min < min || toSubtract.max > max) { throw new IllegalArgumentException(
                                                                                           "range to subtract is not a sub-range"); }

    // subtracted range is entire range
    if (toSubtract.min == min && toSubtract.max == max) { throw new IllegalArgumentException("result is empty range"); }

    // subtracted range smaller than entire range and starts at min, return top
    if (toSubtract.min == min) { return new RangeUtil[] { new RangeUtil(toSubtract.max + 1, max) }; }

    // subtracted range smaller than entire range and ends at max, return bottom
    if (toSubtract.max == max) { return new RangeUtil[] { new RangeUtil(min, toSubtract.min - 1) }; }

    // subtracted range strictly in the middle somewhere, return bottom and top
    return new RangeUtil[] { new RangeUtil(min, toSubtract.min - 1), new RangeUtil(toSubtract.max + 1, max) };
  }

  public boolean hasNext() {
    return next <= max;
  }

  public long next() {
    if (!hasNext()) { throw new IllegalStateException("exceeded range"); }
    return next++;
  }

  /**
   * return the size including the min and max.
   */
  public long size() {
    return max - min + 1;
  }

  public void reset() {
    next = min;
  }

  public long getMin() {
    return min;
  }

  public long getMax() {
    return max;
  }

  @Override
  public String toString() {
    return "RangeUtil{" + "min=" + min + ", max=" + max + ", next=" + next + '}';
  }

}