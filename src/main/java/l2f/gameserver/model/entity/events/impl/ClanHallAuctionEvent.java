package l2f.gameserver.model.entity.events.impl;

import java.util.Arrays;
import java.util.Calendar;
import java.util.List;

import org.apache.commons.lang3.StringUtils;

import l2f.commons.collections.MultiValueSet;
import l2f.commons.dao.JdbcEntityState;
import l2f.gameserver.dao.SiegeClanDAO;
import l2f.gameserver.instancemanager.PlayerMessageStack;
import l2f.gameserver.model.Player;
import l2f.gameserver.model.entity.events.actions.StartStopAction;
import l2f.gameserver.model.entity.events.objects.AuctionSiegeClanObject;
import l2f.gameserver.model.entity.events.objects.SiegeClanObject;
import l2f.gameserver.model.entity.residence.ClanHall;
import l2f.gameserver.model.pledge.Clan;
import l2f.gameserver.network.serverpackets.SystemMessage2;
import l2f.gameserver.network.serverpackets.components.SystemMsg;
import l2f.gameserver.tables.ClanTable;
import l2f.gameserver.templates.item.ItemTemplate;

/**
 * @author Arodev
 * @update 13:24/18.02.2018
 */
public class ClanHallAuctionEvent extends SiegeEvent<ClanHall, AuctionSiegeClanObject>
{
	private Calendar _endSiegeDate = Calendar.getInstance();

	public ClanHallAuctionEvent(MultiValueSet<String> set)
	{
		super(set);
	}

	@Override
	public void reCalcNextTime(boolean onStart)
	{
		clearActions();
		_onTimeActions.clear();

		Clan owner = getResidence().getOwner();

		_endSiegeDate.setTimeInMillis(0);
		// first start
		if (getResidence().getAuctionLength() == 0 && owner == null)
		{
			getResidence().getSiegeDate().setTimeInMillis(System.currentTimeMillis());
			getResidence().getSiegeDate().set(Calendar.DAY_OF_WEEK, Calendar.MONDAY);
			getResidence().getSiegeDate().set(Calendar.HOUR_OF_DAY, 15);
			getResidence().getSiegeDate().set(Calendar.MINUTE, 0);
			getResidence().getSiegeDate().set(Calendar.SECOND, 0);
			getResidence().getSiegeDate().set(Calendar.MILLISECOND, 0);

			getResidence().setAuctionLength(7);
			getResidence().setAuctionMinBid(getResidence().getBaseMinBid());
			getResidence().setJdbcState(JdbcEntityState.UPDATED);
			getResidence().update();

			_onTimeActions.clear();
			addOnTimeAction(0, new StartStopAction(EVENT, true));
			addOnTimeAction(getResidence().getAuctionLength() * 86400, new StartStopAction(EVENT, false));

			_endSiegeDate.setTimeInMillis(getResidence().getSiegeDate().getTimeInMillis() + (getResidence().getAuctionLength() * 86400000L));

			registerActions();
		}
		else if (getResidence().getAuctionLength() == 0 && owner != null)
		{
			// Clan Hall bought
		}		
		else
		{
	
			long endDate = getResidence().getSiegeDate().getTimeInMillis() + (getResidence().getAuctionLength() * 86400000L);
			// end date is far from the current date
			if (endDate <= System.currentTimeMillis())
			{
				getResidence().getSiegeDate().setTimeInMillis(System.currentTimeMillis() + 60000);
				_endSiegeDate.setTimeInMillis(System.currentTimeMillis() + 60000);
				_onTimeActions.clear();
				addOnTimeAction(0, new StartStopAction(EVENT, true));
				addOnTimeAction(60, new StartStopAction(EVENT, false));

				registerActions();
			}
			else
			{	
				_endSiegeDate.setTimeInMillis(getResidence().getSiegeDate().getTimeInMillis() + (getResidence().getAuctionLength() * 86400000L));
				_onTimeActions.clear();
				addOnTimeAction(0, new StartStopAction(EVENT, true));
				addOnTimeAction((int)getEndSiegeForCH(), new StartStopAction(EVENT, false));

				registerActions();
			}	
		}
	}

