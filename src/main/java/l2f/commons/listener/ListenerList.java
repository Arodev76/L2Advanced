package l2f.commons.listener;

import java.util.Collection;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;

/**
 * A class that implements a list of listeners for each type of interface.
 * @author Arodev
 * @param <T> basic listener interface
 */
public class ListenerList<T>
{
	protected Set<Listener<T>> listeners = new CopyOnWriteArraySet<>();
	
	public Collection<Listener<T>> getListeners()
	{
		return listeners;
	}
	
	/**
	 * Add listener to list
	 * @param listener
	 * @return returns true if the listener was added
	 */
	public boolean add(Listener<T> listener)
	{
		return listeners.add(listener);
	}
	
	/**
	 * Delete listener from list
	 * @param listener
	 * @return returns true if the listener was deleted
	 */
	public boolean remove(Listener<T> listener)
	{
		return listeners.remove(listener);
	}
	
}
