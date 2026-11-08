package com.practicebot.bot;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.practicebot.PracticeBotMod;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.util.math.MathHelper;

public record BotSettings(
	int shieldDisableTicks,
	ArmorType armorType,
	float windBoost,
	boolean invulnerable,
	boolean infiniteTotems,
	String skinName,
	boolean noKnockback,
	Extra extra
) {
	public record Extra(
		String displayName,
		boolean lookAtPlayer,
		float moveRadius,
		float moveSpeed,
		float gapHealthThreshold,
		boolean regen,
		int regenDelayTicks,
		boolean glowing,
		ShowHpMode showHp,
		int hurtCooldownTicks
	) {
		public static final Extra DEFAULT = new Extra(
			"",
			true,
			2.5F,
			0.15F,
			8.0F,
			false,
			40,
			false,
			ShowHpMode.WHEN_HURT,
			10
		);

		public static final Codec<Extra> CODEC = RecordCodecBuilder.create(instance -> instance.group(
			Codec.STRING.optionalFieldOf("display_name", DEFAULT.displayName).forGetter(Extra::displayName),
			Codec.BOOL.optionalFieldOf("look_at_player", DEFAULT.lookAtPlayer).forGetter(Extra::lookAtPlayer),
			Codec.FLOAT.optionalFieldOf("move_radius", DEFAULT.moveRadius).forGetter(Extra::moveRadius),
			Codec.FLOAT.optionalFieldOf("move_speed", DEFAULT.moveSpeed).forGetter(Extra::moveSpeed),
			Codec.FLOAT.optionalFieldOf("gap_health_threshold", DEFAULT.gapHealthThreshold).forGetter(Extra::gapHealthThreshold),
			Codec.BOOL.optionalFieldOf("regen", DEFAULT.regen).forGetter(Extra::regen),
			Codec.INT.optionalFieldOf("regen_delay_ticks", DEFAULT.regenDelayTicks).forGetter(Extra::regenDelayTicks),
			Codec.BOOL.optionalFieldOf("glowing", DEFAULT.glowing).forGetter(Extra::glowing),
			Codec.STRING.optionalFieldOf("show_hp", DEFAULT.showHp.getId()).xmap(
				id -> {
					ShowHpMode mode = ShowHpMode.fromId(id);
					return mode == null ? DEFAULT.showHp : mode;
				},
				ShowHpMode::getId
			).forGetter(Extra::showHp),
			Codec.INT.optionalFieldOf("hurt_cooldown_ticks", DEFAULT.hurtCooldownTicks).forGetter(Extra::hurtCooldownTicks)
		).apply(instance, Extra::new));

		public Extra clamped() {
			return new Extra(
				displayName == null ? "" : displayName.trim(),
				lookAtPlayer,
				MathHelper.clamp(moveRadius, 0.5F, 16.0F),
				MathHelper.clamp(moveSpeed, 0.01F, 1.0F),
				MathHelper.clamp(gapHealthThreshold, 1.0F, 40.0F),
				regen,
				MathHelper.clamp(regenDelayTicks, 0, 200),
				glowing,
				showHp == null ? ShowHpMode.WHEN_HURT : showHp,
				MathHelper.clamp(hurtCooldownTicks, 0, 40)
			);
		}
	}

	public static final BotSettings DEFAULT = new BotSettings(
		10,
		ArmorType.NETHERITE,
		1.35F,
		false,
		true,
		PracticeBotMod.SKIN_USERNAME,
		true,
		Extra.DEFAULT
	);

	public static final Codec<BotSettings> CODEC = RecordCodecBuilder.create(instance -> instance.group(
		Codec.INT.optionalFieldOf("shield_disable_ticks", DEFAULT.shieldDisableTicks).forGetter(BotSettings::shieldDisableTicks),
		Codec.STRING.optionalFieldOf("armor_type", DEFAULT.armorType.getId()).xmap(
			id -> {
				ArmorType type = ArmorType.fromId(id);
				return type == null ? DEFAULT.armorType : type;
			},
			ArmorType::getId
		).forGetter(BotSettings::armorType),
		Codec.FLOAT.optionalFieldOf("wind_boost", DEFAULT.windBoost).forGetter(BotSettings::windBoost),
		Codec.BOOL.optionalFieldOf("invulnerable", DEFAULT.invulnerable).forGetter(BotSettings::invulnerable),
		Codec.BOOL.optionalFieldOf("infinite_totems", DEFAULT.infiniteTotems).forGetter(BotSettings::infiniteTotems),
		Codec.STRING.optionalFieldOf("skin_name", DEFAULT.skinName).forGetter(BotSettings::skinName),
		Codec.BOOL.optionalFieldOf("no_knockback", DEFAULT.noKnockback).forGetter(BotSettings::noKnockback),
		Extra.CODEC.optionalFieldOf("extra", Extra.DEFAULT).forGetter(BotSettings::extra)
	).apply(instance, BotSettings::new));

	public static final PacketCodec<RegistryByteBuf, BotSettings> PACKET_CODEC = PacketCodec.of(BotSettings::write, BotSettings::read);

	private static void write(BotSettings settings, RegistryByteBuf buf) {
		buf.writeVarInt(settings.shieldDisableTicks);
		buf.writeString(settings.armorType.getId());
		buf.writeFloat(settings.windBoost);
		buf.writeBoolean(settings.invulnerable);
		buf.writeBoolean(settings.infiniteTotems);
		buf.writeString(settings.skinName);
		buf.writeBoolean(settings.noKnockback);
		Extra e = settings.extra;
		buf.writeString(e.displayName);
		buf.writeBoolean(e.lookAtPlayer);
		buf.writeFloat(e.moveRadius);
		buf.writeFloat(e.moveSpeed);
		buf.writeFloat(e.gapHealthThreshold);
		buf.writeBoolean(e.regen);
		buf.writeVarInt(e.regenDelayTicks);
		buf.writeBoolean(e.glowing);
		buf.writeString(e.showHp.getId());
		buf.writeVarInt(e.hurtCooldownTicks);
	}

	private static BotSettings read(RegistryByteBuf buf) {
		int ticks = MathHelper.clamp(buf.readVarInt(), 0, 100);
		ArmorType armor = ArmorType.fromId(buf.readString());
		float wind = MathHelper.clamp(buf.readFloat(), 0.1F, 5.0F);
		boolean invuln = buf.readBoolean();
		boolean totems = buf.readBoolean();
		String skin = buf.readString();
		boolean noKb = buf.readBoolean();
		Extra extra = new Extra(
			buf.readString(),
			buf.readBoolean(),
			buf.readFloat(),
			buf.readFloat(),
			buf.readFloat(),
			buf.readBoolean(),
			buf.readVarInt(),
			buf.readBoolean(),
			ShowHpMode.fromId(buf.readString()),
			buf.readVarInt()
		);
		return new BotSettings(
			ticks,
			armor == null ? ArmorType.NETHERITE : armor,
			wind,
			invuln,
			totems,
			skin.isBlank() ? PracticeBotMod.SKIN_USERNAME : skin.trim(),
			noKb,
			extra
		).clamped();
	}

	public String displayName() { return extra.displayName(); }
	public boolean lookAtPlayer() { return extra.lookAtPlayer(); }
	public float moveRadius() { return extra.moveRadius(); }
	public float moveSpeed() { return extra.moveSpeed(); }
	public float gapHealthThreshold() { return extra.gapHealthThreshold(); }
	public boolean regen() { return extra.regen(); }
	public int regenDelayTicks() { return extra.regenDelayTicks(); }
	public boolean glowing() { return extra.glowing(); }
	public ShowHpMode showHp() { return extra.showHp(); }
	public int hurtCooldownTicks() { return extra.hurtCooldownTicks(); }

	public BotSettings clamped() {
		return new BotSettings(
			MathHelper.clamp(shieldDisableTicks, 0, 100),
			armorType == null ? ArmorType.NETHERITE : armorType,
			MathHelper.clamp(windBoost, 0.1F, 5.0F),
			invulnerable,
			infiniteTotems,
			skinName == null || skinName.isBlank() ? PracticeBotMod.SKIN_USERNAME : skinName.trim(),
			noKnockback,
			extra == null ? Extra.DEFAULT : extra.clamped()
		);
	}

	private BotSettings withExtra(Extra next) {
		return new BotSettings(shieldDisableTicks, armorType, windBoost, invulnerable, infiniteTotems, skinName, noKnockback, next);
	}

	public BotSettings withShieldDisableTicks(int ticks) {
		return new BotSettings(MathHelper.clamp(ticks, 0, 100), armorType, windBoost, invulnerable, infiniteTotems, skinName, noKnockback, extra);
	}

	public BotSettings withArmorType(ArmorType armorType) {
		return new BotSettings(shieldDisableTicks, armorType, windBoost, invulnerable, infiniteTotems, skinName, noKnockback, extra);
	}

	public BotSettings withWindBoost(float windBoost) {
		return new BotSettings(shieldDisableTicks, armorType, MathHelper.clamp(windBoost, 0.1F, 5.0F), invulnerable, infiniteTotems, skinName, noKnockback, extra);
	}

	public BotSettings withInvulnerable(boolean invulnerable) {
		return new BotSettings(shieldDisableTicks, armorType, windBoost, invulnerable, infiniteTotems, skinName, noKnockback, extra);
	}

	public BotSettings withInfiniteTotems(boolean infiniteTotems) {
		return new BotSettings(shieldDisableTicks, armorType, windBoost, invulnerable, infiniteTotems, skinName, noKnockback, extra);
	}

	public BotSettings withSkinName(String skinName) {
		String trimmed = skinName == null || skinName.isBlank() ? PracticeBotMod.SKIN_USERNAME : skinName.trim();
		return new BotSettings(shieldDisableTicks, armorType, windBoost, invulnerable, infiniteTotems, trimmed, noKnockback, extra);
	}

	public BotSettings withNoKnockback(boolean noKnockback) {
		return new BotSettings(shieldDisableTicks, armorType, windBoost, invulnerable, infiniteTotems, skinName, noKnockback, extra);
	}

	public BotSettings withDisplayName(String displayName) {
		return withExtra(new Extra(displayName == null ? "" : displayName.trim(), extra.lookAtPlayer, extra.moveRadius, extra.moveSpeed, extra.gapHealthThreshold, extra.regen, extra.regenDelayTicks, extra.glowing, extra.showHp, extra.hurtCooldownTicks));
	}

	public BotSettings withLookAtPlayer(boolean lookAtPlayer) {
		return withExtra(new Extra(extra.displayName, lookAtPlayer, extra.moveRadius, extra.moveSpeed, extra.gapHealthThreshold, extra.regen, extra.regenDelayTicks, extra.glowing, extra.showHp, extra.hurtCooldownTicks));
	}

	public BotSettings withMoveRadius(float moveRadius) {
		return withExtra(new Extra(extra.displayName, extra.lookAtPlayer, MathHelper.clamp(moveRadius, 0.5F, 16.0F), extra.moveSpeed, extra.gapHealthThreshold, extra.regen, extra.regenDelayTicks, extra.glowing, extra.showHp, extra.hurtCooldownTicks));
	}

	public BotSettings withMoveSpeed(float moveSpeed) {
		return withExtra(new Extra(extra.displayName, extra.lookAtPlayer, extra.moveRadius, MathHelper.clamp(moveSpeed, 0.01F, 1.0F), extra.gapHealthThreshold, extra.regen, extra.regenDelayTicks, extra.glowing, extra.showHp, extra.hurtCooldownTicks));
	}

	public BotSettings withGapHealthThreshold(float gapHealthThreshold) {
		return withExtra(new Extra(extra.displayName, extra.lookAtPlayer, extra.moveRadius, extra.moveSpeed, MathHelper.clamp(gapHealthThreshold, 1.0F, 40.0F), extra.regen, extra.regenDelayTicks, extra.glowing, extra.showHp, extra.hurtCooldownTicks));
	}

	public BotSettings withRegen(boolean regen) {
		return withExtra(new Extra(extra.displayName, extra.lookAtPlayer, extra.moveRadius, extra.moveSpeed, extra.gapHealthThreshold, regen, extra.regenDelayTicks, extra.glowing, extra.showHp, extra.hurtCooldownTicks));
	}

	public BotSettings withRegenDelayTicks(int regenDelayTicks) {
		return withExtra(new Extra(extra.displayName, extra.lookAtPlayer, extra.moveRadius, extra.moveSpeed, extra.gapHealthThreshold, extra.regen, MathHelper.clamp(regenDelayTicks, 0, 200), extra.glowing, extra.showHp, extra.hurtCooldownTicks));
	}

	public BotSettings withGlowing(boolean glowing) {
		return withExtra(new Extra(extra.displayName, extra.lookAtPlayer, extra.moveRadius, extra.moveSpeed, extra.gapHealthThreshold, extra.regen, extra.regenDelayTicks, glowing, extra.showHp, extra.hurtCooldownTicks));
	}

	public BotSettings withShowHp(ShowHpMode showHp) {
		return withExtra(new Extra(extra.displayName, extra.lookAtPlayer, extra.moveRadius, extra.moveSpeed, extra.gapHealthThreshold, extra.regen, extra.regenDelayTicks, extra.glowing, showHp, extra.hurtCooldownTicks));
	}

	public BotSettings withHurtCooldownTicks(int hurtCooldownTicks) {
		return withExtra(new Extra(extra.displayName, extra.lookAtPlayer, extra.moveRadius, extra.moveSpeed, extra.gapHealthThreshold, extra.regen, extra.regenDelayTicks, extra.glowing, extra.showHp, MathHelper.clamp(hurtCooldownTicks, 0, 40)));
	}
}
