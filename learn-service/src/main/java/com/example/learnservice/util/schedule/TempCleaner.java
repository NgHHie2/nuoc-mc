package com.example.learnservice.util.schedule;

import java.io.File;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@EnableScheduling
public class TempCleaner {

    @Value("${temp.cleanup.interval:1800000}") // 30 phút default
    private long cleanupInterval;

    @Value("${temp.dir:D:/temp}")
    private String tempDir;

    @Scheduled(fixedRateString = "${temp.cleanup.interval:1800000}")
    public void cleanup() {
        File[] files = new File(tempDir).listFiles();
        if (files != null) {
            for (File f : files)
                f.delete();
        }
    }
}