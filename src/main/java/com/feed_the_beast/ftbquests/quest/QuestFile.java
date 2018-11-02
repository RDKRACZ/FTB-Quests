package com.feed_the_beast.ftbquests.quest;

import com.feed_the_beast.ftblib.lib.config.ConfigGroup;
import com.feed_the_beast.ftblib.lib.config.ConfigItemStack;
import com.feed_the_beast.ftblib.lib.config.ConfigTimer;
import com.feed_the_beast.ftblib.lib.icon.Color4I;
import com.feed_the_beast.ftblib.lib.icon.Icon;
import com.feed_the_beast.ftblib.lib.icon.IconAnimation;
import com.feed_the_beast.ftblib.lib.math.MathUtils;
import com.feed_the_beast.ftblib.lib.math.Ticks;
import com.feed_the_beast.ftbquests.events.ObjectCompletedEvent;
import com.feed_the_beast.ftbquests.item.ItemMissing;
import com.feed_the_beast.ftbquests.item.LootRarity;
import com.feed_the_beast.ftbquests.quest.reward.QuestReward;
import com.feed_the_beast.ftbquests.quest.task.QuestTask;
import com.feed_the_beast.ftbquests.quest.task.QuestTaskType;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import net.minecraft.init.Items;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.TextComponentTranslation;
import net.minecraftforge.common.util.Constants;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

/**
 * @author LatvianModder
 */
public abstract class QuestFile extends QuestObject
{
	public static final int VERSION = 1;

	public final List<QuestChapter> chapters;
	public final List<QuestVariable> variables;

	private final Int2ObjectOpenHashMap<QuestObjectBase> intMap;
	private final Map<String, QuestObject> map;
	public Collection<QuestTask> allTasks;

	public final List<ItemStack> emergencyItems;
	public Ticks emergencyItemsCooldown;
	public String soundTask, soundQuest, soundChapter, soundFile;
	public final ResourceLocation[] lootTables;
	public int lootSize;
	public Color4I colCompleted, colStarted, colNotStarted, colCantStart;
	public boolean defaultRewardTeam;
	public int fileVersion;

	public QuestFile()
	{
		id = "*";
		chapters = new ArrayList<>();
		variables = new ArrayList<>();

		intMap = new Int2ObjectOpenHashMap<>();
		map = new HashMap<>();

		emergencyItems = new ArrayList<>();
		emergencyItems.add(new ItemStack(Items.APPLE));
		emergencyItemsCooldown = Ticks.MINUTE.x(5);
		soundTask = "";
		soundQuest = "";
		soundChapter = "";
		soundFile = "minecraft:ui.toast.challenge_complete";

		lootTables = new ResourceLocation[LootRarity.VALUES.length];

		for (LootRarity rarity : LootRarity.VALUES)
		{
			lootTables[rarity.ordinal()] = rarity.getLootTable();
		}

		lootSize = 27;

		colCompleted = Color4I.rgb(0x56FF56);
		colStarted = Color4I.rgb(0x00FFFF);
		colNotStarted = Color4I.rgb(0xFFFFFF);
		colCantStart = Color4I.rgb(0x999999);

		defaultRewardTeam = false;
		fileVersion = 0;
	}

	public abstract boolean isClient();

	@Override
	public QuestFile getQuestFile()
	{
		return this;
	}

	@Override
	public QuestObjectType getObjectType()
	{
		return QuestObjectType.FILE;
	}

	@Override
	public String getID()
	{
		return id;
	}

	@Override
	public long getProgress(ITeamData data)
	{
		long progress = 0L;

		for (QuestChapter chapter : chapters)
		{
			progress += chapter.getProgress(data);
		}

		return progress;
	}

	@Override
	public long getMaxProgress()
	{
		long maxProgress = 0L;

		for (QuestChapter chapter : chapters)
		{
			maxProgress += chapter.getMaxProgress();
		}

		return maxProgress;
	}

	@Override
	public int getRelativeProgress(ITeamData data)
	{
		int progress = 0;

		for (QuestChapter chapter : chapters)
		{
			progress += chapter.getRelativeProgress(data);
		}

		return fixRelativeProgress(progress, chapters.size());
	}

