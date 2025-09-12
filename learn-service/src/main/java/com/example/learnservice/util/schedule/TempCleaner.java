package com.example.learnservice.util.schedule;

import java.io.File;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
@EnableScheduling
public class TempCleaner {

    @Value("${temp.cleanup.interval:1800000}") // 30 phút default
    private long cleanupInterval;

    @Value("${temp.cleanup.min-age:1700000}")
    private long minAgeMillis;

    @Value("${temp.dir:D:/temp}")
    private String tempDir;

    @Scheduled(fixedRateString = "${temp.cleanup.interval:1800000}")
    public void cleanup() {
        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")
                .withZone(ZoneId.systemDefault());
        File dir = new File(tempDir);
        File[] files = dir.listFiles();
        if (files == null)
            return;

        long now = System.currentTimeMillis();
        for (File f : files) {
            if (!f.isFile())
                continue;

            long age = now - f.lastModified();
            long last = f.lastModified();
            String formatted = fmt.format(Instant.ofEpochMilli(last));
            log.info("File: {}, lastModified: {}", f.getName(), formatted);
            if (age > minAgeMillis) {
                log.info("Deleting {} (age {} ms)", f.getName(), age);
                f.delete();
            }
        }
    }
}