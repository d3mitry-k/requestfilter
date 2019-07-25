package com.test;

import java.time.LocalDateTime;
import java.util.Deque;
import java.util.Iterator;
import java.util.List;
import java.util.Optional;
import java.util.Random;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

public class RequestTokenThresholdFilter {

    private final int tokenThreshold;
    private final int frameSeconds;
    private final ClockService clockService;

    final ConcurrentMap<String, AtomicInteger> tokensMap = new ConcurrentHashMap<>();
    final Deque<Entry> inputDeque = new ConcurrentLinkedDeque<>();

    public RequestTokenThresholdFilter(int tokenThresholdCount, int frameSeconds, ClockService clockService) {
        this.tokenThreshold = tokenThresholdCount;
        this.frameSeconds = frameSeconds;
        this.clockService = clockService;
    }

    public boolean filter(final String token) {
        inputDeque.addFirst(new Entry(token, clockService.currentTimeMillis() + frameSeconds * 1000L));

        tokensMap.putIfAbsent(token, new AtomicInteger());
        tokensMap.get(token).incrementAndGet();

        cleanOldValues();

        return Optional.ofNullable(tokensMap.get(token))
                .map(count -> count.get() <= tokenThreshold)
                .orElse(false);
    }

    private synchronized void cleanOldValues() {
        while (true) {
            Iterator<Entry> descendingIterator = inputDeque.descendingIterator();
            if (descendingIterator.hasNext()) {
                Entry next = descendingIterator.next();
                if (next.getExpiredTimeMs() < clockService.currentTimeMillis()) {
                    String token = next.getToken();
                    AtomicInteger atomicInteger = tokensMap.get(token);
                    if (atomicInteger != null && atomicInteger.decrementAndGet() == 0) {
                        tokensMap.remove(token);
                    }
                    descendingIterator.remove();
                } else {
                    break;
                }
            } else {
                break;
            }
        }
    }

    public static void main(String[] args) {
        int tokenThreshold = 5;
        int frameSeconds = 1;
        int tokensCount = 50;
        int maxSleepInterval = 50;
        int nThreads = 5;

        RequestTokenThresholdFilter requestTokenThresholdFilter = new RequestTokenThresholdFilter(tokenThreshold, frameSeconds, new ClockService());
        ExecutorService executorService = Executors.newFixedThreadPool(nThreads);
        List<String> tokens = Stream.generate(() -> UUID.randomUUID().toString()).limit(tokensCount).collect(Collectors.toList());

        IntStream.range(0, 20000)
                .mapToObj($ -> CompletableFuture.runAsync(() -> {
                    String currentToken = tokens.get(new Random().nextInt(tokensCount));
                    try {
                        TimeUnit.MILLISECONDS.sleep(new Random().nextInt(maxSleepInterval));
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                    boolean filter = requestTokenThresholdFilter.filter(currentToken);
                    System.out.println(LocalDateTime.now() + " : " + Thread.currentThread().getName() + " , token: " + currentToken + ", filtered: " + filter);
                }, executorService))
                .collect(Collectors.toList());

        executorService.shutdown();
    }

    class Entry {
        private String token;
        private Long expiredTimeMs;

        public Entry(String token, Long expiredTimeMs) {
            this.token = token;
            this.expiredTimeMs = expiredTimeMs;
        }

        public String getToken() {
            return token;
        }

        public Long getExpiredTimeMs() {
            return expiredTimeMs;
        }
    }
}
