package cz.hystrix.bug;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;

import java.util.concurrent.atomic.AtomicBoolean;

import org.junit.Test;
import org.reactivestreams.Publisher;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import rx.Observable;
import rx.RxReactiveStreams;

public class ObservableTest {

    @Test
    public void testFlux() throws InterruptedException {
        doTest(false);
    }

    @Test
    public void testMono() throws InterruptedException {
        doTest(true);
    }

    public void doTest(boolean useMono) throws InterruptedException {
        AtomicBoolean bool = new AtomicBoolean(false);

        Observable<Integer> observable = Observable.just(1)
                .doOnCompleted(() -> bool.set(true))
                .map(i -> i * 10);

        Publisher<Integer> publisher = RxReactiveStreams.toPublisher(observable);

        Mono<Integer> mono = useMono ? Mono.from(publisher) : Flux.from(publisher).singleOrEmpty();
        int res = mono
                .map(i -> i * 10)
                .block();

        assertThat(res, is(100));
        assertTrue("OnCompleted handler has not been called", bool.get());
    }

}
