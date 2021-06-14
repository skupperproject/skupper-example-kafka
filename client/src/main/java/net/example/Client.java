package net.example;

import io.quarkus.runtime.QuarkusApplication;
import io.quarkus.runtime.annotations.QuarkusMain;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import org.eclipse.microprofile.reactive.messaging.Channel;
import org.eclipse.microprofile.reactive.messaging.Emitter;
import org.eclipse.microprofile.reactive.messaging.Incoming;

@ApplicationScoped
@QuarkusMain
public class Client implements QuarkusApplication {
    @Inject
    @Channel("outgoing-messages")
    Emitter<String> emitter;

    int desired = 10;
    CountDownLatch completion = new CountDownLatch(desired);

    @Override
    public int run(String... args) throws Exception {
        java.lang.Thread.sleep(3000);

        try {
            for (int i = 1; i <= desired; i++) {
                String message = "message " + i;

                emitter.send(message);

                System.out.println("Sent " + message);
            }

            boolean completed = completion.await(30, TimeUnit.SECONDS);

            if (!completed) {
                System.out.println("Result: Timed out!");
                System.exit(1);
            }

            System.out.println("Result: OK");
        } catch (Exception e) {
            System.out.println("Result: Error!");
            e.printStackTrace();
            System.exit(1);
        }

        return 0;
    }

    @Incoming("incoming-messages")
    public void receive(String message) {
        System.out.println("Received " + message);
        completion.countDown();
    }
}
