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

import org.jboss.arquillian.core.api.Instance;
import org.jboss.arquillian.core.api.annotation.Inject;

public class DefaultAssertionSynchronizer implements AssertionSynchronizer {
    
    @Inject
    private Instance<SynchronizationPoint> synchronization;
    
    @Inject
    private Instance<WarpContext> warpContext;

    @Override
    public void advertise() {
        synchronization().advertise();
        warpContext().tryFinalizeResponse();
    }

    @Override
    public void finish() {
        synchronization().close();
    }

    @Override
    public void waitForResponse() {
        synchronization().awaitResponses();
    }

    @Override
    public void clean() {
        // nothing to do - context will be freed automatically
    }
    
    private SynchronizationPoint synchronization() {
        return synchronization.get();
    }
    
    private WarpContext warpContext() {
        return warpContext.get();
    }
}
