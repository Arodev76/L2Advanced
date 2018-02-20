package l2f.gameserver.model.entity.events.fightclubmanager;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import l2f.gameserver.utils.Location;

public class FightClubTeam implements Serializable
{
	public static enum TEAM_NAMES {
		Red(0x162ee1),
		Blue(0xb53e41),
		Green(0x3eb541),
		Yellow(0x2efdff),
		Gray(0x808080),
		Orange(0x0087f9),
		Black(0x161616),
		White(0xffffff),
		Violet(0xba2785),
		Cyan(0xe3e136),
		Pink(0xde6def);
		
		public int _nameColor;
		
		private TEAM_NAMES(int nameColor)
		{
			_nameColor = nameColor;
		}
	}
	private int _index;
	private String _name;
	private List<FightClubPlayer> _players = new ArrayList<>();
	private Location _spawnLoc;
	private int _score;
	
	public FightClubTeam(int index)
	{
		_index = index;
		chooseName();
	}
	
	public int getIndex()
	{
		return _index;
	}
	
	public String getName()
	{
		return _name;
	}
	
	public void setName(String name)
	{
		_name = name;
	}
	
	public String chooseName()
	{
		_name = TEAM_NAMES.values()[_index-1].toString();
		return _name;
	}
	
	public int getNickColor()
	{
		return TEAM_NAMES.values()[_index-1]._nameColor;
	}
	
	public List<FightClubPlayer> getPlayers()
	{
		return _players;
	}
	
	public void addPlayer(FightClubPlayer player)
	{
		_players.add(player);
	}
	
	public void removePlayer(FightClubPlayer player)
	{
		_players.remove(player);
	}
	
	public void setSpawnLoc(Location loc)
	{
		_spawnLoc = loc;
	}
	
	public Location getSpawnLoc()
	{
		return _spawnLoc;
	}
	
	public void setScore(int newScore)
	{
		_score = newScore;
	}
	
	public void incScore(int by)
	{
		_score += by;
	}
	
	public int getScore()
	{
		return _score;
	}
}
