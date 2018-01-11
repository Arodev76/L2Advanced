package quests;

import l2f.gameserver.model.Player;
import l2f.gameserver.model.instances.NpcInstance;
import l2f.gameserver.model.quest.Quest;
import l2f.gameserver.model.quest.QuestState;
import l2f.gameserver.scripts.ScriptFile;

public class _025_HidingBehindTheTruth extends Quest implements ScriptFile
{
	// Список NPC
	private final int AGRIPEL = 31348;
	private final int BENEDICT = 31349;
	private final int BROKEN_BOOK_SHELF = 31534;
	private final int COFFIN = 31536;
	private final int MAID_OF_LIDIA = 31532;
	private final int MYSTERIOUS_WIZARD = 31522;
	private final int TOMBSTONE = 31531;

	// Список итемов
	private final int CONTRACT = 7066;
	private final int EARRING_OF_BLESSING = 874;
	private final int GEMSTONE_KEY = 7157;
	private final int LIDIAS_DRESS = 7155;
	private final int MAP_FOREST_OF_DEADMAN = 7063;
	private final int NECKLACE_OF_BLESSING = 936;
	private final int RING_OF_BLESSING = 905;
	private final int SUSPICIOUS_TOTEM_DOLL_1 = 7151;
	private final int SUSPICIOUS_TOTEM_DOLL_2 = 7156;
	private final int SUSPICIOUS_TOTEM_DOLL_3 = 7158;

	// Список мобов
	// Triol's Pawn
	private final int TRIOLS_PAWN = 27218;

	private NpcInstance COFFIN_SPAWN = null;

	public _025_HidingBehindTheTruth()
	{
		super(false);

		addStartNpc(BENEDICT);

		addTalkId(AGRIPEL);
		addTalkId(BROKEN_BOOK_SHELF);
		addTalkId(COFFIN);
		addTalkId(MAID_OF_LIDIA);
		addTalkId(MYSTERIOUS_WIZARD);
		addTalkId(TOMBSTONE);

		addKillId(TRIOLS_PAWN);

		addQuestItem(SUSPICIOUS_TOTEM_DOLL_3);
	}

	@Override
	public String onEvent(String event, QuestState qs, NpcInstance npc)
	{
		if (event.equalsIgnoreCase("StartQuest"))
		{
			if (qs.getCond() == 0)
				qs.setState(STARTED);
			QuestState qs_24 = qs.getPlayer().getQuestState(_024_InhabitantsOfTheForestOfTheDead.class);
			if (qs_24 == null || !qs_24.isCompleted())
			{
				qs.setCond(1);
				return "31349-02.htm";
			}
			qs.playSound(SOUND_ACCEPT);
			if (qs.getQuestItemsCount(SUSPICIOUS_TOTEM_DOLL_1) == 0)
			{
				qs.setCond(2);
				return "31349-03a.htm";
			}
			return "31349-03.htm";
		}
		else if (event.equalsIgnoreCase("31349-10.htm"))
			qs.setCond(4);
		else if (event.equalsIgnoreCase("31348-08.htm"))
		{
			if (qs.getCond() == 4)
			{
				qs.setCond(5);
				qs.takeItems(SUSPICIOUS_TOTEM_DOLL_1, -1);
				qs.takeItems(SUSPICIOUS_TOTEM_DOLL_2, -1);
				if (qs.getQuestItemsCount(GEMSTONE_KEY) == 0)
					qs.giveItems(GEMSTONE_KEY, 1);
			}
			else if (qs.getCond() == 5)
				return "31348-08a.htm";
		}
		else if (event.equalsIgnoreCase("31522-04.htm"))
		{
			qs.setCond(6);
			if (qs.getQuestItemsCount(MAP_FOREST_OF_DEADMAN) == 0)
				qs.giveItems(MAP_FOREST_OF_DEADMAN, 1);
		}
		else if (event.equalsIgnoreCase("31534-07.htm"))
		{
			Player player = qs.getPlayer();
			qs.addSpawn(TRIOLS_PAWN, player.getX() + 50, player.getY() + 50, player.getZ());
			qs.setCond(7);
		}
		else if (event.equalsIgnoreCase("31534-11.htm"))
		{
			qs.set("id", "8");
			qs.giveItems(CONTRACT, 1);
		}
		else if (event.equalsIgnoreCase("31532-07.htm"))
			qs.setCond(11);
		else if (event.equalsIgnoreCase("31531-02.htm"))
		{
			qs.setCond(12);
			Player player = qs.getPlayer();

			if (COFFIN_SPAWN != null)
				COFFIN_SPAWN.deleteMe();
			COFFIN_SPAWN = qs.addSpawn(COFFIN, player.getX() + 50, player.getY() + 50, player.getZ());

			qs.startQuestTimer("Coffin_Despawn", 120000);
		}
		else if (event.equalsIgnoreCase("Coffin_Despawn"))
		{
			if (COFFIN_SPAWN != null)
				COFFIN_SPAWN.deleteMe();

			if (qs.getCond() == 12)
				qs.setCond(11);
			return null;
		}
		else if (event.equalsIgnoreCase("Lidia_wait"))
		{
			qs.set("id", "14");
			return null;
		}
		else if (event.equalsIgnoreCase("31532-21.htm"))
			qs.setCond(15);
		else if (event.equalsIgnoreCase("31522-13.htm"))
			qs.setCond(16);
		else if (event.equalsIgnoreCase("31348-16.htm"))
			qs.setCond(17);
		else if (event.equalsIgnoreCase("31348-17.htm"))
			qs.setCond(18);
		else if (event.equalsIgnoreCase("31348-14.htm"))
			qs.set("id", "16");
		else if (event.equalsIgnoreCase("End1"))
		{
			if (qs.getCond() != 17)
				return "31532-24.htm";
			qs.giveItems(RING_OF_BLESSING, 2);
			qs.giveItems(EARRING_OF_BLESSING, 1);
			qs.addExpAndSp(572277, 53750);
			qs.exitCurrentQuest(false);
			return "31532-25.htm";
		}
		else if (event.equalsIgnoreCase("End2"))
		{
			if (qs.getCond() != 18)
				return "31522-15a.htm";
			qs.giveItems(NECKLACE_OF_BLESSING, 1);
			qs.giveItems(EARRING_OF_BLESSING, 1);
			qs.addExpAndSp(572277, 53750);
			qs.exitCurrentQuest(false);
			return "31522-16.htm";
		}
		return event;
	}

