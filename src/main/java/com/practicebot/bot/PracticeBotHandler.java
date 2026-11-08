package com.practicebot.bot;

import com.practicebot.mixin.MannequinEntityAccessor;
import com.practicebot.mixin.PlayerLikeEntityAccessor;
import com.practicebot.network.DamageHologramPayload;
import net.fabricmc.fabric.api.networking.v1.PlayerLookup;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.ProfileComponent;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityPose;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.decoration.MannequinEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerModelPart;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Hand;
import net.minecraft.util.Unit;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.Set;

public final class PracticeBotHandler {
	public static final String ROOT_TAG = "practicebot";
	private static final String MODE_TAG_PREFIX = "pbot_";
	private static final float ELYTRA_GLIDE_PITCH = 28.0F;
	private static final double LOOK_RANGE = 32.0;
	private static final double REMOVE_RANGE = 64.0;

	private PracticeBotHandler() {
	}

	public static boolean isPracticeBot(Entity entity) {
		if (!(entity instanceof MannequinEntity mannequin)) {
			return false;
		}
		if (getMode(mannequin) != null) {
			return true;
		}
		return hasPracticeSkin(mannequin);
	}

	@Nullable
	public static BotMode getMode(MannequinEntity mannequin) {
		PracticeBotAttachments.Data data = mannequin.getAttached(PracticeBotAttachments.DATA);
		if (data != null) {
			BotMode mode = BotMode.fromId(data.mode());
			if (mode != null) {
				ensureTags(mannequin, mode);
				return mode;
			}
		}

		Set<String> tags = mannequin.getCommandTags();
		if (tags.contains(ROOT_TAG)) {
			for (BotMode mode : BotMode.orderedForTags()) {
				if (tags.contains(modeTag(mode))) {
					BotSettings settings = data != null ? data.settings() : BotSettings.DEFAULT;
					int shield = data != null ? data.shieldDisabledTicks() : 0;
					setBotData(mannequin, mode, settings, shield);
					return mode;
				}
			}
		}

		return null;
	}

	public static BotSettings getSettings(MannequinEntity mannequin) {
		PracticeBotAttachments.Data data = mannequin.getAttached(PracticeBotAttachments.DATA);
		return data == null ? BotSettings.DEFAULT : data.settings();
	}

	public static PracticeBotAttachments.@Nullable Data getData(MannequinEntity mannequin) {
		return mannequin.getAttached(PracticeBotAttachments.DATA);
	}

	public static void setBotData(MannequinEntity mannequin, BotMode mode, BotSettings settings, int shieldDisabledTicks) {
		ensureTags(mannequin, mode);
		PracticeBotAttachments.Data existing = mannequin.getAttached(PracticeBotAttachments.DATA);
		PracticeBotAttachments.Data next = PracticeBotAttachments.Data.create(mode.getId(), settings, shieldDisabledTicks);
		if (existing != null) {
			next = new PracticeBotAttachments.Data(
				mode.getId(),
				Math.max(0, shieldDisabledTicks),
				settings.clamped(),
				existing.totemPops(),
				existing.lastDamage(),
				existing.homeX(),
				existing.homeY(),
				existing.homeZ(),
				existing.hasHome(),
				existing.ticksSinceDamage(),
				existing.hurtCooldownRemaining()
			);
		}
		mannequin.setAttached(PracticeBotAttachments.DATA, next);
	}

	public static void applySettingsToBot(MannequinEntity mannequin, BotSettings settings) {
		BotMode mode = getMode(mannequin);
		if (mode == null) {
			return;
		}
		setBotData(mannequin, mode, settings, getShieldDisabledTicks(mannequin));
		applyEquipment(mannequin, mode, settings);
		applyKnockbackResistance(mannequin, effectiveNoKnockback(mode, settings));
		updateNametag(mannequin, mode, settings);
	}

	public static void disableShield(MannequinEntity mannequin) {
		BotMode mode = getMode(mannequin);
		if (mode == null || !mode.usesShield()) {
			return;
		}
		BotSettings settings = getSettings(mannequin);
		setBotData(mannequin, mode, settings, settings.shieldDisableTicks());
		mannequin.clearActiveItem();
	}

