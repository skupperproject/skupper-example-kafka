/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package net.example;

import io.quarkus.runtime.QuarkusApplication;
import io.quarkus.runtime.annotations.QuarkusMain;
import jakarta.enterprise.context.ApplicationScoped;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import org.eclipse.microprofile.reactive.messaging.Channel;
import org.eclipse.microprofile.reactive.messaging.Emitter;
import org.eclipse.microprofile.reactive.messaging.Incoming;

@ApplicationScoped
@QuarkusMain
public class Client implements QuarkusApplication {
    static int desired = 10;
    static CountDownLatch completion = new CountDownLatch(desired);

    @Channel("outgoing-messages")
    Emitter<String> emitter;

    @Incoming("incoming-messages")
    public void receive(String message) {
        System.out.println("Received " + message);
        completion.countDown();
    }

    @Override
    public int run(String... args) {
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

            return 0;
        } catch (Exception e) {
            System.out.println("Result: Error!");
            e.printStackTrace();
            return 1;
        }
    }
}
