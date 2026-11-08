package com.practicebot.network;

import com.practicebot.bot.BotMode;
import com.practicebot.bot.BotSettings;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;

public record SpawnPracticeBotPayload(String modeId, BotSettings settings) implements CustomPayload {
	public static final CustomPayload.Id<SpawnPracticeBotPayload> ID =
		new CustomPayload.Id<>(Identifier.of("practicebot", "spawn_bot"));

	public static final PacketCodec<RegistryByteBuf, SpawnPracticeBotPayload> CODEC = PacketCodec.tuple(
		PacketCodecs.STRING, SpawnPracticeBotPayload::modeId,
		BotSettings.PACKET_CODEC, SpawnPracticeBotPayload::settings,
		SpawnPracticeBotPayload::new
	);

	public SpawnPracticeBotPayload(BotMode mode, BotSettings settings) {
		this(mode.getId(), settings);
	}

	@Override
	public Id<? extends CustomPayload> getId() {
		return ID;
	}
}
