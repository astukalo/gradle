/*
 * Copyright 2016 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.gradle.launcher.daemon.server;

import com.google.common.base.Function;
import com.google.common.base.Functions;
import com.google.common.base.Preconditions;
import org.gradle.api.logging.Logger;
import org.gradle.api.logging.Logging;

import java.util.concurrent.TimeUnit;

import static org.gradle.launcher.daemon.server.DaemonExpirationStatus.QUIET_EXPIRE;

public class DaemonIdleTimeoutExpirationStrategy implements DaemonExpirationStrategy {
    private static final Logger LOG = Logging.getLogger(DaemonIdleTimeoutExpirationStrategy.class);
    private Function<?, Long> idleTimeout;
    private final Daemon daemon;

    public DaemonIdleTimeoutExpirationStrategy(Daemon daemon, int idleTimeout, TimeUnit timeUnit) {
        this(daemon, Functions.constant(timeUnit.toMillis(idleTimeout)));
    }

    public DaemonIdleTimeoutExpirationStrategy(Daemon daemon, Function<?, Long> timeoutClosure) {
        this.daemon = daemon;
        this.idleTimeout = Preconditions.checkNotNull(timeoutClosure);
    }

    @Override
    public DaemonExpirationResult checkExpiration() {
        long idleMillis = daemon.getStateCoordinator().getIdleMillis(System.currentTimeMillis());
        boolean idleTimeoutExceeded = idleMillis > idleTimeout.apply(null);
        if (idleTimeoutExceeded) {
            LOG.info("Idle timeout: daemon has been idle for {} milliseconds. Expiring.", idleMillis);
            return new DaemonExpirationResult(QUIET_EXPIRE, "daemon has been idle for " + idleMillis + " milliseconds");
        }
        return DaemonExpirationResult.NOT_TRIGGERED;
    }
}
