package com.moqifei.rpc.exchange.params;

import com.moqifei.rpc.exchange.invoker.PigeonRpcInvoker;
import com.moqifei.rpc.net.params.PigeonRpcRequest;
import com.moqifei.rpc.net.params.PigeonRpcResponse;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

public class PigeonRpcFutureReponse implements Future<PigeonRpcResponse> {
    private boolean done = false;
    private final Lock lock = new ReentrantLock();
    private final Condition cond;
    private final long timeout;
    private final PigeonRpcInvoker rpcInvoker;

    private PigeonRpcRequest rpcRequest;
    private PigeonRpcResponse rpcResponse;


    //CALLBACK TODO

    public PigeonRpcFutureReponse(long timeout, PigeonRpcInvoker rpcInvoker, PigeonRpcRequest rpcRequest) {
        this.timeout = timeout;
        this.rpcInvoker = rpcInvoker;
        this.cond = this.lock.newCondition();
        this.rpcRequest = rpcRequest;
        setInvokeFuture();
    }

    public void setInvokeFuture(){
        this.rpcInvoker.setInvokerFuture(rpcRequest.getRequestId(), this);
    }

    public void removeInvokeFuture(){
        this.rpcInvoker.removeInvokerFuture(rpcRequest.getRequestId());
    }

    public void setRpcResponse(PigeonRpcResponse rpcResponse){
        this.lock.lock();
        try {
            this.rpcResponse = rpcResponse;
            done = true;
            this.cond.signal();
        } finally {
            this.lock.unlock();
        }
    }


    /**
     * Attempts to cancel execution of this task.  This attempt will
     * fail if the task has already completed, has already been cancelled,
     * or could not be cancelled for some other reason. If successful,
     * and this task has not started when {@code cancel} is called,
     * this task should never run.  If the task has already started,
     * then the {@code mayInterruptIfRunning} parameter determines
     * whether the thread executing this task should be interrupted in
     * an attempt to stop the task.
     *
     * <p>After this method returns, subsequent calls to {@link #isDone} will
     * always return {@code true}.  Subsequent calls to {@link #isCancelled}
     * will always return {@code true} if this method returned {@code true}.
     *
     * @param mayInterruptIfRunning {@code true} if the thread executing this
     *                              task should be interrupted; otherwise, in-progress tasks are allowed
     *                              to complete
     * @return {@code false} if the task could not be cancelled,
     * typically because it has already completed normally;
     * {@code true} otherwise
     */
    @Override
    public boolean cancel(boolean mayInterruptIfRunning) {
        return false;
    }

    /**
     * Returns {@code true} if this task was cancelled before it completed
     * normally.
     *
     * @return {@code true} if this task was cancelled before it completed
     */
    @Override
    public boolean isCancelled() {
        return false;
    }

    /**
     * Returns {@code true} if this task completed.
     * <p>
     * Completion may be due to normal termination, an exception, or
     * cancellation -- in all of these cases, this method will return
     * {@code true}.
     *
     * @return {@code true} if this task completed
     */
    @Override
    public boolean isDone() {
        return done;
    }

    /**
     * Waits if necessary for the computation to complete, and then
     * retrieves its result.
     *
     * @return the computed result
     * @throws ExecutionException    if the computation threw an
     *                               exception
     * @throws InterruptedException  if the current thread was interrupted
     *                               while waiting
     */
    @Override
    public PigeonRpcResponse get() throws InterruptedException, ExecutionException {
        try {
            return get(this.timeout, TimeUnit.MILLISECONDS);
        } catch (TimeoutException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Waits if necessary for at most the given time for the computation
     * to complete, and then retrieves its result, if available.
     *
     * @param timeout the maximum time to wait
     * @param unit    the time unit of the timeout argument
     * @return the computed result
     * @throws ExecutionException    if the computation threw an
     *                               exception
     * @throws InterruptedException  if the current thread was interrupted
     *                               while waiting
     * @throws TimeoutException      if the wait timed out
     */
    @Override
    public PigeonRpcResponse get(long timeout, TimeUnit unit) throws InterruptedException, ExecutionException, TimeoutException {
        if (timeout <= 0) {
            timeout = 1000;
        }

        if (!this.isDone()) {
            this.lock.lock();

            try {
                while(!this.isDone()) {
                    this.cond.await(timeout, unit);
                }
            } catch (InterruptedException var8) {
                throw new RuntimeException(var8);
            } finally {
                this.lock.unlock();
            }

            if (!this.isDone()) {
                throw new TimeoutException("xxl-rpc, request timeout at:"+ System.currentTimeMillis() +", request:" + rpcRequest.toString());
            }
        }

        return this.rpcResponse;
    }
}
