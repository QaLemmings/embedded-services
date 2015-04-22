package ru.yandex.qatools.embed.service;

import de.flapdoodle.embed.process.io.IStreamProcessor;

import java.util.Set;

/**
 * @author Ilya Sadykov
 */
public class LogWatchStreamProcessor extends de.flapdoodle.embed.process.io.LogWatchStreamProcessor {
    private final Object mutex = new Object();
    private final String success;
    private final Set<String> failures;
    private volatile boolean found = false;

    public LogWatchStreamProcessor(String success, Set<String> failures, IStreamProcessor destination) {
        super(success, failures, destination);
        this.success = success;
        this.failures = failures;
    }

    @Override
    public void process(String block) {
        if (containsSuccess(block) || containsFailure(block)) {
            synchronized (mutex) {
                found = true;
                mutex.notifyAll();
            }
        } else {
            super.process(block);
        }
    }

    private boolean containsSuccess(String block) {
        return block.contains(success);
    }

    private boolean containsFailure(String block) {
        for (String failure : failures) {
            if (block.contains(failure)) {
                return true;
            }
        }
        return false;
    }

    public void waitForResult(long timeout) {
        synchronized (mutex) {
            try {
                while (!found) {
                    mutex.wait(timeout);
                }
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }
}