	public static int getShieldDisabledTicks(MannequinEntity mannequin) {
		PracticeBotAttachments.Data data = mannequin.getAttached(PracticeBotAttachments.DATA);
		return data == null ? 0 : data.shieldDisabledTicks();
	}

	private static void setShieldDisabledTicks(MannequinEntity mannequin, int ticks) {
		BotMode mode = getMode(mannequin);
		if (mode == null) {
			return;
		}
		setBotData(mannequin, mode, getSettings(mannequin), ticks);
	}

	public static MannequinEntity spawn(ServerPlayerEntity player, BotMode mode) {
		return spawn(player, mode, BotSettings.DEFAULT);
	}

	public static MannequinEntity spawn(ServerPlayerEntity player, BotMode mode, BotSettings settings) {
		ServerWorld world = player.getEntityWorld();
		MannequinEntity bot = new MannequinEntity(net.minecraft.entity.EntityType.MANNEQUIN, world);
		BotSettings effective = settings.clamped();
		if (mode.forcesNoKnockback()) {
			effective = effective.withNoKnockback(true);
		}
		if (mode.forcesGlowing()) {
			effective = effective.withGlowing(true);
		}

		bot.refreshPositionAndAngles(player.getX(), player.getY(), player.getZ(), player.getYaw(), player.getPitch());
		bot.setYaw(player.getYaw());
		bot.setBodyYaw(player.getYaw());
		bot.setHeadYaw(player.getYaw());
		bot.setPitch(player.getPitch());

		MannequinEntityAccessor accessor = (MannequinEntityAccessor) (Object) bot;
		accessor.invokeSetMannequinProfile(ProfileComponent.ofDynamic(effective.skinName()));
		accessor.invokeSetImmovable(false);
		accessor.invokeSetHideDescription(true);
		hideCape(bot);

		bot.setCustomNameVisible(false);
		bot.setSilent(false);
		bot.setNoGravity(mode.isFrozen());
		bot.setInvulnerable(effective.invulnerable());
		bot.setHealth(bot.getMaxHealth());
		bot.setGlowing(effective.glowing() || mode.forcesGlowing());
		if (mode == BotMode.WIND) {
			bot.fallDistance = 0.0;
		}

		setBotData(bot, mode, effective, 0);
		PracticeBotAttachments.Data data = bot.getAttached(PracticeBotAttachments.DATA);
		if (data != null) {
			bot.setAttached(PracticeBotAttachments.DATA, data.withHome(bot.getX(), bot.getY(), bot.getZ()));
		}

		applyEquipment(bot, mode, effective);
		applyKnockbackResistance(bot, effectiveNoKnockback(mode, effective));
		if (mode.usesElytra()) {
			bot.setPose(EntityPose.GLIDING);
			bot.setPitch(ELYTRA_GLIDE_PITCH);
		}
		ensureBlocking(bot, mode);
		updateNametag(bot, mode, effective);

		world.spawnEntity(bot);
		return bot;
	}

	public static boolean removeNearest(ServerPlayerEntity player) {
		Optional<MannequinEntity> nearest = findNearest(player, REMOVE_RANGE);
		if (nearest.isEmpty()) {
			return false;
		}
		nearest.get().discard();
		return true;
	}

	public static int removeAll(ServerPlayerEntity player) {
		ServerWorld world = player.getEntityWorld();
		List<MannequinEntity> bots = world.getEntitiesByClass(MannequinEntity.class, player.getBoundingBox().expand(256.0), PracticeBotHandler::isPracticeBot);
		for (MannequinEntity bot : bots) {
			bot.discard();
		}
		return bots.size();
	}

	public static int removeByMode(ServerPlayerEntity player, BotMode mode) {
		ServerWorld world = player.getEntityWorld();
		List<MannequinEntity> bots = world.getEntitiesByClass(
			MannequinEntity.class,
			player.getBoundingBox().expand(256.0),
			bot -> getMode(bot) == mode
		);
		for (MannequinEntity bot : bots) {
			bot.discard();
		}
		return bots.size();
	}

