/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2010-2018 Oracle and/or its affiliates. All rights reserved.
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
 */

package org.glassfish.enterprise.concurrent;

import java.util.concurrent.*;
import javax.enterprise.concurrent.ManagedTaskListener;
import org.glassfish.enterprise.concurrent.internal.ManagedFutureTask;
import org.glassfish.enterprise.concurrent.internal.TaskDoneCallback;

/**
 *
 * Adopted from java.util.concurrent.ExecutorCompletionService with support
 * for ManagedTaskListener.
 */
public class ManagedExecutorCompletionService<V> implements TaskDoneCallback {
    private final AbstractManagedExecutorService executor;
    private final BlockingQueue<Future<V>> completionQueue;

    /**
     * Creates an ExecutorCompletionService using the supplied
     * executor for base task execution and a
     * {@link LinkedBlockingQueue} as a completion queue.
     *
     * @param executor the executor to use
     * @throws NullPointerException if executor is {@code null}
     */
    public ManagedExecutorCompletionService(AbstractManagedExecutorService executor) {
        if (executor == null) {
            throw new NullPointerException();
        }
        this.executor = executor;
        this.completionQueue = new LinkedBlockingQueue<Future<V>>();
    }

    /**
     * Creates an ExecutorCompletionService using the supplied
     * executor for base task execution and the supplied queue as its
     * completion queue.
     *
     * @param executor the executor to use
     * @param completionQueue the queue to use as the completion queue
     *        normally one dedicated for use by this service. This
     *        queue is treated as unbounded -- failed attempted
     *        {@code Queue.add} operations for completed taskes cause
     *        them not to be retrievable.
     * @throws NullPointerException if executor or completionQueue are {@code null}
     */
    public ManagedExecutorCompletionService(AbstractManagedExecutorService executor,
                                     BlockingQueue<Future<V>> completionQueue) {
        if (executor == null || completionQueue == null) {
            throw new NullPointerException();
        }
        this.executor = executor;
        this.completionQueue = completionQueue;
    }
    
    public Future<V> submit(Callable<V> task) {
        if (task == null) {
            throw new NullPointerException();
        }
        ManagedFutureTask<V> f = executor.getNewTaskFor(task);
        f.setTaskDoneCallback(this);
        executor.executeManagedFutureTask(f);
        return f;
    }

    public Future<V> submit(Runnable task, V result, ManagedTaskListener taskListener) {
        if (task == null) {
            throw new NullPointerException();
        }
        ManagedFutureTask<V> f = executor.getNewTaskFor(task, result);
        f.setTaskDoneCallback(this);
        executor.executeManagedFutureTask(f);
        return f;
    }

    
    public Future<V> take() throws InterruptedException {
        return completionQueue.take();
    }

    public Future<V> poll() {
        return completionQueue.poll();
    }

    public Future<V> poll(long timeout, TimeUnit unit)
            throws InterruptedException {
        return completionQueue.poll(timeout, unit);
    }

    @Override
    public void taskDone(ManagedFutureTask future) {
        completionQueue.add(future); 
    }
}

