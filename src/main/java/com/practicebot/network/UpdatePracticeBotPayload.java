package com.practicebot.network;

import com.practicebot.bot.BotSettings;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;

public record UpdatePracticeBotPayload(BotSettings settings) implements CustomPayload {
	public static final CustomPayload.Id<UpdatePracticeBotPayload> ID =
		new CustomPayload.Id<>(Identifier.of("practicebot", "update_bot"));

	public static final PacketCodec<RegistryByteBuf, UpdatePracticeBotPayload> CODEC = PacketCodec.tuple(
		BotSettings.PACKET_CODEC, UpdatePracticeBotPayload::settings,
		UpdatePracticeBotPayload::new
	);

	@Override
	public Id<? extends CustomPayload> getId() {
		return ID;
	}
}
