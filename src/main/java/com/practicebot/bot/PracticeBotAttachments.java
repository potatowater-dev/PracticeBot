package com.practicebot.bot;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.fabricmc.fabric.api.attachment.v1.AttachmentRegistry;
import net.fabricmc.fabric.api.attachment.v1.AttachmentSyncPredicate;
import net.fabricmc.fabric.api.attachment.v1.AttachmentType;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.MathHelper;

public final class PracticeBotAttachments {
	public static final AttachmentType<Data> DATA = AttachmentRegistry.create(
		Identifier.of("practicebot", "bot_data"),
		builder -> builder
			.persistent(Data.CODEC)
			.syncWith(Data.PACKET_CODEC, AttachmentSyncPredicate.all())
	);

	private PracticeBotAttachments() {
	}

	public static void bootstrap() {
		if (DATA == null) {
			throw new IllegalStateException("Practice bot attachment failed to register");
		}
	}

	public record Data(
		String mode,
		int shieldDisabledTicks,
		BotSettings settings,
		int totemPops,
		float lastDamage,
		double homeX,
		double homeY,
		double homeZ,
		boolean hasHome,
		int ticksSinceDamage,
		int hurtCooldownRemaining
	) {
		public static final Codec<Data> CODEC = RecordCodecBuilder.create(instance -> instance.group(
			Codec.STRING.fieldOf("mode").forGetter(Data::mode),
			Codec.INT.optionalFieldOf("shield_disabled_ticks", 0).forGetter(Data::shieldDisabledTicks),
			BotSettings.CODEC.optionalFieldOf("settings", BotSettings.DEFAULT).forGetter(Data::settings),
			Codec.INT.optionalFieldOf("totem_pops", 0).forGetter(Data::totemPops),
			Codec.FLOAT.optionalFieldOf("last_damage", 0.0F).forGetter(Data::lastDamage),
			Codec.DOUBLE.optionalFieldOf("home_x", 0.0).forGetter(Data::homeX),
			Codec.DOUBLE.optionalFieldOf("home_y", 0.0).forGetter(Data::homeY),
			Codec.DOUBLE.optionalFieldOf("home_z", 0.0).forGetter(Data::homeZ),
			Codec.BOOL.optionalFieldOf("has_home", false).forGetter(Data::hasHome),
			Codec.INT.optionalFieldOf("ticks_since_damage", 0).forGetter(Data::ticksSinceDamage),
			Codec.INT.optionalFieldOf("hurt_cooldown_remaining", 0).forGetter(Data::hurtCooldownRemaining)
		).apply(instance, Data::new));

		public static final PacketCodec<RegistryByteBuf, Data> PACKET_CODEC = PacketCodec.of(Data::write, Data::read);

		private static void write(Data data, RegistryByteBuf buf) {
			buf.writeString(data.mode);
			buf.writeVarInt(data.shieldDisabledTicks);
			BotSettings.PACKET_CODEC.encode(buf, data.settings);
			buf.writeVarInt(data.totemPops);
			buf.writeFloat(data.lastDamage);
			buf.writeDouble(data.homeX);
			buf.writeDouble(data.homeY);
			buf.writeDouble(data.homeZ);
			buf.writeBoolean(data.hasHome);
			buf.writeVarInt(data.ticksSinceDamage);
			buf.writeVarInt(data.hurtCooldownRemaining);
		}

		private static Data read(RegistryByteBuf buf) {
			return new Data(
				buf.readString(),
				buf.readVarInt(),
				BotSettings.PACKET_CODEC.decode(buf),
				buf.readVarInt(),
				buf.readFloat(),
				buf.readDouble(),
				buf.readDouble(),
				buf.readDouble(),
				buf.readBoolean(),
				buf.readVarInt(),
				buf.readVarInt()
			);
		}

		public static Data create(String mode, BotSettings settings, int shieldDisabledTicks) {
			return new Data(mode, Math.max(0, shieldDisabledTicks), settings.clamped(), 0, 0.0F, 0.0, 0.0, 0.0, false, 9999, 0);
		}

		public Data withShieldDisabledTicks(int ticks) {
			return new Data(this.mode, Math.max(0, ticks), this.settings, this.totemPops, this.lastDamage, this.homeX, this.homeY, this.homeZ, this.hasHome, this.ticksSinceDamage, this.hurtCooldownRemaining);
		}

		public Data withSettings(BotSettings settings) {
			return new Data(this.mode, this.shieldDisabledTicks, settings.clamped(), this.totemPops, this.lastDamage, this.homeX, this.homeY, this.homeZ, this.hasHome, this.ticksSinceDamage, this.hurtCooldownRemaining);
		}

		public Data withMode(String mode) {
			return new Data(mode, this.shieldDisabledTicks, this.settings, this.totemPops, this.lastDamage, this.homeX, this.homeY, this.homeZ, this.hasHome, this.ticksSinceDamage, this.hurtCooldownRemaining);
		}

		public Data withTotemPops(int totemPops) {
			return new Data(this.mode, this.shieldDisabledTicks, this.settings, Math.max(0, totemPops), this.lastDamage, this.homeX, this.homeY, this.homeZ, this.hasHome, this.ticksSinceDamage, this.hurtCooldownRemaining);
		}

		public Data withLastDamage(float lastDamage) {
			return new Data(this.mode, this.shieldDisabledTicks, this.settings, this.totemPops, Math.max(0.0F, lastDamage), this.homeX, this.homeY, this.homeZ, this.hasHome, 0, this.hurtCooldownRemaining);
		}

		public Data withHome(double x, double y, double z) {
			return new Data(this.mode, this.shieldDisabledTicks, this.settings, this.totemPops, this.lastDamage, x, y, z, true, this.ticksSinceDamage, this.hurtCooldownRemaining);
		}

		public Data tickTimers() {
			int since = MathHelper.clamp(this.ticksSinceDamage + 1, 0, 1_000_000);
			int hurt = Math.max(0, this.hurtCooldownRemaining - 1);
			if (since == this.ticksSinceDamage && hurt == this.hurtCooldownRemaining) {
				return this;
			}
			return new Data(this.mode, this.shieldDisabledTicks, this.settings, this.totemPops, this.lastDamage, this.homeX, this.homeY, this.homeZ, this.hasHome, since, hurt);
		}

		public Data withHurtCooldown(int ticks) {
			return new Data(this.mode, this.shieldDisabledTicks, this.settings, this.totemPops, this.lastDamage, this.homeX, this.homeY, this.homeZ, this.hasHome, this.ticksSinceDamage, Math.max(0, ticks));
		}

		public Data resetPops() {
			return withTotemPops(0);
		}
	}
}
