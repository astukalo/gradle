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
package org.gradle.launcher.daemon.server

import com.google.common.base.Function
import spock.lang.Specification

import javax.annotation.Nullable
import java.util.concurrent.TimeUnit

import static org.gradle.launcher.daemon.server.DaemonExpirationStatus.*

class DaemonIdleTimeoutExpirationStrategyTest extends Specification {
    final Daemon daemon = Mock(Daemon)
    final DaemonStateCoordinator daemonStateCoordinator = Mock(DaemonStateCoordinator)

    def "daemon should expire when its idle time exceeds idleTimeout"() {
        given:
        DaemonIdleTimeoutExpirationStrategy expirationStrategy = new DaemonIdleTimeoutExpirationStrategy(daemon, 100, TimeUnit.MILLISECONDS)

        when:
        1 * daemon.getStateCoordinator() >> { daemonStateCoordinator }
        1 * daemonStateCoordinator.getIdleMillis(_) >> { 101L }

        then:
        DaemonExpirationResult result = expirationStrategy.checkExpiration()
        result.status == QUIET_EXPIRE
        result.reason == "daemon has been idle for 101 milliseconds"
    }

    def "daemon accepts idle timeout closure"() {
        given:
        DaemonIdleTimeoutExpirationStrategy expirationStrategy = new DaemonIdleTimeoutExpirationStrategy(daemon, new Function<Void, Long>() {
            private long numTimesCalled = 0;

            @Override
            Long apply(@Nullable Void input) {
                return ++numTimesCalled
            }
        })

        when:
        _ * daemon.getStateCoordinator() >> { daemonStateCoordinator }
        _ * daemonStateCoordinator.getIdleMillis(_) >> { 2L }

        then:
        DaemonExpirationResult firstResult = expirationStrategy.checkExpiration()
        firstResult.status == QUIET_EXPIRE
        firstResult.reason == "daemon has been idle for 2 milliseconds"

        DaemonExpirationResult secondResult = expirationStrategy.checkExpiration()
        secondResult.status == DO_NOT_EXPIRE

        DaemonExpirationResult thirdResult = expirationStrategy.checkExpiration()
        thirdResult.status == DO_NOT_EXPIRE
    }
}