	public static Optional<MannequinEntity> findNearest(ServerPlayerEntity player, double range) {
		Vec3d pos = player.getEntityPos();
		return player.getEntityWorld().getEntitiesByClass(MannequinEntity.class, player.getBoundingBox().expand(range), PracticeBotHandler::isPracticeBot)
			.stream()
			.min(Comparator.comparingDouble(bot -> bot.squaredDistanceTo(pos)));
	}

	public static boolean updateNearest(ServerPlayerEntity player, BotSettings settings) {
		Optional<MannequinEntity> nearest = findNearest(player, REMOVE_RANGE);
		if (nearest.isEmpty() || getMode(nearest.get()) == null) {
			return false;
		}
		applySettingsToBot(nearest.get(), settings);
		return true;
	}

	public static int resetPopsNearest(ServerPlayerEntity player) {
		Optional<MannequinEntity> nearest = findNearest(player, REMOVE_RANGE);
		if (nearest.isEmpty()) {
			return 0;
		}
		PracticeBotAttachments.Data data = nearest.get().getAttached(PracticeBotAttachments.DATA);
		if (data == null) {
			return 0;
		}
		nearest.get().setAttached(PracticeBotAttachments.DATA, data.resetPops());
		updateNametag(nearest.get(), getMode(nearest.get()), getSettings(nearest.get()));
		return 1;
	}

	public static int resetPopsAll(ServerPlayerEntity player) {
		int count = 0;
		for (MannequinEntity bot : findNearby(player, 256.0)) {
			PracticeBotAttachments.Data data = bot.getAttached(PracticeBotAttachments.DATA);
			if (data == null) {
				continue;
			}
			bot.setAttached(PracticeBotAttachments.DATA, data.resetPops());
			updateNametag(bot, getMode(bot), getSettings(bot));
			count++;
		}
		return count;
	}

	public static List<Text> listNearby(ServerPlayerEntity player) {
		List<Text> lines = new ArrayList<>();
		Vec3d pos = player.getEntityPos();
		List<MannequinEntity> bots = findNearby(player, REMOVE_RANGE);
		bots.sort(Comparator.comparingDouble(bot -> bot.squaredDistanceTo(pos)));
		for (MannequinEntity bot : bots) {
			BotMode mode = getMode(bot);
			PracticeBotAttachments.Data data = getData(bot);
			String modeId = mode == null ? "?" : mode.getId();
			String name = getSettings(bot).displayName();
			if (name.isBlank()) {
				name = modeId;
			}
			int pops = data == null ? 0 : data.totemPops();
			double dist = Math.sqrt(bot.squaredDistanceTo(pos));
			lines.add(Text.translatable(
				"commands.practicebot.list_entry",
				name,
				modeId,
				String.format("%.0f", bot.getHealth()),
				String.format("%.0f", bot.getMaxHealth()),
				pops,
				String.format("%.1f", dist)
			));
		}
		return lines;
	}

