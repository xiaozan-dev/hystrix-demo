package uk.bluedawnsolutions.web.controllers;

import org.apache.commons.lang.time.StopWatch;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import uk.bluedawnsolutions.Application;

import java.util.OptionalDouble;
import java.util.OptionalLong;
import java.util.concurrent.*;
import java.util.function.Supplier;
import java.util.stream.IntStream;

@RunWith(SpringJUnit4ClassRunner.class)
@SpringBootTest(classes = Application.class, webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
public class HystrixControllerTest {

    private static final int TEST_EXECUTION_THREAD_POOL = 1;
    private static final int TOTAL_REQUESTS = 1;
    private static final int TOTAL_MINUTES_TO_WAIT_TO_COMPLETE_TEST = 5;
    private final ExecutorService executorService = Executors.newFixedThreadPool(TEST_EXECUTION_THREAD_POOL);

    @Autowired
    private TestRestTemplate restTemplate;

    private CountDownLatch submitCountdownLatch = new CountDownLatch(TOTAL_REQUESTS);

    private final CopyOnWriteArrayList<CallResult> testResults = new CopyOnWriteArrayList<>();

    @Test
    public void slowRunExecution() throws Exception {
        submitTasksAndAwaitCompletion(() -> new RequestRunnable("/hystrix/timeout-in-run", restTemplate));
        analyseResults("Execution is slow in run(), but fallback() returns immediately...");
    }

    @Test
    public void runFailureSlowFallbackExecution() throws Exception {
        submitTasksAndAwaitCompletion(() -> new RequestRunnable("/hystrix/delay-in-fallback", restTemplate));
        analyseResults("Execution where immediate failure occurs in run(), but fallback() takes ages...");
    }

    @Test
    public void slowRunAndSlowFallbackExecution() throws Exception {
        submitTasksAndAwaitCompletion(() -> new RequestRunnable("/hystrix/slow-run-and-slow-fallback", restTemplate));
        analyseResults("Execution is slow in run() AND but fallback() takes ages...");
    }

    @Test
    public void slowRunAndDeterministicFallbackExecution() throws Exception {
        submitTasksAndAwaitCompletion(() -> new RequestRunnable("/hystrix/slow-run-and-deterministic-fallback", restTemplate));
        analyseResults("Execution is slow in run() AND but fallback() takes a deterministic time to execute.");
    }

    @Test
    public void slowRunAndFallbackExecutionOccurringOnCallingThread() throws Exception {
        submitTasksAndAwaitCompletion(() -> new RequestRunnable("/hystrix/slow-run-and-fallback-occurring-on-calling-thread", restTemplate));
        analyseResults("Execution is slow in run() AND but fallback() occurs on the calling thread.");
    }


    private void submitTasksAndAwaitCompletion(Supplier<RequestRunnable> commandSupplier) {
        IntStream.range(0, TOTAL_REQUESTS).forEach(index -> {
            RequestRunnable requestRunnable = commandSupplier.get();
            executorService.execute(() -> {
                CallResult result = requestRunnable.call();
                testResults.add(result);
                submitCountdownLatch.countDown();
            });
        });

        try {
            submitCountdownLatch.await(TOTAL_MINUTES_TO_WAIT_TO_COMPLETE_TEST, TimeUnit.MINUTES);
        } catch (InterruptedException e) {
        }
    }

    class CallResult {
        private final long timeToExecute;
        private final HttpStatus resultHttpStatus;
        private final String body;

        CallResult(long timeToExecute, HttpStatus statusCode, String body) {
            this.timeToExecute = timeToExecute;
            resultHttpStatus = statusCode;
            this.body = body;
        }

        long getTimeToExecute() {
            return timeToExecute;
        }

        @Override
        public String toString() {
            final StringBuilder sb = new StringBuilder("CallResult{");
            sb.append(resultHttpStatus.getReasonPhrase());
            sb.append(" -> ").append(timeToExecute);
            sb.append("ms}");
            sb.append(" [result=]").append(body);
            return sb.toString();
        }
    }

    class RequestRunnable {
        private final String url;
        private final TestRestTemplate restTemplate;

        RequestRunnable(String url, TestRestTemplate restTemplate) {
            this.url = url;
            this.restTemplate = restTemplate;
        }

        CallResult call() {
            StopWatch sw = new StopWatch();
            sw.start();
            ResponseEntity<String> forEntity = restTemplate.getForEntity(url, String.class);
            sw.stop();
            return new CallResult(sw.getTime(), forEntity.getStatusCode(), forEntity.getBody());
        }

    }

    private void analyseResults(String scenario) {
        System.out.println("---------------------------------------");
        System.out.println(scenario);
        System.out.println("---------------------------------------");
        testResults.forEach(r -> System.out.println(r));
        System.out.println();
        System.out.println(String.format("Total Calls: %d", testResults.size()));
        OptionalLong min = testResults.stream().mapToLong(CallResult::getTimeToExecute).min();
        OptionalLong max = testResults.stream().mapToLong(CallResult::getTimeToExecute).max();
        OptionalDouble mean = testResults.stream().mapToLong(CallResult::getTimeToExecute).average();
        System.out.println(String.format("Min: %d  Max: %d  Mean: %f", min.orElse(0), max.orElse(0), mean.orElse(0)));
        System.out.println();
    }
}