	@Override
	public boolean isComplete(ITeamData data)
	{
		for (QuestChapter chapter : chapters)
		{
			if (!chapter.isComplete(data))
			{
				return false;
			}
		}

		return true;
	}

	@Override
	public void onCompleted(ITeamData data)
	{
		super.onCompleted(data);
		new ObjectCompletedEvent.FileEvent(data, this).post();
	}

	@Override
	public void resetProgress(ITeamData data, boolean dependencies)
	{
		for (QuestChapter chapter : chapters)
		{
			chapter.resetProgress(data, dependencies);
		}
	}

	@Override
	public void completeInstantly(ITeamData data, boolean dependencies)
	{
		for (QuestChapter chapter : chapters)
		{
			chapter.completeInstantly(data, dependencies);
		}
	}

	@Override
	public void deleteSelf()
	{
		invalid = true;
	}

	@Override
	public void deleteChildren()
	{
		for (QuestChapter chapter : chapters)
		{
			chapter.deleteChildren();
			chapter.invalid = true;
		}

		chapters.clear();

		for (QuestVariable variable : variables)
		{
			variable.deleteChildren();
			variable.invalid = true;
		}

		variables.clear();
	}

	@Nullable
	@Deprecated
	public QuestObject get(String id)
	{
		if (id.isEmpty())
		{
			return null;
		}
		else if (id.charAt(0) == '*')
		{
			return this;
		}

		QuestObject object = map.get(id);
		return object == null || object.invalid ? null : object;
	}

	@Nullable
	public QuestObjectBase getBase(int id)
	{
		if (id == 0)
		{
			return this;
		}

		QuestObjectBase object = intMap.get(id);
		return object == null || object.invalid ? null : object;
	}

	@Nullable
	public QuestObject get(int id)
	{
		QuestObjectBase object = getBase(id);
		return object instanceof QuestObject ? (QuestObject) object : null;
	}

	@Nullable
	public QuestObjectBase remove(int id)
	{
		QuestObjectBase object = intMap.remove(id);

		if (object != null)
		{
			if (object instanceof QuestObject)
			{
				map.remove(((QuestObject) object).getID());
			}

			object.invalid = true;
			refreshIDMap();
			return object;
		}

		return null;
	}

	@Nullable
	@Deprecated
	public QuestChapter getChapter(String id)
	{
		QuestObject object = get(id);
		return object instanceof QuestChapter ? (QuestChapter) object : null;
	}

	@Nullable
	public QuestChapter getChapter(int id)
	{
		QuestObjectBase object = getBase(id);
		return object instanceof QuestChapter ? (QuestChapter) object : null;
	}

	@Nullable
	@Deprecated
	public Quest getQuest(String id)
	{
		QuestObject object = get(id);
		return object instanceof Quest ? (Quest) object : null;
	}

	@Nullable
	public Quest getQuest(int id)
	{
		QuestObjectBase object = getBase(id);
		return object instanceof Quest ? (Quest) object : null;
	}

	@Nullable
	@Deprecated
	public QuestTask getTask(String id)
	{
		QuestObject object = get(id);
		return object instanceof QuestTask ? (QuestTask) object : null;
	}

	@Nullable
	public QuestTask getTask(int id)
	{
		QuestObjectBase object = getBase(id);
		return object instanceof QuestTask ? (QuestTask) object : null;
	}

	@Nullable
	@Deprecated
	public QuestVariable getVariable(String id)
	{
		QuestObject object = get(id);
		return object instanceof QuestVariable ? (QuestVariable) object : null;
	}

	@Nullable
	public QuestVariable getVariable(int id)
	{
		QuestObjectBase object = getBase(id);
		return object instanceof QuestVariable ? (QuestVariable) object : null;
	}

	@Nullable
	public QuestReward getReward(int id)
	{
		QuestObjectBase object = getBase(id);
		return object instanceof QuestReward ? (QuestReward) object : null;
	}

