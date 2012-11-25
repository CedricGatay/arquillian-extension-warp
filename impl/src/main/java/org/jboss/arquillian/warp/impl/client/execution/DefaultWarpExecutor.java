/**
 * JBoss, Home of Professional Open Source
 * Copyright 2012, Red Hat Middleware LLC, and individual contributors
 * by the @authors tag. See the copyright.txt in the distribution for a
 * full listing of individual contributors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * http://www.apache.org/licenses/LICENSE-2.0
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.jboss.arquillian.warp.impl.client.execution;

import org.jboss.arquillian.core.api.Event;
import org.jboss.arquillian.core.api.annotation.Inject;
import org.jboss.arquillian.test.spi.TestResult;
import org.jboss.arquillian.warp.ClientAction;
import org.jboss.arquillian.warp.client.result.WarpResult;
import org.jboss.arquillian.warp.exception.ClientWarpExecutionException;
import org.jboss.arquillian.warp.exception.ServerWarpExecutionException;
import org.jboss.arquillian.warp.impl.client.event.AdvertiseEnrichment;
import org.jboss.arquillian.warp.impl.client.event.AwaitResponse;
import org.jboss.arquillian.warp.impl.client.event.CleanEnrichment;
import org.jboss.arquillian.warp.impl.client.event.FinishEnrichment;
import org.jboss.arquillian.warp.impl.client.execution.DefaultRequestExecutor.ClientActionException;

public class DefaultWarpExecutor implements WarpExecutor {

    @Inject
    private Event<AdvertiseEnrichment> advertiseEnrichment;

    @Inject
    private Event<FinishEnrichment> finishEnrichment;

    @Inject
    private Event<CleanEnrichment> cleanEnrichment;

    @Inject
    private Event<AwaitResponse> awaitResponse;

    @Inject
    private Event<ClientAction> executeClientAction;
    
    private RuntimeException actionException;

    @Override
    public WarpResult execute(ClientAction action, WarpContextImpl warpContext) {
        try {
            setupServerAssertion();
            executeClientAction(action);
            awaitServerExecution(warpContext);
            checkClientActionFailure();

            return warpContext.getResult();
        } finally {
            cleanup();
        }
    }

    private void setupServerAssertion() {
        advertiseEnrichment.fire(new AdvertiseEnrichment());
        finishEnrichment.fire(new FinishEnrichment());
    }

    private void executeClientAction(ClientAction action) {
        actionException = null;
        try {
            executeClientAction.fire(action);
        } catch (Exception e) {
            actionException = new ClientActionException(e);
        }
    }

    private void checkClientActionFailure() {
        if (actionException != null) {
            throw actionException;
        }
    }

    private void awaitServerExecution(WarpContextImpl warpContext) {
        awaitResponse.fire(new AwaitResponse());

        TestResult testResult = warpContext.getFirstNonSuccessfulResult();

        if (testResult == null) {
            return;
        }

        switch (testResult.getStatus()) {
            case FAILED:
                propagateFailure(testResult);
                break;
            case SKIPPED:
                propagateSkip();
                break;
        }
    }

    private void cleanup() {
        cleanEnrichment.fire(new CleanEnrichment());
    }

    private void propagateFailure(TestResult testResult) {
        Throwable e = testResult.getThrowable();

        propagateException(e);
    }
    
    private void propagateException(Throwable e) {
        if (e instanceof AssertionError) {
            throw (AssertionError) e;
        } else if (e instanceof ClientWarpExecutionException) {
            throw (ClientWarpExecutionException) e;
        } else if (e instanceof RuntimeException) {
            throw (RuntimeException) e;
        } else {
            throw new ServerWarpExecutionException(e);
        }
    }

    private void propagateSkip() {
        throw new ServerWarpExecutionException("execution was skipped");
    }
}
