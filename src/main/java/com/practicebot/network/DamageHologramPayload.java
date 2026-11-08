package com.practicebot.network;

import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;

public record DamageHologramPayload(double x, double y, double z, float amount) implements CustomPayload {
	public static final CustomPayload.Id<DamageHologramPayload> ID =
		new CustomPayload.Id<>(Identifier.of("practicebot", "damage_hologram"));

	public static final PacketCodec<RegistryByteBuf, DamageHologramPayload> CODEC = PacketCodec.of(
		(payload, buf) -> {
			buf.writeDouble(payload.x());
			buf.writeDouble(payload.y());
			buf.writeDouble(payload.z());
			buf.writeFloat(payload.amount());
		},
		buf -> new DamageHologramPayload(buf.readDouble(), buf.readDouble(), buf.readDouble(), buf.readFloat())
	);

	@Override
	public Id<? extends CustomPayload> getId() {
		return ID;
	}
}
