package com.practicebot.client.gui;

import com.practicebot.bot.BotMode;
import com.practicebot.bot.BotSettings;
import com.practicebot.client.ClientSpawnPreset;
import com.practicebot.network.SpawnPracticeBotPayload;
import com.practicebot.network.UpdatePracticeBotPayload;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.text.Text;

public class PracticeBotScreen extends Screen {
	private static final int BUTTON_WIDTH = 180;
	private static final int BUTTON_HEIGHT = 20;
	private static final int GAP = 3;

	public PracticeBotScreen() {
		super(Text.translatable("gui.practicebot.title"));
	}

	@Override
	protected void init() {
		int centerX = this.width / 2;
		int y = this.height / 2 - 130;

		this.addDrawableChild(modeButton(centerX, y, "gui.practicebot.shield", BotMode.SHIELD));
		y += BUTTON_HEIGHT + GAP;
		this.addDrawableChild(modeButton(centerX, y, "gui.practicebot.still", BotMode.STILL));
		y += BUTTON_HEIGHT + GAP;
		this.addDrawableChild(modeButton(centerX, y, "gui.practicebot.wind", BotMode.WIND));
		y += BUTTON_HEIGHT + GAP;
		this.addDrawableChild(modeButton(centerX, y, "gui.practicebot.elytra", BotMode.ELYTRA));
		y += BUTTON_HEIGHT + GAP;
		this.addDrawableChild(modeButton(centerX, y, "gui.practicebot.elytra_shield", BotMode.ELYTRA_SHIELD));
		y += BUTTON_HEIGHT + GAP;
		this.addDrawableChild(modeButton(centerX, y, "gui.practicebot.move", BotMode.MOVE));
		y += BUTTON_HEIGHT + GAP;
		this.addDrawableChild(modeButton(centerX, y, "gui.practicebot.gap", BotMode.GAP));
		y += BUTTON_HEIGHT + GAP;
		this.addDrawableChild(modeButton(centerX, y, "gui.practicebot.crystal", BotMode.CRYSTAL));
		y += BUTTON_HEIGHT + GAP + 4;

		this.addDrawableChild(ButtonWidget.builder(Text.translatable("gui.practicebot.options"), button -> {
			if (this.client != null) {
				this.client.setScreen(new PracticeBotOptionsScreen(this));
			}
		}).dimensions(centerX - BUTTON_WIDTH / 2, y, BUTTON_WIDTH, BUTTON_HEIGHT).build());
		y += BUTTON_HEIGHT + GAP;

		this.addDrawableChild(ButtonWidget.builder(Text.translatable("gui.practicebot.apply_nearest"), button -> {
			ClientPlayNetworking.send(new UpdatePracticeBotPayload(ClientSpawnPreset.get()));
		}).dimensions(centerX - BUTTON_WIDTH / 2, y, BUTTON_WIDTH, BUTTON_HEIGHT).build());
		y += BUTTON_HEIGHT + GAP;

		this.addDrawableChild(ButtonWidget.builder(Text.translatable("gui.practicebot.remove"), button -> runCommand("pbot remove"))
			.dimensions(centerX - BUTTON_WIDTH / 2, y, BUTTON_WIDTH, BUTTON_HEIGHT).build());
		y += BUTTON_HEIGHT + GAP;

		this.addDrawableChild(ButtonWidget.builder(Text.translatable("gui.practicebot.remove_all"), button -> runCommand("pbot remove all"))
			.dimensions(centerX - BUTTON_WIDTH / 2, y, BUTTON_WIDTH, BUTTON_HEIGHT).build());
		y += BUTTON_HEIGHT + GAP;

		this.addDrawableChild(ButtonWidget.builder(Text.translatable("gui.practicebot.close"), button -> this.close())
			.dimensions(centerX - BUTTON_WIDTH / 2, y, BUTTON_WIDTH, BUTTON_HEIGHT).build());
	}

	private ButtonWidget modeButton(int centerX, int y, String translationKey, BotMode mode) {
		return ButtonWidget.builder(Text.translatable(translationKey), button -> spawn(mode))
			.dimensions(centerX - BUTTON_WIDTH / 2, y, BUTTON_WIDTH, BUTTON_HEIGHT)
			.build();
	}

	private void spawn(BotMode mode) {
		ClientPlayNetworking.send(new SpawnPracticeBotPayload(mode, ClientSpawnPreset.get()));
	}

	private void runCommand(String command) {
		if (this.client != null && this.client.player != null) {
			this.client.player.networkHandler.sendChatCommand(command);
		}
	}

	@Override
	public void render(DrawContext context, int mouseX, int mouseY, float delta) {
		super.render(context, mouseX, mouseY, delta);
		context.drawCenteredTextWithShadow(this.textRenderer, this.title, this.width / 2, this.height / 2 - 148, 0xFFFFFF);
		BotSettings settings = ClientSpawnPreset.get();
		context.drawCenteredTextWithShadow(
			this.textRenderer,
			Text.translatable(
				"gui.practicebot.preset_summary",
				settings.armorType().getId(),
				settings.shieldDisableTicks(),
				settings.skinName()
			),
			this.width / 2,
			this.height / 2 - 138,
			0xA0A0A0
		);
	}

	@Override
	public boolean shouldPause() {
		return false;
	}
}
