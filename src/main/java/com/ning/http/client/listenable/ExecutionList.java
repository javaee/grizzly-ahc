/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2017 Oracle and/or its affiliates. All rights reserved.
 *
 * The contents of this file are subject to the terms of either the GNU
 * General Public License Version 2 only ("GPL") or the Common Development
 * and Distribution License("CDDL") (collectively, the "License").  You
 * may not use this file except in compliance with the License.  You can
 * obtain a copy of the License at
 * https://oss.oracle.com/licenses/CDDL+GPL-1.1
 * or LICENSE.txt.  See the License for the specific
 * language governing permissions and limitations under the License.
 *
 * When distributing the software, include this License Header Notice in each
 * file and include the License file at LICENSE.txt.
 *
 * GPL Classpath Exception:
 * Oracle designates this particular file as subject to the "Classpath"
 * exception as provided by Oracle in the GPL Version 2 section of the License
 * file that accompanied this code.
 *
 * Modifications:
 * If applicable, add the following below the License Header, with the fields
 * enclosed by brackets [] replaced by your own identifying information:
 * "Portions Copyright [year] [name of copyright owner]"
 *
 * Contributor(s):
 * If you wish your version of this file to be governed by only the CDDL or
 * only the GPL Version 2, indicate your decision by adding "[Contributor]
 * elects to include this software in this distribution under the [CDDL or GPL
 * Version 2] license."  If you don't indicate a single choice of license, a
 * recipient has the option to distribute your version of this file under
 * either the CDDL, the GPL Version 2 or to extend the choice of license to
 * its licensees as provided above.  However, if you add GPL Version 2 code
 * and therefore, elected the GPL Version 2 license, then the option applies
 * only if the new code is made subject to such option by the copyright
 * holder.
 *
 *
 * This file incorporates work covered by the following copyright and
 * permission notice:
 *
 * Copyright (c) 2010-2012 Sonatype, Inc. All rights reserved.
 *
 * This program is licensed to you under the Apache License Version 2.0,
 * and you may not use this file except in compliance with the Apache License Version 2.0.
 * You may obtain a copy of the Apache License Version 2.0 at http://www.apache.org/licenses/LICENSE-2.0.
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the Apache License Version 2.0 is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the Apache License Version 2.0 for the specific language governing permissions and limitations there under.
 */
/*
 * Copyright (C) 2007 Google Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.ning.http.client.listenable;

import java.util.Queue;
import java.util.concurrent.Executor;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * <p>A list of ({@code Runnable}, {@code Executor}) pairs that guarantees
 * that every {@code Runnable} that is added using the add method will be
 * executed in its associated {@code Executor} after {@link #run()} is called.
 * {@code Runnable}s added after {@code run} is called are still guaranteed to
 * execute.
 *
 * @author Nishant Thakkar
 * @author Sven Mawson
 * @since 1
 */
public final class ExecutionList implements Runnable {

    // Logger to log exceptions caught when running runnables.
    private static final Logger log =
            Logger.getLogger(ExecutionList.class.getName());

    // The runnable,executor pairs to execute.
    private final Queue<RunnableExecutorPair> runnables = new LinkedBlockingQueue<>();

    // Boolean we use mark when execution has started.  Only accessed from within
    // synchronized blocks.
    private boolean executed = false;

    /**
     * Add the runnable/executor pair to the list of pairs to execute.  Executes
     * the pair immediately if we've already started execution.
     */
    public void add(Runnable runnable, Executor executor) {

        if (runnable == null) {
            throw new NullPointerException("Runnable is null");
        }

        if (executor == null) {
            throw new NullPointerException("Executor is null");
        }

        boolean executeImmediate = false;

        // Lock while we check state.  We must maintain the lock while adding the
        // new pair so that another thread can't run the list out from under us.
        // We only add to the list if we have not yet started execution.
        synchronized (runnables) {
            if (!executed) {
                runnables.add(new RunnableExecutorPair(runnable, executor));
            } else {
                executeImmediate = true;
            }
        }

        // Execute the runnable immediately.  Because of scheduling this may end up
        // getting called before some of the previously added runnables, but we're
        // ok with that.  If we want to change the contract to guarantee ordering
        // among runnables we'd have to modify the logic here to allow it.
        if (executeImmediate) {
            executor.execute(runnable);
        }
    }

    /**
     * Runs this execution list, executing all pairs in the order they were
     * added.  Pairs added after this method has started executing the list will
     * be executed immediately.
     */
    public void run() {

        // Lock while we update our state so the add method above will finish adding
        // any listeners before we start to run them.
        synchronized (runnables) {
            executed = true;
        }

        // At this point the runnables will never be modified by another
        // thread, so we are safe using it outside of the synchronized block.
        while (!runnables.isEmpty()) {
            runnables.poll().execute();
        }
    }

    private static class RunnableExecutorPair {
        final Runnable runnable;
        final Executor executor;

        RunnableExecutorPair(Runnable runnable, Executor executor) {
            this.runnable = runnable;
            this.executor = executor;
        }

        void execute() {
            try {
                executor.execute(runnable);
            } catch (RuntimeException e) {
                // Log it and keep going, bad runnable and/or executor.  Don't
                // punish the other runnables if we're given a bad one.  We only
                // catch RuntimeException because we want Errors to propagate up.
                log.log(Level.SEVERE, "RuntimeException while executing runnable "
                        + runnable + " with executor " + executor, e);
            }
        }
    }
}
