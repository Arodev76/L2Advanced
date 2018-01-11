package l2f.gameserver.handler.voicecommands.impl.BotReport;

import l2f.gameserver.model.Player;

/**
 * Captcha Event that contains 
 */
public class CaptchaEvent
{
	private final String actorName;
	private final String targetName;
	private final String correctCaptcha;
	private final long startDate;

	CaptchaEvent(Player actor, Player target, String correctCaptcha, long startDate)
	{
		actorName = actor.getName();
		targetName = target.getName();
		this.correctCaptcha = correctCaptcha;
		this.startDate = startDate;
	}

	public String getActorName()
	{
		return actorName;
	}

	public String getTargetName()
	{
		return targetName;
	}

	public String getCorrectCaptcha()
	{
		return correctCaptcha;
	}

	public long getStartDate()
	{
		return startDate;
	}
}