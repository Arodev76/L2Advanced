package l2f.commons.lang.reference;

public class AbstractHardReference<T> implements HardReference<T>
{
	private T reference;

	public AbstractHardReference(T reference)
	{
		this.reference = reference;
	}

	@Override
	public T get()
	{
		return reference;
	}

	@Override
	public void clear()
	{
		reference = null;
	}

	@SuppressWarnings("rawtypes")
	@Override
	public boolean equals(Object o)
	{
		if (o == this)
			return true;
		if (o == null)
			return false;
		if (!(o instanceof AbstractHardReference))
			return false;
		if ((((AbstractHardReference) o)).get() == null)
			return false;
		return ((((AbstractHardReference) o)).get().equals(get()));
	}
}