	public static void tickBot(MannequinEntity bot) {
		BotMode mode = getMode(bot);
		if (mode == null) {
			return;
		}

		BotSettings settings = getSettings(bot);
		ensureTags(bot, mode);
		hideCape(bot);
		bot.setInvulnerable(settings.invulnerable());
		bot.setGlowing(settings.glowing() || mode.forcesGlowing());

		MannequinEntityAccessor accessor = (MannequinEntityAccessor) (Object) bot;
		accessor.invokeSetMannequinProfile(ProfileComponent.ofDynamic(settings.skinName()));
		accessor.invokeSetImmovable(false);

		PracticeBotAttachments.Data data = bot.getAttached(PracticeBotAttachments.DATA);
		if (data != null) {
			data = data.tickTimers();
			bot.setAttached(PracticeBotAttachments.DATA, data);
		}

		int disabled = getShieldDisabledTicks(bot);
		if (disabled > 0) {
			setShieldDisabledTicks(bot, disabled - 1);
		}

		applyEquipment(bot, mode, settings);
		applyKnockbackResistance(bot, effectiveNoKnockback(mode, settings));

		if (mode.isFrozen()) {
			bot.setNoGravity(true);
			bot.setVelocity(Vec3d.ZERO);
			bot.velocityDirty = true;
		} else {
			bot.setNoGravity(false);
		}

		if (mode.usesElytra()) {
			bot.setPose(EntityPose.GLIDING);
			if (!settings.lookAtPlayer()) {
				bot.setPitch(ELYTRA_GLIDE_PITCH);
			}
		} else if (bot.getPose() == EntityPose.GLIDING) {
			bot.setPose(EntityPose.STANDING);
		}

		if (mode == BotMode.MOVE) {
			tickMoveOrbit(bot, settings);
		}

		if (mode == BotMode.GAP) {
			tickGapEat(bot, settings);
		} else {
			ensureBlocking(bot, mode);
		}

		if (settings.lookAtPlayer() && mode != BotMode.MOVE) {
			lookAtNearestPlayer(bot, mode, settings);
		}

		if (settings.regen()) {
			tickRegen(bot, settings);
		}

		updateNametag(bot, mode, settings);

		if (mode == BotMode.WIND) {
			bot.fallDistance = 0.0;
			if (bot.isOnGround() && !bot.getEntityWorld().isClient()) {
				launchWindCharge(bot, settings);
			}
		}
	}

	public static void onBotDamaged(MannequinEntity bot, DamageSource source, float amount) {
		BotMode mode = getMode(bot);
		if (mode == null || amount <= 0.0F) {
			return;
		}

		PracticeBotAttachments.Data data = bot.getAttached(PracticeBotAttachments.DATA);
		if (data == null) {
			return;
		}

		BotSettings settings = data.settings();
		int hurtCd = settings.hurtCooldownTicks();
		bot.setAttached(PracticeBotAttachments.DATA, data.withLastDamage(amount).withHurtCooldown(hurtCd));

		if (!bot.getEntityWorld().isClient() && bot.getEntityWorld() instanceof ServerWorld serverWorld) {
			Vec3d pos = bot.getEntityPos().add(0.0, bot.getHeight() + 0.2, 0.0);
			DamageHologramPayload payload = new DamageHologramPayload(pos.x, pos.y, pos.z, amount);
			for (ServerPlayerEntity player : PlayerLookup.tracking(bot)) {
				ServerPlayNetworking.send(player, payload);
			}
			if (hurtCd > 0) {
				bot.hurtTime = Math.max(bot.hurtTime, hurtCd);
				bot.maxHurtTime = Math.max(bot.maxHurtTime, hurtCd);
				bot.playSound(SoundEvents.ENTITY_PLAYER_HURT);
			}
			serverWorld.spawnParticles(ParticleTypes.DAMAGE_INDICATOR, pos.x, pos.y, pos.z, Math.max(1, MathHelper.ceil(amount)), 0.2, 0.2, 0.2, 0.0);
		}
	}

	public static void onTotemPop(MannequinEntity bot) {
		PracticeBotAttachments.Data data = bot.getAttached(PracticeBotAttachments.DATA);
		if (data == null || getMode(bot) == null) {
			return;
		}
		bot.setAttached(PracticeBotAttachments.DATA, data.withTotemPops(data.totemPops() + 1));
		updateNametag(bot, getMode(bot), getSettings(bot));
		if (!bot.getEntityWorld().isClient()) {
			for (ServerPlayerEntity player : PlayerLookup.tracking(bot)) {
				player.sendMessage(Text.translatable("commands.practicebot.totem_pop", data.totemPops() + 1), true);
			}
		}
	}

	public static boolean shouldIgnoreFallDamage(MannequinEntity bot) {
		return getMode(bot) == BotMode.WIND;
	}

	public static boolean shouldIgnoreKnockback(MannequinEntity bot) {
		BotMode mode = getMode(bot);
		if (mode == null) {
			return false;
		}
		return effectiveNoKnockback(mode, getSettings(bot));
	}

