package org.tc.cluster.watcher.util;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;

import org.apache.commons.io.IOUtils;

public class StreamCopier extends Thread {

  private final OutputStream   out;
  private final BufferedReader reader;

  public StreamCopier(InputStream stream, OutputStream out) {
    if ((stream == null) || (out == null)) {
      throw new AssertionError("null streams not allowed");
    }

    reader = new BufferedReader(new InputStreamReader(stream));
    this.out = out;

    setName("Stream Copier");
    setDaemon(true);
  }

  public void run() {
    final String newLine = System.getProperty("line.separator", "\n");
    String line;
    try {
      while ((line = reader.readLine()) != null) {
        line += newLine;
        out.write(line.getBytes());
        out.flush();
      }
    } catch (IOException ioe) {
      ioe.printStackTrace();
    } finally {
      IOUtils.closeQuietly(reader);
    }
  }
}
