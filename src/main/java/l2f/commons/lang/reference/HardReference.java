package l2f.commons.lang.reference;

/**
 * Interface custodian links.
 * @author G1ta0
 * @param <T>
 */
public interface HardReference<T>
{
	/**
	 * Get the object that is held
	 * @return
	 **/
	public T get();
	
	/** Clear link above to hold the item **/
	public void clear();
}
