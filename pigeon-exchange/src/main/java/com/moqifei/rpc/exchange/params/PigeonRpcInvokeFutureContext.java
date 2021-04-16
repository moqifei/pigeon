package com.moqifei.rpc.exchange.params;

import com.moqifei.rpc.net.params.PigeonRpcResponse;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

public class PigeonRpcInvokeFutureContext implements Future {
    private PigeonRpcFutureReponse pigeonRpcFutureReponse;
    public PigeonRpcInvokeFutureContext(PigeonRpcFutureReponse pigeonRpcFutureReponse){
        this.pigeonRpcFutureReponse = pigeonRpcFutureReponse;
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
        return pigeonRpcFutureReponse.cancel(mayInterruptIfRunning);
    }

    /**
     * Returns {@code true} if this task was cancelled before it completed
     * normally.
     *
     * @return {@code true} if this task was cancelled before it completed
     */
    @Override
    public boolean isCancelled() {
        return pigeonRpcFutureReponse.isCancelled();
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
        return pigeonRpcFutureReponse.isDone();
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
    public Object get() throws InterruptedException, ExecutionException {
        try {
            return get(-1,TimeUnit.MILLISECONDS);
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
    public Object get(long timeout, TimeUnit unit) throws InterruptedException, ExecutionException, TimeoutException {
        try{
            PigeonRpcResponse pigeonRpcResponse = pigeonRpcFutureReponse.get(timeout,unit);
            return pigeonRpcResponse;
        }finally {
            pigeonRpcFutureReponse.removeInvokeFuture();
        }
    }

    private static ThreadLocal<PigeonRpcInvokeFutureContext> threadLocal = new ThreadLocal<>();

    public static void setContext(PigeonRpcInvokeFutureContext context){
        threadLocal.set(context);
    }

    public static <T> Future<T> getContext(Class<T> type){
        Future<T> future = (Future<T>) threadLocal.get();
        threadLocal.remove();
        return future;
    }

}
