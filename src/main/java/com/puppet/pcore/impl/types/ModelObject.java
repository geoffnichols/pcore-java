package com.puppet.pcore.impl.types;

import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

abstract class ModelObject {
	interface Visitor {
		void visit(ModelObject visitable, RecursionGuard guard);
	}

	/**
	 * Keeps track of self recursion of conceptual 'this' and 'that' instances using two separate maps and
	 * <p>
	 * a state. The class is used when tracking self recursion in two objects ('this' and 'that') simultaneously.
	 * A typical example of when this is needed is when testing if 'that' Puppet Type is assignable to 'this'
	 * Puppet Type since both types may contain self references.
	 * <p>
	 * All comparisons are made using the identity of the instance.
	 */
	static class RecursionGuard {

		static final int NO_SELF_RECURSION = 0;
		static final int SELF_RECURSION_IN_BOTH = 3;
		static final int SELF_RECURSION_IN_THAT = 2;
		static final int SELF_RECURSION_IN_THIS = 1;
		private Map<Object,Boolean> thatMap;
		private Map<Object,Boolean> thisMap;
		private Map<Object,Boolean> recursiveThatMap;
		private Map<Object,Boolean> recursiveThisMap;
		private int state;

		RecursionGuard() {
			state = NO_SELF_RECURSION;
		}

		/**
		 * Add the given argument as 'that' and call block with the resulting state. Pop
		 * restore state after call.
		 *
		 * @param instance the object to add
		 * @return the result of calling the block
		 */
		<R> R withThat(Object instance, Function<Integer, R> block) {
			R result;
			if(getThatMap().put(instance, Boolean.TRUE) == null) {
				result = block.apply(state);
				thatMap.remove(instance);
			} else {
				getRecursiveThatMap().put(instance, Boolean.TRUE);
				if((state & SELF_RECURSION_IN_THAT) == 0) {
					state |= SELF_RECURSION_IN_THAT;
					result = block.apply(state);
					state &= ~SELF_RECURSION_IN_THAT;
				}
				else
					result = block.apply(state);
			}
			return result;
		}

		/**
		 * Add the given argument as 'this' and call block with the resulting state. Pop
		 * restore state after call.
		 *
		 * @param instance the object to add
		 * @return the result of calling the block
		 */
		<R> R withThis(Object instance, Function<Integer, R> block) {
			R result;
			if(getThisMap().put(instance, Boolean.TRUE) == null) {
				result = block.apply(state);
				thisMap.remove(instance);
			} else {
				getRecursiveThisMap().put(instance, Boolean.TRUE);
				if((state & SELF_RECURSION_IN_THIS) == 0) {
					state |= SELF_RECURSION_IN_THIS;
					result = block.apply(state);
					state &= ~SELF_RECURSION_IN_THIS;
				} else
					result = block.apply(state);
			}
			return result;
		}

		/**
		 * Checks if recursion was detected for the given argument in the 'that' context
		 *
		 * @param instance the object to check
		 * @return true if recursion was detected, false otherwise.
		 */
		boolean recursiveThat(Object instance) {
			return recursiveThatMap != null && recursiveThatMap.containsKey(instance);
		}

		/**
		 * Checks if recursion was detected for the given argument in the 'this' context
		 *
		 * @param instance the object to check
		 * @return true if recursion was detected, false otherwise.
		 */
		boolean recursiveThis(Object instance) {
			return recursiveThisMap != null && recursiveThisMap.containsKey(instance);
		}

		private Map<Object,Boolean> getRecursiveThatMap() {
			if(recursiveThatMap == null)
				recursiveThatMap = new IdentityHashMap<>();
			return recursiveThatMap;
		}

		private Map<Object,Boolean> getRecursiveThisMap() {
			if(recursiveThisMap == null)
				recursiveThisMap = new IdentityHashMap<>();
			return recursiveThisMap;
		}

		private Map<Object,Boolean> getThatMap() {
			if(thatMap == null)
				thatMap = new IdentityHashMap<>();
			return thatMap;
		}

		private Map<Object,Boolean> getThisMap() {
			if(thisMap == null)
				thisMap = new IdentityHashMap<>();
			return thisMap;
		}
	}

	/**
	 * Acceptor used when re-checking for self recursion
	 */
	static class NoopAcceptor implements Visitor {
		static final NoopAcceptor singleton = new NoopAcceptor();

		@Override
		public void visit(ModelObject type, RecursionGuard guard) {
		}
	}

	protected static boolean equals(AnyType a, AnyType b, RecursionGuard guard) {
		return (a == b) || (a != null && a.guardedEquals(b, guard));
	}

	protected static boolean equals(List<?> a, List<?> b, RecursionGuard guard) {
		if (a == b)
			return true;

		int idx = a.size();
		if (idx != b.size())
			return false;

		while(--idx >= 0) {
			Object av = a.get(idx);
			if(av instanceof ModelObject) {
				if(!((ModelObject)av).guardedEquals(b.get(idx), guard))
					return false;
			} else {
				if(!av.equals(b.get(idx)))
					return false;
			}
		}

		return true;
	}

	protected static <K,V extends ModelObject> boolean equals(Map<K,V> a, Map<K,V> b, RecursionGuard guard) {
		if (a == b)
			return true;

		if (a.size() != b.size())
			return false;

		for(Map.Entry<K,V> e : a.entrySet()) {
			K key = e.getKey();
			V value = e.getValue();
			if(value == null) {
				if(!(b.get(key) == null && b.containsKey(key)))
					return false;
			} else {
				if(!value.guardedEquals(b.get(key), guard))
					return false;
			}
		}
		return true;
	}

	@SuppressWarnings("EqualsWhichDoesntCheckParameterClass")
	public final boolean equals(Object o) {
		return guardedEquals(o, null);
	}

	void accept(Visitor visitor, RecursionGuard guard) {
		visitor.visit(this, guard);
	}

	abstract boolean guardedEquals(Object o, RecursionGuard guard);
}
