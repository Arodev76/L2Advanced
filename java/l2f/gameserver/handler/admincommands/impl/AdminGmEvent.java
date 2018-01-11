package l2f.gameserver.handler.admincommands.impl;

import java.util.StringTokenizer;

import l2f.gameserver.handler.admincommands.IAdminCommandHandler;
import l2f.gameserver.model.Player;
import l2f.gameserver.network.serverpackets.NpcHtmlMessage;
import Elemental.managers.GmEventManager;
import Elemental.managers.GmEventManager.EventParameter;
import Elemental.managers.GmEventManager.StateEnum;

/**
 * Un engine simple para facilitar la tarea de los gms a la hora de hacer un evento manual
 * Desde aca se puede crear el evento, detenerlo, controlar los pjs registrados, etc
 *
 * @author Prims
 */
public class AdminGmEvent implements IAdminCommandHandler
{
	private static enum Commands
	{
		admin_gmevent
	}

	@Override
	public boolean useAdminCommand(Enum comm, String[] wordList, String fullString, Player activeChar)
	{
		final StringTokenizer st = new StringTokenizer(fullString, " ");
		st.nextToken();

		if (!st.hasMoreTokens())
		{
			showMainMenu(activeChar);
			return true;
		}

		switch (st.nextToken())
		{
			case "create":
				try
				{
					if (GmEventManager.getInstance().getEventStatus() != StateEnum.INACTIVE)
					{
						activeChar.sendMessage("There is already a created event");
						showMainMenu(activeChar);
						return true;
					}

					String eventName = st.nextToken();
					while (st.hasMoreTokens())
						eventName += " " + st.nextToken();

					GmEventManager.getInstance().createEvent(activeChar, eventName);
				}
				catch (Exception e)
				{
					activeChar.sendMessage("Uso: //gmevent create [eventName]");
				}
				break;
			case "setminlvl":
				try
				{
					final int minLvl = Integer.parseInt(st.nextToken());
					GmEventManager.getInstance().changeEventParameter(EventParameter.MIN_LVL, minLvl);
				}
				catch (Exception e)
				{
					activeChar.sendMessage("Uso: //gmevent setminlvl [minLvl]");
				}
				break;
			case "setmaxlvl":
				try
				{
					final int maxLvl = Integer.parseInt(st.nextToken());
					GmEventManager.getInstance().changeEventParameter(EventParameter.MAX_LVL, maxLvl);
				}
				catch (Exception e)
				{
					activeChar.sendMessage("Uso: //gmevent setmaxlvl [maxLvl]");
				}
				break;
			case "setmintime":
				try
				{
					final int minTime = Integer.parseInt(st.nextToken());
					GmEventManager.getInstance().changeEventParameter(EventParameter.MIN_TIME, minTime);
				}
				catch (Exception e)
				{
					activeChar.sendMessage("Uso: //gmevent setmintime [minOnlineTime]");
				}
				break;
			case "setmaxtime":
				try
				{
					final int maxTime = Integer.parseInt(st.nextToken());
					GmEventManager.getInstance().changeEventParameter(EventParameter.MAX_TIME, maxTime);
				}
				catch (Exception e)
				{
					activeChar.sendMessage("Uso: //gmevent setmaxtime [maxOnlineTime]");
				}
				break;
			case "setpvpevent":
				try
				{
					final int isPvp = Integer.parseInt(st.nextToken());
					GmEventManager.getInstance().changeEventParameter(EventParameter.IS_PVP_EVENT, isPvp);
				}
				catch (Exception e)
				{
					activeChar.sendMessage("Uso: //gmevent setpvpevent 0|1");
				}
				break;
			case "setpeaceevent":
				try
				{
					final int isPeace = Integer.parseInt(st.nextToken());
					GmEventManager.getInstance().changeEventParameter(EventParameter.IS_PEACE_EVENT, isPeace);
				}
				catch (Exception e)
				{
					activeChar.sendMessage("Uso: //gmevent setpeaceevent 0|1");
				}
				break;
			case "setautores":
				try
				{
					final int autoRes = Integer.parseInt(st.nextToken());
					GmEventManager.getInstance().changeEventParameter(EventParameter.IS_AUTO_RES, autoRes);
				}
				catch (Exception e)
				{
					activeChar.sendMessage("Uso: //gmevent setautores 0|1");
				}
				break;
			case "register":
				GmEventManager.getInstance().startRegistration();
				break;
			case "start":
				GmEventManager.getInstance().startEvent();
				break;
			case "stop":
				GmEventManager.getInstance().stopEvent();
				break;
			case "menu":
				showMainMenu(activeChar);
				break;
			default:
				return false;
		}

		showMainMenu(activeChar);
		return true;
	}