	@Override
	public void stopEvent(boolean step)
	{
		List<AuctionSiegeClanObject> siegeClanObjects = removeObjects(ATTACKERS);
		// sort with Max to Min
		AuctionSiegeClanObject[] clans = siegeClanObjects.toArray(new AuctionSiegeClanObject[siegeClanObjects.size()]);
		Arrays.sort(clans, SiegeClanObject.SiegeClanComparatorImpl.getInstance());

		Clan oldOwner = getResidence().getOwner();
		AuctionSiegeClanObject winnerSiegeClan = clans.length > 0 ? clans[0] : null;

		// if there is a winner (is more than one clan)
		if (winnerSiegeClan != null)
		{
			// message send out, we will return all the money
			SystemMessage2 msg = new SystemMessage2(SystemMsg.THE_CLAN_HALL_WHICH_WAS_PUT_UP_FOR_AUCTION_HAS_BEEN_AWARDED_TO_S1_CLAN).addString(winnerSiegeClan.getClan().getName());
			for(AuctionSiegeClanObject $siegeClan : siegeClanObjects)
			{
				Player player = $siegeClan.getClan().getLeader().getPlayer();
				if (player != null)
					player.sendPacket(msg);
				else
					PlayerMessageStack.getInstance().mailto($siegeClan.getClan().getLeaderId(), msg);

				if ($siegeClan != winnerSiegeClan)
				{
					long returnBid = $siegeClan.getParam() - (long)($siegeClan.getParam() * 0.1);

					$siegeClan.getClan().getWarehouse().addItem(ItemTemplate.ITEM_ID_ADENA, returnBid, "Clan Hall Big Return");
				}
			}
			// if there was ovner, refundable deposit + bid stake
			if (oldOwner != null)
			{
				oldOwner.getWarehouse().addItem(ItemTemplate.ITEM_ID_ADENA, getResidence().getDeposit(), "Clan Hall Auction Old Owner Bid");
				long givedBid = winnerSiegeClan.getParam();
				oldOwner.getWarehouse().addItem(ItemTemplate.ITEM_ID_ADENA, givedBid, "Clan Hall Auction Old Owner Bid");
			}	
				
			SiegeClanDAO.getInstance().delete(getResidence());

			

			getResidence().setAuctionLength(0);
			getResidence().setAuctionMinBid(0);
			getResidence().setAuctionDescription(StringUtils.EMPTY);
			getResidence().getSiegeDate().setTimeInMillis(0);
			getResidence().getLastSiegeDate().setTimeInMillis(0);
			getResidence().getOwnDate().setTimeInMillis(System.currentTimeMillis());
			getResidence().setJdbcState(JdbcEntityState.UPDATED);

			getResidence().changeOwner(winnerSiegeClan.getClan());
			getResidence().startCycleTask();
		}
		else
		{
			if (oldOwner != null)
			{
				Player player = oldOwner.getLeader().getPlayer();
				if (player != null)
					player.sendPacket(SystemMsg.THE_CLAN_HALL_WHICH_HAD_BEEN_PUT_UP_FOR_AUCTION_WAS_NOT_SOLD_AND_THEREFORE_HAS_BEEN_RELISTED);
				else
					PlayerMessageStack.getInstance().mailto(oldOwner.getLeaderId(), SystemMsg.THE_CLAN_HALL_WHICH_HAD_BEEN_PUT_UP_FOR_AUCTION_WAS_NOT_SOLD_AND_THEREFORE_HAS_BEEN_RELISTED.packet(null));
			}
			else
			{
				reCalcNewDate();
			}	
		}

		super.stopEvent(step);
	}
	
	private void reCalcNewDate()
	{
		getResidence().getSiegeDate().setTimeInMillis(System.currentTimeMillis());
		getResidence().getSiegeDate().set(Calendar.DAY_OF_WEEK, Calendar.MONDAY);
		getResidence().getSiegeDate().set(Calendar.HOUR_OF_DAY, 15);
		getResidence().getSiegeDate().set(Calendar.MINUTE, 0);
		getResidence().getSiegeDate().set(Calendar.SECOND, 0);
		getResidence().getSiegeDate().set(Calendar.MILLISECOND, 0);	
		getResidence().setJdbcState(JdbcEntityState.UPDATED);
		getResidence().update();		
		_endSiegeDate.setTimeInMillis(getResidence().getSiegeDate().getTimeInMillis() + (getResidence().getAuctionLength() * 86400000L));
		_onTimeActions.clear();
		addOnTimeAction(0, new StartStopAction(EVENT, true));
		addOnTimeAction(getResidence().getAuctionLength() * 86400, new StartStopAction(EVENT, false));	
		registerActions();		
	}
	
	@Override
	public boolean isParticle(Player player)
	{
		return false;
	}

	@Override
	public AuctionSiegeClanObject newSiegeClan(String type, int clanId, long param, long date)
	{
		Clan clan = ClanTable.getInstance().getClan(clanId);
		return clan == null ? null : new AuctionSiegeClanObject(type, clan, param, date);
	}
	
	public long getEndSiegeForCH()
	{
		//int days_to_add = getResidence().getAuctionLength();
		//long one_day = 86400000L;
		long start_date_msec = getResidence().getSiegeDate().getTimeInMillis();
		long end_date = getResidence().getSiegeDate().getTimeInMillis() + (getResidence().getAuctionLength() * 86400000L);
		long diff = end_date - start_date_msec;
		
		return diff / 1000;
	}
	public Calendar getEndSiegeDate()
	{
		return _endSiegeDate;
	}
}
