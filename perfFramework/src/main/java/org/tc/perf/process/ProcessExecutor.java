package org.tc.perf.process;

import org.apache.log4j.Logger;

public class ProcessExecutor {

    private static final Logger log = Logger.getLogger(ProcessExecutor.class);
    private final ProcessThread process;

    public ProcessExecutor(final ProcessConfig config) {
        this.process = new ProcessThread(config);
    }

    private void execute(){
        new Thread(process).start();
        try {
            Thread.sleep(2000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    public boolean waitFor(final long timeout){
        log.info("Starting process.");
        execute();
        log.info(String.format("Timeout set to %d secs.", timeout));
        boolean bool = process.isStarted();
        while (!bool){
            try {
                Thread.sleep(500);
                bool = process.isStarted();
            } catch (InterruptedException e) {
                e.printStackTrace();
                bool = false;
            }
        }
        return Boolean.TRUE;
    }
}