	protected void showMainMenu(Player activeChar)
	{
		// Si no hay ningun evento creado, mostramos la ventana de creacion
		if (GmEventManager.getInstance().getEventStatus() == StateEnum.INACTIVE)
		{
			final NpcHtmlMessage adminReply = new NpcHtmlMessage(0);
			adminReply.setFile("admin/events/gmevent_create.htm");
			activeChar.sendPacket(adminReply);
			return;
		}

		// Si el evento esta siendo creado, mostramos los comandos para controlar el evento
		final NpcHtmlMessage adminReply = new NpcHtmlMessage(0);
		adminReply.setFile("admin/events/gmevent_control.htm");

		// Segun que estado tenga el evento van a aparecer diferentes botones
		switch (GmEventManager.getInstance().getEventStatus())
		{
			case STARTING:
				adminReply.replace("%startEvent%", "<button value=\"Start Register\" action=\"bypass -h admin_gmevent register\" width=140 height=20 back=L2UI_ct1.button_df fore=L2UI_ct1.button_df>");
				break;
			case REGISTERING:
				adminReply.replace("%startEvent%", "<button value=\"Start Event\" action=\"bypass -h admin_gmevent start\" width=140 height=20 back=L2UI_ct1.button_df fore=L2UI_ct1.button_df>");
				break;
			default:
				adminReply.replace("%startEvent%", "");
				break;
		}

		// Botones para cambiar configs de evento
		final String pvpButton;
		if (GmEventManager.getInstance().isPvPEvent())
			pvpButton = "value=\"Disable\" action=\"bypass -h admin_gmevent setpvpevent 0\"";
		else
			pvpButton = "value=\"Enable\" action=\"bypass -h admin_gmevent setpvpevent 1\"";

		final String peaceButton;
		if (GmEventManager.getInstance().isPeaceEvent())
			peaceButton = "value=\"Disable\" action=\"bypass -h admin_gmevent setpeaceevent 0\"";
		else
			peaceButton = "value=\"Enable\" action=\"bypass -h admin_gmevent setpeaceevent 1\"";

		final String resButton;
		if (GmEventManager.getInstance().isAutoRes())
			resButton = "value=\"Disable\" action=\"bypass -h admin_gmevent setautores 0\"";
		else
			resButton = "value=\"Enable\" action=\"bypass -h admin_gmevent setautores 1\"";

		// Reemplazamos variables
		adminReply.replace("%eventName%", GmEventManager.getInstance().getEventName());
		adminReply.replace("%minLvl%", GmEventManager.getInstance().getMinLvl());
		adminReply.replace("%maxLvl%", GmEventManager.getInstance().getMaxLvl());
		adminReply.replace("%minTime%", GmEventManager.getInstance().getMinOnlineTime());
		adminReply.replace("%maxTime%", GmEventManager.getInstance().getMaxOnlineTime());
		adminReply.replace("%isPvPEvent%", GmEventManager.getInstance().isPvPEvent() ? "Enabled" : "Disabled");
		adminReply.replace("%isPeaceEvent%", GmEventManager.getInstance().isPeaceEvent() ? "Enabled" : "Disabled");
		adminReply.replace("%isAutoRes%", GmEventManager.getInstance().isAutoRes() ? "Enabled" : "Disabled");
		adminReply.replace("%pvpButton%", pvpButton);
		adminReply.replace("%peaceButton%", peaceButton);
		adminReply.replace("%resButton%", resButton);

		activeChar.sendPacket(adminReply);
	}

	@Override
	public Enum[] getAdminCommandEnum()
	{
		return Commands.values();
	}
}