	public static void refreshShieldForTracker(MannequinEntity bot) {
		BotMode mode = getMode(bot);
		if (mode == null || !mode.usesShield() || getShieldDisabledTicks(bot) > 0) {
			return;
		}
		if (!bot.getStackInHand(Hand.MAIN_HAND).isOf(Items.SHIELD) || !bot.isUsingItem()) {
			return;
		}
		bot.clearActiveItem();
		bot.setCurrentHand(Hand.MAIN_HAND);
	}

	private static boolean effectiveNoKnockback(BotMode mode, BotSettings settings) {
		return settings.noKnockback() || mode.forcesNoKnockback();
	}

	private static void tickMoveOrbit(MannequinEntity bot, BotSettings settings) {
		PracticeBotAttachments.Data data = bot.getAttached(PracticeBotAttachments.DATA);
		if (data == null || !data.hasHome()) {
			return;
		}
		double angle = bot.age * settings.moveSpeed();
		double x = data.homeX() + Math.cos(angle) * settings.moveRadius();
		double z = data.homeZ() + Math.sin(angle) * settings.moveRadius();
		double y = data.homeY();
		bot.refreshPositionAndAngles(x, y, z, (float) (MathHelper.wrapDegrees(Math.toDegrees(angle) + 90.0)), 0.0F);
		bot.setVelocity(Vec3d.ZERO);
		bot.velocityDirty = true;
		bot.setOnGround(true);
	}

	private static void tickGapEat(MannequinEntity bot, BotSettings settings) {
		if (bot.getHealth() <= settings.gapHealthThreshold()) {
			if (!bot.getStackInHand(Hand.MAIN_HAND).isOf(Items.GOLDEN_APPLE)) {
				bot.equipStack(EquipmentSlot.MAINHAND, new ItemStack(Items.GOLDEN_APPLE, 64));
			}
			if (!bot.isUsingItem()) {
				bot.setCurrentHand(Hand.MAIN_HAND);
			}
		} else if (bot.isUsingItem() && bot.getActiveItem().isOf(Items.GOLDEN_APPLE)) {
			bot.clearActiveItem();
		}
	}

	private static void tickRegen(MannequinEntity bot, BotSettings settings) {
		PracticeBotAttachments.Data data = bot.getAttached(PracticeBotAttachments.DATA);
		if (data == null) {
			return;
		}
		if (bot.getHealth() >= bot.getMaxHealth()) {
			return;
		}
		if (data.ticksSinceDamage() < settings.regenDelayTicks()) {
			return;
		}
		bot.setHealth(bot.getMaxHealth());
	}

	private static void lookAtNearestPlayer(MannequinEntity bot, BotMode mode, BotSettings settings) {
		PlayerEntity nearest = bot.getEntityWorld().getClosestPlayer(bot, LOOK_RANGE);
		if (nearest == null) {
			if (mode.usesElytra()) {
				bot.setPitch(ELYTRA_GLIDE_PITCH);
			}
			return;
		}
		Vec3d eyes = bot.getEyePos();
		Vec3d target = nearest.getEyePos();
		Vec3d delta = target.subtract(eyes);
		double horiz = Math.sqrt(delta.x * delta.x + delta.z * delta.z);
		float yaw = (float) (MathHelper.atan2(delta.z, delta.x) * (180.0 / Math.PI)) - 90.0F;
		float pitch = mode.usesElytra() && settings.lookAtPlayer()
			? (float) (-(MathHelper.atan2(delta.y, horiz) * (180.0 / Math.PI)))
			: mode.usesElytra() ? ELYTRA_GLIDE_PITCH : (float) (-(MathHelper.atan2(delta.y, horiz) * (180.0 / Math.PI)));
		if (mode.usesElytra() && !settings.lookAtPlayer()) {
			pitch = ELYTRA_GLIDE_PITCH;
		}
		bot.setYaw(yaw);
		bot.setBodyYaw(yaw);
		bot.setHeadYaw(yaw);
		bot.setPitch(pitch);
	}