	@Override
	public String onTalk(NpcInstance npc, QuestState st)
	{
		int npcId = npc.getNpcId();
		int cond = st.getCond();
		int IntId = st.getInt("id");
		switch (npcId)
		{
			case BENEDICT:
				if (cond == 0 || cond == 1)
					return "31349-01.htm";
				if (cond == 2)
					return st.getQuestItemsCount(SUSPICIOUS_TOTEM_DOLL_1) == 0 ? "31349-03a.htm" : "31349-03.htm";
				if (cond == 3)
					return "31349-03.htm";
				if (cond == 4)
					return "31349-11.htm";
				break;

			case MYSTERIOUS_WIZARD:
				if (cond == 2)
				{
					st.setCond(3);
					st.giveItems(SUSPICIOUS_TOTEM_DOLL_2, 1);
					return "31522-01.htm";
				}
				if (cond == 3)
					return "31522-02.htm";
				if (cond == 5)
					return "31522-03.htm";
				if (cond == 6)
					return "31522-05.htm";
				if (cond == 8)
				{
					if (IntId != 8)
						return "31522-05.htm";
					st.setCond(9);
					return "31522-06.htm";
				}
				if (cond == 15)
					return "31522-06a.htm";
				if (cond == 16)
					return "31522-12.htm";
				if (cond == 17)
					return "31522-15a.htm";
				if (cond == 18)
				{
					st.set("id", "18");
					return "31522-15.htm";
				}
				break;

			case AGRIPEL:
				if (cond == 4)
					return "31348-01.htm";
				if (cond == 5)
					return "31348-03.htm";
				if (cond == 16)
					return IntId == 16 ? "31348-15.htm" : "31348-09.htm";
				if (cond == 17 || cond == 18)
					return "31348-15.htm";
				break;

			case BROKEN_BOOK_SHELF:
				if (cond == 6)
					return "31534-01.htm";
				if (cond == 7)
					return "31534-08.htm";
				if (cond == 8)
					return IntId == 8 ? "31534-06.htm" : "31534-10.htm";
				break;

			case MAID_OF_LIDIA:
				if (cond == 9)
					return st.getQuestItemsCount(CONTRACT) > 0 ? "31532-01.htm" : "You have no Contract...";
				if (cond == 11 || cond == 12)
					return "31532-08.htm";
				if (cond == 13)
				{
					if (st.getQuestItemsCount(LIDIAS_DRESS) == 0)
						return "31532-08.htm";
					st.setCond(14);
					st.startQuestTimer("Lidia_wait", 60000);
					st.takeItems(LIDIAS_DRESS, 1);
					return "31532-09.htm";
				}
				if (cond == 14)
					return IntId == 14 ? "31532-10.htm" : "31532-09.htm";
				if (cond == 17)
				{
					st.set("id", "17");
					return "31532-23.htm";
				}
				if (cond == 18)
					return "31532-24.htm";
				break;

			case TOMBSTONE:
				if (cond == 11)
					return "31531-01.htm";
				if (cond == 12)
					return "31531-02.htm";
				if (cond == 13)
					return "31531-03.htm";
				break;

			case COFFIN:
				if (cond == 12)
				{
					st.setCond(13);
					st.giveItems(LIDIAS_DRESS, 1);
					return "31536-01.htm";
				}
				if (cond == 13)
					return "31531-03.htm";
				break;
		}
		return "noquest";
	}

	@Override
	public String onKill(NpcInstance npc, QuestState qs)
	{
		if (qs == null)
			return null;

		if (qs.getState() != STARTED)
			return null;

		int npcId = npc.getNpcId();
		int cond = qs.getCond();

		if (npcId == TRIOLS_PAWN && cond == 7)
		{
			qs.giveItems(SUSPICIOUS_TOTEM_DOLL_3, 1);
			qs.playSound(SOUND_MIDDLE);
			qs.setCond(8);
		}

		return null;
	}

	@Override
	public void onLoad()
	{
	}

	@Override
	public void onReload()
	{
	}

	@Override
	public void onShutdown()
	{
	}
}