	public void refreshIDMap()
	{
		clearCachedData();

		map.clear();
		intMap.clear();
		List<QuestTask> tasks = new ObjectArrayList<>();

		for (int i = 0; i < chapters.size(); i++)
		{
			QuestChapter chapter = chapters.get(i);
			chapter.chapterIndex = i;
			map.put(chapter.getID(), chapter);
			intMap.put(chapter.uid, chapter);

			for (Quest quest : chapter.quests)
			{
				map.put(quest.getID(), quest);
				intMap.put(quest.uid, quest);

				for (QuestTask task : quest.tasks)
				{
					map.put(task.getID(), task);
					intMap.put(task.uid, task);
					tasks.add(task);
				}

				for (QuestReward reward : quest.rewards)
				{
					intMap.put(reward.uid, reward);
				}
			}
		}

		for (int i = 0; i < variables.size(); i++)
		{
			QuestVariable variable = variables.get(i);
			variable.index = (short) i;
			map.put(variable.getID(), variable);
			intMap.put(variable.uid, variable);
		}

		allTasks = Collections.unmodifiableList(Arrays.asList(tasks.toArray(new QuestTask[0])));
		map.put("*", this);
		intMap.put(0, this);

		clearCachedData();
	}

	@Nullable
	public QuestObject create(QuestObjectType type, int parent, NBTTagCompound nbt)
	{
		switch (type)
		{
			case CHAPTER:
				return new QuestChapter(this);
			case QUEST:
			{
				QuestChapter chapter = getChapter(parent);

				if (chapter != null)
				{
					return new Quest(chapter);
				}

				return null;
			}
			case TASK:
			{
				Quest quest = getQuest(parent);

				if (quest != null)
				{
					return QuestTaskType.createTask(quest, nbt.getString("type"));
				}

				return null;
			}
			case VARIABLE:
				return new QuestVariable(this);
			default:
				return null;
		}
	}

	@Override
	public final void writeData(NBTTagCompound nbt)
	{
		writeCommonData(nbt);
		nbt.setInteger("version", VERSION);
		nbt.setBoolean("default_reward_team", defaultRewardTeam);

		NBTTagList list = new NBTTagList();

		for (QuestChapter chapter : chapters)
		{
			NBTTagCompound nbt1 = new NBTTagCompound();
			chapter.writeData(nbt1);
			nbt1.setString("id", chapter.id);
			nbt1.setInteger("uid", chapter.uid);
			list.appendTag(nbt1);
		}

		nbt.setTag("chapters", list);

		if (!variables.isEmpty())
		{
			list = new NBTTagList();

			for (QuestVariable variable : variables)
			{
				NBTTagCompound nbt1 = new NBTTagCompound();
				variable.writeData(nbt1);
				nbt1.setString("id", variable.id);
				nbt1.setInteger("uid", variable.uid);
				list.appendTag(nbt1);
			}

			nbt.setTag("variables", list);
		}

		if (!emergencyItems.isEmpty())
		{
			list = new NBTTagList();

			for (ItemStack stack : emergencyItems)
			{
				list.appendTag(ItemMissing.write(stack, true));
			}

			nbt.setTag("emergency_items", list);
		}

		nbt.setString("emergency_items_cooldown", emergencyItemsCooldown.toString());

		for (LootRarity rarity : LootRarity.VALUES)
		{
			if (!lootTables[rarity.ordinal()].equals(rarity.getLootTable()))
			{
				nbt.setString(rarity.getName() + "_loot_table", lootTables[rarity.ordinal()].toString());
			}
		}

		nbt.setShort("loot_size", (short) lootSize);
	}