	private static void updateNametag(MannequinEntity bot, @Nullable BotMode mode, BotSettings settings) {
		if (mode == null) {
			return;
		}
		PracticeBotAttachments.Data data = bot.getAttached(PracticeBotAttachments.DATA);
		int pops = data == null ? 0 : data.totemPops();
		boolean hurt = bot.getHealth() < bot.getMaxHealth() - 0.01F;
		boolean showHp = switch (settings.showHp()) {
			case ALWAYS -> true;
			case WHEN_HURT -> hurt;
			case NEVER -> false;
		};

		List<String> parts = new ArrayList<>();
		if (!settings.displayName().isBlank()) {
			parts.add(settings.displayName());
		}
		if (showHp) {
			parts.add(String.format("❤ %.0f/%.0f", bot.getHealth(), bot.getMaxHealth()));
		}
		if (pops > 0) {
			parts.add("Pops: " + pops);
		}

		if (parts.isEmpty()) {
			bot.setCustomName(null);
			bot.setCustomNameVisible(false);
		} else {
			bot.setCustomName(Text.literal(String.join(" | ", parts)).formatted(Formatting.YELLOW));
			bot.setCustomNameVisible(true);
		}
	}

	private static void launchWindCharge(MannequinEntity bot, BotSettings settings) {
		ServerWorld world = (ServerWorld) bot.getEntityWorld();
		Vec3d pos = bot.getEntityPos();

		bot.fallDistance = 0.0;
		bot.setVelocity(0.0, settings.windBoost(), 0.0);
		bot.velocityDirty = true;
		bot.setOnGround(false);

		world.spawnParticles(ParticleTypes.GUST_EMITTER_SMALL, pos.getX(), pos.getY(), pos.getZ(), 1, 0.0, 0.0, 0.0, 0.0);
		world.spawnParticles(ParticleTypes.GUST_EMITTER_LARGE, pos.getX(), pos.getY(), pos.getZ(), 1, 0.0, 0.0, 0.0, 0.0);
		world.playSound(null, bot.getX(), bot.getY(), bot.getZ(), SoundEvents.ENTITY_WIND_CHARGE_THROW, SoundCategory.PLAYERS, 1.0F, 1.0F);
	}

	private static void ensureBlocking(MannequinEntity bot, BotMode mode) {
		if (!mode.usesShield()) {
			if (bot.isUsingItem() && !bot.getActiveItem().isOf(Items.GOLDEN_APPLE)) {
				bot.clearActiveItem();
			}
			return;
		}

		if (getShieldDisabledTicks(bot) > 0) {
			if (bot.isUsingItem()) {
				bot.clearActiveItem();
			}
			return;
		}

		if (!bot.getStackInHand(Hand.MAIN_HAND).isOf(Items.SHIELD)) {
			return;
		}

		if (!bot.isUsingItem()) {
			bot.setCurrentHand(Hand.MAIN_HAND);
		}
	}

	private static void applyEquipment(MannequinEntity bot, BotMode mode, BotSettings settings) {
		ArmorType armor = settings.armorType();
		equipOrClear(bot, EquipmentSlot.HEAD, armor.getHelmet());
		equipOrClear(bot, EquipmentSlot.LEGS, armor.getLeggings());
		equipOrClear(bot, EquipmentSlot.FEET, armor.getBoots());

		if (mode.usesElytra()) {
			bot.equipStack(EquipmentSlot.CHEST, unbreakable(Items.ELYTRA.getDefaultStack()));
		} else {
			equipOrClear(bot, EquipmentSlot.CHEST, armor.getChestplate());
		}

		switch (mode) {
			case SHIELD, ELYTRA_SHIELD -> {
				bot.equipStack(EquipmentSlot.MAINHAND, unbreakable(Items.SHIELD.getDefaultStack()));
				bot.equipStack(EquipmentSlot.OFFHAND, settings.infiniteTotems() ? totemStack() : singleTotemOrEmpty(bot, EquipmentSlot.OFFHAND));
			}
			case STILL, CRYSTAL, MOVE -> {
				bot.equipStack(EquipmentSlot.MAINHAND, settings.infiniteTotems() ? totemStack() : singleTotemOrEmpty(bot, EquipmentSlot.MAINHAND));
				bot.equipStack(EquipmentSlot.OFFHAND, settings.infiniteTotems() ? totemStack() : singleTotemOrEmpty(bot, EquipmentSlot.OFFHAND));
			}
			case GAP -> {
				bot.equipStack(EquipmentSlot.MAINHAND, new ItemStack(Items.GOLDEN_APPLE, 64));
				bot.equipStack(EquipmentSlot.OFFHAND, settings.infiniteTotems() ? totemStack() : singleTotemOrEmpty(bot, EquipmentSlot.OFFHAND));
			}
			case WIND, ELYTRA -> {
				bot.equipStack(EquipmentSlot.MAINHAND, ItemStack.EMPTY);
				bot.equipStack(EquipmentSlot.OFFHAND, settings.infiniteTotems() ? totemStack() : singleTotemOrEmpty(bot, EquipmentSlot.OFFHAND));
			}
		}
	}

