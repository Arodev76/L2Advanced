package l2f.gameserver.data.xml.parser;

import java.io.File;
import java.util.Iterator;

import org.dom4j.Element;

import l2f.commons.configuration.Config;
import l2f.commons.data.xml.AbstractFileParser;
import l2f.gameserver.data.xml.holder.SoulCrystalHolder;
import l2f.gameserver.templates.SoulCrystal;

/**
 * @author VISTALL
 * @update  00:15/18.02.2018
 */
public final class SoulCrystalParser extends AbstractFileParser<SoulCrystalHolder>
{
	private static final SoulCrystalParser _instance = new SoulCrystalParser();

	public static SoulCrystalParser getInstance()
	{
		return _instance;
	}

	private SoulCrystalParser()
	{
		super(SoulCrystalHolder.getInstance());
	}

	@Override
	public File getXMLFile()
	{
		return new File(Config.DATAPACK_ROOT, "data/soul_crystals.xml");
	}

	@Override
	public String getDTDFileName()
	{
		return "soul_crystals.dtd";
	}

	@Override
	protected void readData(Element rootElement) throws Exception
	{
		for (Iterator<Element> iterator = rootElement.elementIterator("crystal"); iterator.hasNext();)
		{
			Element element = iterator.next();
			int itemId = Integer.parseInt(element.attributeValue("item_id"));
			int level = Integer.parseInt(element.attributeValue("level"));
			int nextItemId = Integer.parseInt(element.attributeValue("next_item_id"));
			int cursedNextItemId = element.attributeValue("cursed_next_item_id") == null ? 0 : Integer.parseInt(element.attributeValue("cursed_next_item_id"));

			getHolder().addCrystal(new SoulCrystal(itemId, level, nextItemId, cursedNextItemId));
		}
	}
}
