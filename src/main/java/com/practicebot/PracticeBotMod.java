package com.practicebot;

import com.practicebot.bot.BotMode;
import com.practicebot.bot.BotSettings;
import com.practicebot.bot.PracticeBotAttachments;
import com.practicebot.bot.PracticeBotHandler;
import com.practicebot.command.PBotCommands;
import com.practicebot.network.DamageHologramPayload;
import com.practicebot.network.SpawnPracticeBotPayload;
import com.practicebot.network.UpdatePracticeBotPayload;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.networking.v1.EntityTrackingEvents;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.entity.decoration.MannequinEntity;
import net.minecraft.text.Text;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class PracticeBotMod implements ModInitializer {
	public static final String MOD_ID = "practicebot";
	public static final String SKIN_USERNAME = "_potatowater";
	public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

	@Override
	public void onInitialize() {
		PracticeBotAttachments.bootstrap();
		PayloadTypeRegistry.playC2S().register(SpawnPracticeBotPayload.ID, SpawnPracticeBotPayload.CODEC);
		PayloadTypeRegistry.playC2S().register(UpdatePracticeBotPayload.ID, UpdatePracticeBotPayload.CODEC);
		PayloadTypeRegistry.playS2C().register(DamageHologramPayload.ID, DamageHologramPayload.CODEC);

		ServerPlayNetworking.registerGlobalReceiver(SpawnPracticeBotPayload.ID, (payload, context) -> {
			context.server().execute(() -> {
				BotMode mode = BotMode.fromId(payload.modeId());
				if (mode == null) {
					context.player().sendMessage(Text.translatable("commands.practicebot.bad_mode"), false);
					return;
				}
				BotSettings settings = payload.settings().clamped();
				PracticeBotHandler.spawn(context.player(), mode, settings);
				context.player().sendMessage(Text.translatable("commands.practicebot.spawned", mode.getId()), false);
			});
		});

		ServerPlayNetworking.registerGlobalReceiver(UpdatePracticeBotPayload.ID, (payload, context) -> {
			context.server().execute(() -> {
				if (PracticeBotHandler.updateNearest(context.player(), payload.settings().clamped())) {
					context.player().sendMessage(Text.translatable("commands.practicebot.updated"), false);
				} else {
					context.player().sendMessage(Text.translatable("commands.practicebot.none"), false);
				}
			});
		});

		CommandRegistrationCallback.EVENT.register(PBotCommands::register);

		EntityTrackingEvents.START_TRACKING.register((entity, player) -> {
			if (entity instanceof MannequinEntity mannequin) {
				PracticeBotHandler.refreshShieldForTracker(mannequin);
			}
		});

		LOGGER.info("Practice Bot loaded (/pbot + GUI keybind).");
	}
}