	private static void equipOrClear(MannequinEntity bot, EquipmentSlot slot, @Nullable Item item) {
		if (item == null) {
			bot.equipStack(slot, ItemStack.EMPTY);
		} else {
			bot.equipStack(slot, unbreakable(item.getDefaultStack()));
		}
	}

	private static ItemStack singleTotemOrEmpty(MannequinEntity bot, EquipmentSlot slot) {
		ItemStack current = bot.getEquippedStack(slot);
		if (current.isOf(Items.TOTEM_OF_UNDYING) && !current.isEmpty()) {
			return current;
		}
		return new ItemStack(Items.TOTEM_OF_UNDYING);
	}

	private static void ensureTags(MannequinEntity mannequin, BotMode mode) {
		mannequin.addCommandTag(ROOT_TAG);
		for (BotMode value : BotMode.values()) {
			mannequin.removeCommandTag(modeTag(value));
		}
		mannequin.addCommandTag(modeTag(mode));
	}

	private static String modeTag(BotMode mode) {
		return MODE_TAG_PREFIX + mode.getId();
	}

	private static boolean hasPracticeSkin(MannequinEntity mannequin) {
		ProfileComponent profile = mannequin.get(DataComponentTypes.PROFILE);
		if (profile == null) {
			return false;
		}
		return profile.getName()
			.map(name -> name.equalsIgnoreCase(BotSettings.DEFAULT.skinName())
				|| name.equalsIgnoreCase(getSettings(mannequin).skinName()))
			.orElse(false);
	}

	private static void hideCape(MannequinEntity bot) {
		var customizationId = PlayerLikeEntityAccessor.getPlayerModeCustomizationId();
		byte parts = bot.getDataTracker().get(customizationId);
		byte withoutCape = (byte) (parts & ~PlayerModelPart.CAPE.getBitFlag());
		if (parts != withoutCape) {
			bot.getDataTracker().set(customizationId, withoutCape);
		}
	}

	private static void applyKnockbackResistance(MannequinEntity bot, boolean noKnockback) {
		double value = noKnockback ? 1.0 : 0.0;
		var knockback = bot.getAttributeInstance(EntityAttributes.KNOCKBACK_RESISTANCE);
		if (knockback != null) {
			knockback.setBaseValue(value);
		}
		var explosion = bot.getAttributeInstance(EntityAttributes.EXPLOSION_KNOCKBACK_RESISTANCE);
		if (explosion != null) {
			explosion.setBaseValue(value);
		}
	}

	private static ItemStack totemStack() {
		return new ItemStack(Items.TOTEM_OF_UNDYING, 64);
	}

	private static ItemStack unbreakable(ItemStack stack) {
		stack.set(DataComponentTypes.UNBREAKABLE, Unit.INSTANCE);
		return stack;
	}

	public static List<MannequinEntity> findNearby(PlayerEntity player, double range) {
		return player.getEntityWorld().getEntitiesByClass(
			MannequinEntity.class,
			player.getBoundingBox().expand(range),
			PracticeBotHandler::isPracticeBot
		);
	}
}
