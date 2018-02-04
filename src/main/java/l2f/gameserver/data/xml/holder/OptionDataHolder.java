package l2f.gameserver.data.xml.holder;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.napile.primitive.maps.IntObjectMap;
import org.napile.primitive.maps.impl.HashIntObjectMap;

import l2f.commons.data.xml.AbstractHolder;
import l2f.gameserver.model.Options.AugmentationFilter;
import l2f.gameserver.templates.OptionDataTemplate;

/**
 * @author VISTALL
 * @date 20:35/19.05.2011
 */
public final class OptionDataHolder extends AbstractHolder
{
	private static final OptionDataHolder _instance = new OptionDataHolder();

	private final IntObjectMap<OptionDataTemplate> _templates = new HashIntObjectMap<OptionDataTemplate>();

	public static OptionDataHolder getInstance()
	{
		return _instance;
	}

	public void addTemplate(OptionDataTemplate template)
	{
		_templates.put(template.getId(), template);
	}

	public OptionDataTemplate getTemplate(int id)
	{
		return _templates.get(id);
	}

	@Override
	public int size()
	{
		return _templates.size();
	}

	@Override
	public void clear()
	{
		_templates.clear();
	}

	/**
	 * @param filter
	 * @return Prims - Devuelve todos las options de augmentations usando un filtro en especial
	 */
	public Collection<OptionDataTemplate> getUniqueOptions(AugmentationFilter filter)
	{
		if (filter == AugmentationFilter.NONE)
			return _templates.values();

		final Map<Integer, OptionDataTemplate> options = new HashMap<>();
		switch (filter)
		{
			case ACTIVE_SKILL:
			{
				for (OptionDataTemplate option : _templates.values())
				{
					// Solo activas
					if (!option.getTriggerList().isEmpty())
						continue;

					if (option.getSkills().isEmpty() || !option.getSkills().get(0).isActive())
						continue;

					// Chequeamos que el lvl de esta skill si ya fue agregado, sea mayor al anterior
					if (!options.containsKey(option.getSkills().get(0).getId())	|| options.get(option.getSkills().get(0).getId()).getSkills().get(0).getLevel() < option.getSkills().get(0).getLevel())
						options.put(option.getSkills().get(0).getId(), option);
				}
				break;
			}
			case PASSIVE_SKILL:
			{
				for (OptionDataTemplate option : _templates.values())
				{
					// Solo pasivas
					if (!option.getTriggerList().isEmpty())
						continue;

					if (option.getSkills().isEmpty() || !option.getSkills().get(0).isPassive())
						continue;

					// Chequeamos que el lvl de esta skill si ya fue agregado, sea mayor al anterior
					if (!options.containsKey(option.getSkills().get(0).getId())	|| options.get(option.getSkills().get(0).getId()).getSkills().get(0).getLevel() < option.getSkills().get(0).getLevel())
						options.put(option.getSkills().get(0).getId(), option);
				}
				break;
			}
			case CHANCE_SKILL:
			{
				for (OptionDataTemplate option : _templates.values())
				{
					// Solo de chance
					if (option.getTriggerList().isEmpty())
						continue;

					// Chequeamos que el lvl de esta skill si ya fue agregado, sea mayor al anterior
					if (!options.containsKey(option.getTriggerList().get(0).getSkillId()) || options.get(option.getTriggerList().get(0).getSkillId()).getTriggerList().get(0).getSkillLevel() < option.getTriggerList().get(0).getSkillLevel())
						options.put(option.getTriggerList().get(0).getSkillId(), option);
				}
				break;
			}
			case STATS:
			{
				for (OptionDataTemplate option : _templates.values())
				{
					// La lista de opciones de stats es hardcoded porque no tenemos forma de saber sino, son solo 5
					switch (option.getId())
					{
						case 16341: // +1 STR
						case 16342: // +1 CON
						case 16343: // +1 INT
						case 16344: // +1 MEN
							options.put(option.getId(), option);
							break;
					}
				}
				break;
			}
		}

		// Ordenamos la lista de augmentations segun el id de su skill
		final List<OptionDataTemplate> augs = new ArrayList<>(options.values());
		Collections.sort(augs, new AugmentationComparator());

		return augs;
	}

	// Comparator para ordenar la lista de augmentations segun el id de su skill
	protected static class AugmentationComparator implements Comparator<OptionDataTemplate>
	{
		@Override
		public int compare(final OptionDataTemplate left, final OptionDataTemplate right)
		{
			if (left.getSkills().isEmpty() || right.getSkills().isEmpty())
				return 0;

			return Integer.valueOf(left.getSkills().get(0).getId()).compareTo(right.getSkills().get(0).getId());
		}
	}
	
//	public OptionDataTemplate getAugment(int augId)
//	{
//		for (Entry<OptionDataTemplate> opt : _templates.entrySet())
//		{
//			OptionDataTemplate template = opt.getValue();
//			System.out.print(" Point A ");
//			if (template.getSkills().size() > 0)
//			{
//				System.out.print("- Point B ");
//				for (Skill skill : template.getSkills())
//				{
//					System.out.print("- Point C ");
//					if (skill.getId() == augId)
//					{
//						System.out.println("Get skill by name:" + skill.getName() + "m id: " + skill.getId());
//						return template;
//					}
//				}
//				System.out.println("");
//			}
//		}
//		return null;
//	}
}
