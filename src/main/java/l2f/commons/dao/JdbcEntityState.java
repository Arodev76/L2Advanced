package l2f.commons.dao;

/**
 * @author VISTALL
 * @date 8:28/31.01.2011
 */
public enum JdbcEntityState
{
	CREATED(true, false, false, false),
	STORED(false, true, false, true),
	UPDATED(false, true, true, true),
	DELETED(false, false, false, false);
	
	private final boolean savable;
	private final boolean deletable;
	private final boolean updatable;
	private final boolean persisted;
	
	JdbcEntityState(boolean savable, boolean deletable, boolean updatable, boolean persisted)
	{
		this.savable = savable;
		this.deletable = deletable;
		this.updatable = updatable;
		this.persisted = persisted;
	}
	
	/**
	 * @return true, If the entity can be stored in the repository
	 */
	public boolean isSavable()
	{
		return savable;
	}
	
	/**
	 * @return true, if the entity can be removed from the repository
	 */
	public boolean isDeletable()
	{
		return deletable;
	}
	
	/**
	 * @return true, if the entity can be updated in the repository
	 */
	public boolean isUpdatable()
	{
		return updatable;
	}
	
	/**
	 * @return true, if the entity in the repository
	 */
	public boolean isPersisted()
	{
		return persisted;
	}
}
