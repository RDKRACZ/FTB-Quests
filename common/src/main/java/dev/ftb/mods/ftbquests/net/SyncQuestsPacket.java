package dev.ftb.mods.ftbquests.net;

import dev.ftb.mods.ftblibrary.net.snm.BaseS2CPacket;
import dev.ftb.mods.ftblibrary.net.snm.PacketID;
import dev.ftb.mods.ftbquests.FTBQuests;
import dev.ftb.mods.ftbquests.quest.QuestFile;
import me.shedaniel.architectury.networking.NetworkManager;
import net.minecraft.network.FriendlyByteBuf;

/**
 * @author LatvianModder
 */
public class SyncQuestsPacket extends BaseS2CPacket {
	private final QuestFile file;

	SyncQuestsPacket(FriendlyByteBuf buffer) {
		file = FTBQuests.PROXY.createClientQuestFile();
		file.readNetDataFull(buffer);
	}

	public SyncQuestsPacket(QuestFile f) {
		file = f;
	}

	@Override
	public PacketID getId() {
		return FTBQuestsNetHandler.SYNC_QUESTS;
	}

	@Override
	public void write(FriendlyByteBuf buffer) {
		file.writeNetDataFull(buffer);
	}

	@Override
	public void handle(NetworkManager.PacketContext context) {
		file.load();
	}
}