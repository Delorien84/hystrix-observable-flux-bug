package cz.hystrix.bug;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

import java.time.Duration;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import org.junit.Test;
import org.springframework.cloud.netflix.hystrix.HystrixCommands;
import org.springframework.cloud.netflix.hystrix.HystrixCommands.PublisherBuilder;

import com.netflix.hystrix.HystrixCommandKey;
import com.netflix.hystrix.HystrixCommandMetrics;
import com.netflix.hystrix.HystrixCommandProperties;
import com.netflix.hystrix.HystrixEventType;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public class HystrixBugTests {

    @Test
    public void testFlux() throws InterruptedException {
        // this test pass
        doTest(false);
    }

    @Test
    public void testMono() throws InterruptedException {
        // this test does not pass
        doTest(true);
    }

    private void doTest(boolean withMono) throws InterruptedException {
        String cmdName = withMono ? "mono" : "flux";

        HystrixCommands.from(Mono.just(1)).commandName(cmdName).toFlux().singleOrEmpty().block();
        HystrixCommandMetrics metrics = HystrixCommandMetrics.getInstance(HystrixCommandKey.Factory.asKey(cmdName));
        System.out.println(metrics.getRollingCount(HystrixEventType.SUCCESS)); // we have to call this to initialize the metrics correctly
        Thread.sleep(10000); // sleep rolling windows, to get rid of this event.

        List<Integer> data = Arrays.asList(1, 2, 3, 4, 5);
        CountDownLatch latch = new CountDownLatch(data.size());

        Flux
                .fromIterable(data)
                .delayElements(Duration.ofMillis(200))
                .flatMap(i -> {
                    PublisherBuilder<Integer> builder = HystrixCommands
                            .from(Mono.just(i)
                                    .map(x -> x * 10)
                                    .delayElement(Duration.ofMillis(200)))
                            .commandName(cmdName)
                            .commandProperties(HystrixCommandProperties.defaultSetter().withExecutionTimeoutEnabled(false));
                    return withMono ? builder.toMono() : builder.toFlux().singleOrEmpty();
                })
                .delayElements(Duration.ofMillis(200))
                // .repeat()
                .subscribe(x -> {
                    System.out.println(x);
                    latch.countDown();
                });
        latch.await(20, TimeUnit.SECONDS);
        Thread.sleep(1000);
        assertThat(metrics.getRollingCount(HystrixEventType.SUCCESS), is((long) data.size()));

    }

}
