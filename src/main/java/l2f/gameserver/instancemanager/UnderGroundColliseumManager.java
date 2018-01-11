package l2f.gameserver.instancemanager;

import l2f.gameserver.model.Zone;
import l2f.gameserver.model.Zone.ZoneType;
import l2f.gameserver.model.entity.Coliseum;
import l2f.gameserver.utils.ReflectionUtils;
import org.slf4j.LoggerFactory;

import java.util.AbstractMap;
import java.util.HashMap;
import java.util.List;

public class UnderGroundColliseumManager
{
	private static final org.slf4j.Logger LOG = LoggerFactory.getLogger(UnderGroundColliseumManager.class);
	private static UnderGroundColliseumManager _instance;
	private HashMap<String, Coliseum> _coliseums;

	public static UnderGroundColliseumManager getInstance()
	{
		if (_instance == null)
			_instance = new UnderGroundColliseumManager();
		return _instance;
	}

	public UnderGroundColliseumManager()
	{
		List<Zone> zones = ReflectionUtils.getZonesByType(ZoneType.UnderGroundColiseum);
		if (zones.isEmpty())
			LOG.info("Not found zones for UnderGround Coliseum!!!");
		else
		{
			for (Zone zone : zones)
				getColiseums().put(zone.getName(), new Coliseum());
		}
		LOG.info("Loaded: " + getColiseums().size() + " UnderGround Coliseums.");
	}

	@SuppressWarnings({ "unchecked", "rawtypes" })
	public final AbstractMap<String, Coliseum> getColiseums()
	{
		if (_coliseums == null)
			_coliseums = new HashMap();
		return _coliseums;
	}

	public Coliseum getColiseumByLevelLimit(int limit)
	{
		for (Coliseum coliseum : _coliseums.values())
		{
			if (coliseum.getMaxLevel() == limit)
				return coliseum;
		}
		return null;
	}
}