package com.feed_the_beast.ftbquests.quest;

import com.feed_the_beast.ftbquests.quest.rewards.QuestReward;
import com.feed_the_beast.ftbquests.quest.tasks.QuestTask;
import com.feed_the_beast.ftbquests.quest.tasks.QuestTaskData;
import it.unimi.dsi.fastutil.shorts.ShortCollection;
import net.minecraft.entity.player.EntityPlayer;

/**
 * @author LatvianModder
 */
public interface IProgressData
{
	String getTeamID();

	QuestTaskData getQuestTaskData(QuestTask task);

	boolean claimReward(EntityPlayer player, QuestReward reward);

	ShortCollection getClaimedRewards(EntityPlayer player);

	default boolean isRewardClaimed(EntityPlayer player, QuestReward reward)
	{
		return getClaimedRewards(player).contains(reward.id);
	}

	void syncTask(QuestTaskData data);

	void removeTask(QuestTask task);

	void createTaskData(QuestTask task);

	void unclaimReward(QuestReward reward);
}