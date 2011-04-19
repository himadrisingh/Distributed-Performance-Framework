package com.terracotta.util;

public abstract class Util {

  public static String formatTimeInSecondsToWords(long timeInSeconds) {
    StringBuilder sb = new StringBuilder();
    int tmp = (int) (timeInSeconds / 3600);
    timeInSeconds -= tmp * 3600;
    if (tmp > 0) sb.append(tmp).append(" Hours ");
    tmp = (int) (timeInSeconds / 60);
    timeInSeconds -= tmp * 60;
    if (tmp > 0) sb.append(tmp).append(" Minutes ");
    sb.append((int) timeInSeconds).append(" Seconds");
    return sb.toString();
  }

}
