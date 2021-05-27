/**
 * Copyright (C) 2015 The Gravitee team (http://gravitee.io)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.gravitee.gateway.standalone.websocket;

import io.gravitee.common.http.HttpStatusCode;
import io.gravitee.gateway.standalone.junit.annotation.ApiDescriptor;
import io.gravitee.gateway.standalone.junit.rules.ApiDeployer;
import io.vertx.core.Vertx;
import io.vertx.core.http.HttpClient;
import io.vertx.core.http.HttpClientOptions;
import io.vertx.core.http.HttpServer;
import io.vertx.core.http.UpgradeRejectedException;
import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.RuleChain;
import org.junit.rules.TestRule;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

@ApiDescriptor("/io/gravitee/gateway/standalone/websocket/teams.json")
public class WebsocketRejectTest extends AbstractWebSocketGatewayTest {

    @Rule
    public final TestRule chain = RuleChain.outerRule(new ApiDeployer(this));

    @Test
    public void websocket_rejected_request() throws InterruptedException {
        Vertx vertx = Vertx.vertx();

        HttpServer httpServer = vertx.createHttpServer();
        httpServer
                .webSocketHandler(webSocket -> webSocket.reject(HttpStatusCode.UNAUTHORIZED_401))
                .listen(16664);

        // Wait for result
        final CountDownLatch latch = new CountDownLatch(1);

        HttpClient httpClient = vertx.createHttpClient(new HttpClientOptions()
                .setDefaultPort(8082)
                .setDefaultHost("localhost"));

        httpClient.webSocket("/test", event -> {
            Assert.assertTrue(event.failed());
            Assert.assertEquals(UpgradeRejectedException.class, event.cause().getClass());
            Assert.assertEquals(HttpStatusCode.UNAUTHORIZED_401, ((UpgradeRejectedException) event.cause()).getStatus());

            latch.countDown();
        });

        Assert.assertTrue(latch.await(10, TimeUnit.SECONDS));
        httpServer.close();
    }
}
