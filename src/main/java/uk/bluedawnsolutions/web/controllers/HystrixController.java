package uk.bluedawnsolutions.web.controllers;

import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;
import uk.bluedawnsolutions.hystrix.*;

import java.util.concurrent.ExecutionException;

@RestController
@RequestMapping("/hystrix")
@Slf4j
public class HystrixController {

    @RequestMapping(value = "/delay-in-fallback", method = RequestMethod.GET)
    @ResponseBody
    public String delayinFallback() throws ExecutionException, InterruptedException {
        log.info("inside controller");
        return new QuickRunFailureSlowFallbackCommand().queue().get();
    }

    @RequestMapping(value = "/timeout-in-run", method = RequestMethod.GET)
    @ResponseBody
    public String timeoutInDoRun() throws ExecutionException, InterruptedException {
        log.info("inside controller");
        return new SlowRunAndImmediateFallbackCommand().queue().get();
    }

    @RequestMapping(value = "/slow-run-and-slow-fallback", method = RequestMethod.GET)
    @ResponseBody
    public String slowExecutionAndFallback() throws ExecutionException, InterruptedException {
        log.info("inside controller");
        return new SlowRunAndSlowFallbackCommand().queue().get();
    }

    @RequestMapping(value = "/slow-run-and-deterministic-fallback", method = RequestMethod.GET)
    @ResponseBody
    public String slowExecutionAndDeterministicFallback() throws ExecutionException, InterruptedException {
        log.info("inside controller");
        return new SlowRunAndDeterministicFallbackCommand().queue().get();
    }

    @RequestMapping(value = "/slow-run-and-fallback-occurring-on-calling-thread", method = RequestMethod.GET)
    @ResponseBody
    public String slowExecutionAndFallbackOccurringOnCallingThread() throws ExecutionException, InterruptedException {
        log.info("inside controller");
        return new SlowRunAndFallbackExecutedOnCallingThreadCommand().queue().get().get();
    }

}