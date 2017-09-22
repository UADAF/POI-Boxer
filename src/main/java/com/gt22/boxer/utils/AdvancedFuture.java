package com.gt22.boxer.utils;

import com.gt22.randomutils.Instances;
import org.jooq.lambda.Unchecked;
import org.jooq.lambda.tuple.Tuple;
import org.jooq.lambda.tuple.Tuple2;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;
import java.util.function.Consumer;

public abstract class AdvancedFuture<T> implements Future<T> {
	protected List<Consumer<T>> onCompleteActions;

	public static <T> AdvancedFuture<T> decorator(Future<T> future) {
		return future instanceof AdvancedFuture ? (AdvancedFuture) future : new FutureDecorator<>(future);
	}

	public static <T> AdvancedFuture<T> constant(T val) {
		return new Constant<>(val);
	}

	/**
	 * @return tuple of future, and consumer that should be called to complete future
	 * Example:
	 * <code>
	 *     Tuple2&lt;AdvancedFuture&lt;String&gt;, Consumer&lt;String&gt;&gt; future = AdvancedFuture.manual();<br>
	 *     ...some parallel thread...<br>
	 *     Thread.sleep(5000);<br>
	 *     future.v2().accept("Future completed");<br>
	 *     ...end of parallel thread...<br>
	 *     return future.v1()
	 * </code>
	 */
	public static <T> Tuple2<AdvancedFuture<T>, Consumer<T>> manual() {
		Manual<T> m = new Manual<>();
		return Tuple.tuple(m, m::complete);
	}

	private static class Constant<T> extends AdvancedFuture<T> {
		private T val;
		Constant(T val) {
			this.val = val;
		}

		@Override
		public T get() {
			return val;
		}

		@Override
		public T get(long timeout, TimeUnit unit) {
			return get();
		}

		@Override
		public boolean isDone() {
			return true;
		}
	}

	private static class FutureDecorator<T> extends AdvancedFuture<T> {

		private Future<T> base;
		public FutureDecorator(Future<T> base) {
			this.base = base;
			Instances.getExecutor().submit(Unchecked.runnable(() -> {
				T val = get();
				if(onCompleteActions != null) {
					onCompleteActions.forEach(c -> c.accept(val));
					onCompleteActions = null;
				}
			}));
		}

		@Override
		public T get() throws InterruptedException, ExecutionException {
			return base.get();
		}

		@Override
		public T get(long timeout, TimeUnit unit) throws InterruptedException, ExecutionException, TimeoutException {
			return base.get(timeout, unit);
		}

		@Override
		public boolean cancel(boolean mayInterruptIfRunning) {
			return base.cancel(mayInterruptIfRunning);
		}

		@Override
		public boolean isCancelled() {
			return base.isCancelled();
		}

		@Override
		public boolean canCancel() {
			return true;
		}

		@Override
		public boolean isDone() {
			return base.isDone();
		}

	}

	private static class Manual<T> extends AdvancedFuture<T> {
		private static final Object FUTURE_NOT_COMPLETED = new Object(); //Placeholder to allow using null values
		private CountDownLatch lock = new CountDownLatch(1);
		private Object val = FUTURE_NOT_COMPLETED;

		@Override
		public boolean isDone() {
			return val != FUTURE_NOT_COMPLETED;
		}

		@SuppressWarnings("unchecked")
		@Override
		public T get() throws InterruptedException {
			lock.await();
			assert val != FUTURE_NOT_COMPLETED; //If lock completed and value is still FUTURE_NOT_COMPLETED then something is really wrong
			return (T) val;
		}

		@SuppressWarnings("unchecked")
		@Override
		public T get(long timeout, TimeUnit unit) throws InterruptedException {
			lock.await(timeout, unit);
			assert val != FUTURE_NOT_COMPLETED; //If lock completed and value is still FUTURE_NOT_COMPLETED then something is really wrong
			return (T) val;
		}

		public void complete(T val) {
			if(this.val != FUTURE_NOT_COMPLETED) throw new IllegalStateException("Future already completed");
			if(val == FUTURE_NOT_COMPLETED) throw new IllegalArgumentException("Usage of AdvancedFuture#FUTURE_NOT_COMPLETED in AdvancedFuture#complete method is not allowed");
			this.val = val;
			onCompleteActions.forEach(c -> c.accept(val));
			onCompleteActions = null;
			lock.countDown();
		}
	}

	@Override
	public boolean cancel(boolean mayInterruptIfRunning) {
		throw new UnsupportedOperationException("This future cannot be canceled");
	}

	@Override
	public boolean isCancelled() {
		return false;
	}

	public boolean canCancel() {
		return false;
	}

	@SuppressWarnings("unchecked")
	public void onComplete(Consumer<T> action) {
		if(isDone()) {
			try {
				action.accept(get());
			} catch (InterruptedException | ExecutionException e) { //Future is done, so no exceptions should be thrown
				e.printStackTrace();
			}
		} else {
			if(onCompleteActions == null) {
				onCompleteActions = new ArrayList<>();
			}
			onCompleteActions.add(action);
		}
	}

}