	@Override
	public final void readData(NBTTagCompound nbt)
	{
		readCommonData(nbt);
		fileVersion = nbt.getInteger("version");
		defaultRewardTeam = nbt.getBoolean("default_reward_team");

		chapters.clear();

		NBTTagList list = nbt.getTagList("chapters", Constants.NBT.TAG_COMPOUND);

		for (int i = 0; i < list.tagCount(); i++)
		{
			QuestChapter chapter = new QuestChapter(this);
			chapter.readData(list.getCompoundTagAt(i));
			chapter.chapterIndex = chapters.size();
			chapters.add(chapter);
		}

		variables.clear();

		list = nbt.getTagList("variables", Constants.NBT.TAG_COMPOUND);

		for (int i = 0; i < list.tagCount(); i++)
		{
			QuestVariable variable = new QuestVariable(this);
			variable.readData(list.getCompoundTagAt(i));
			variable.index = (short) variables.size();
			variables.add(variable);
		}

		for (QuestChapter chapter : chapters)
		{
			for (Quest quest : chapter.quests)
			{
				quest.verifyDependencies();
			}
		}

		refreshIDMap();

		emergencyItems.clear();

		list = nbt.getTagList("emergency_items", Constants.NBT.TAG_COMPOUND);

		for (int i = 0; i < list.tagCount(); i++)
		{
			ItemStack stack = ItemMissing.read(list.getCompoundTagAt(i));

			if (!stack.isEmpty())
			{
				emergencyItems.add(stack);
			}
		}

		Ticks t = Ticks.get(nbt.getString("emergency_items_cooldown"));
		emergencyItemsCooldown = t.hasTicks() ? t : Ticks.MINUTE.x(5);

		for (LootRarity rarity : LootRarity.VALUES)
		{
			String s = nbt.getString(rarity.getName() + "_loot_table");
			lootTables[rarity.ordinal()] = s.isEmpty() ? rarity.getLootTable() : new ResourceLocation(s);
		}

		lootSize = nbt.getShort("loot_size") & 0xFFFF;

		if (lootSize == 0)
		{
			lootSize = 27;
		}
	}

	@Nullable
	public abstract ITeamData getData(String team);

	public abstract Collection<? extends ITeamData> getAllData();

	public abstract void deleteObject(int id);

	@Override
	public Icon getAltIcon()
	{
		List<Icon> list = new ArrayList<>();

		for (QuestChapter chapter : chapters)
		{
			list.add(chapter.getIcon());
		}

		return IconAnimation.fromList(list, false);
	}

	@Override
	public ITextComponent getAltDisplayName()
	{
		return new TextComponentTranslation("ftbquests.file");
	}

	@Override
	public void getConfig(ConfigGroup config)
	{
		config.addList("emergency_items", emergencyItems, new ConfigItemStack(ItemStack.EMPTY), ConfigItemStack::new, ConfigItemStack::getStack);

		config.add("emergency_items_cooldown", new ConfigTimer(Ticks.NO_TICKS)
		{
			@Override
			public Ticks getTimer()
			{
				return emergencyItemsCooldown;
			}

			@Override
			public void setTimer(Ticks t)
			{
				emergencyItemsCooldown = t;
			}
		}, new ConfigTimer(Ticks.MINUTE.x(5)));

		ConfigGroup lootGroup = config.getGroup("loot_tables");
		Pattern pattern = Pattern.compile("[a-z0-9_]+:.+");

		for (LootRarity r : LootRarity.VALUES)
		{
			lootGroup.addString(r.getName(), () -> lootTables[r.ordinal()].toString(), v -> lootTables[r.ordinal()] = v.equals(r.getLootTable().toString()) ? r.getLootTable() : new ResourceLocation(v), r.getLootTable().toString(), pattern).setDisplayName(new TextComponentTranslation(r.getTranslationKey()));
		}

		config.addInt("loot_size", () -> lootSize, v -> lootSize = v, 27, 1, 1024);
		config.addBool("default_reward_team", () -> defaultRewardTeam, v -> defaultRewardTeam = v, false);
	}

	@Override
	public void clearCachedData()
	{
		super.clearCachedData();

		for (QuestChapter chapter : chapters)
		{
			chapter.clearCachedData();
		}
	}

	public int readID(int id)
	{
		while (id == 0 || intMap.get(id) != null)
		{
			id = MathUtils.RAND.nextInt();
		}

		return id;
	}

	public int getID(String stringID)
	{
		return 0;
	}
}