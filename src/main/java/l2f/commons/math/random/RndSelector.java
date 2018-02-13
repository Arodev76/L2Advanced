package l2f.commons.math.random;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import l2f.commons.util.Rnd;

public class RndSelector<E>
{
	private class RndNode<T> implements Comparable<RndNode<T>>
	{
		protected final T value;
		protected final int weight;
		
		public RndNode(T value, int weight)
		{
			this.value = value;
			this.weight = weight;
		}
		
		@Override
		public int compareTo(RndNode<T> o)
		{
			return this.weight - weight;
		}
	}
	
	private int totalWeight = 0;
	private final List<RndNode<E>> nodes;
	
	public RndSelector()
	{
		nodes = new ArrayList<>();
	}
	
	public RndSelector(int initialCapacity)
	{
		nodes = new ArrayList<>(initialCapacity);
	}
	
	public void add(E value, int weight)
	{
		if ((value == null) || (weight <= 0))
			return;
		totalWeight += weight;
		nodes.add(new RndNode<>(value, weight));
	}
	
	/**
	 * Returns one of the elements or null, null is possible only if the sum of weights of all elements is less than maxWeight
	 * @param maxWeight
	 * @return
	 */
	public E chance(int maxWeight)
	{
		if (maxWeight <= 0)
			return null;
		
		Collections.sort(nodes);
		
		final int r = Rnd.get(maxWeight);
		int weight = 0;
		for (int i = 0; i < nodes.size(); i++)
			if ((weight += nodes.get(i).weight) > r)
				return nodes.get(i).value;
		return null;
	}
	
	/**
	 * Returns one of the elements or null, null is possible only if the sum of the weights of all elements is less than 100
	 * @return
	 */
	public E chance()
	{
		return chance(100);
	}
	
	/**
	 * Will return one of the elements
	 * @return
	 */
	public E select()
	{
		return chance(totalWeight);
	}
	
	public void clear()
	{
		totalWeight = 0;
		nodes.clear();
	}
}