package com.practicebot.client.gui;

import com.practicebot.bot.BotSettings;
import com.practicebot.client.ClientSpawnPreset;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.text.Text;

public class PracticeBotOptionsScreen extends Screen {
	private static final int BUTTON_WIDTH = 240;
	private static final int BUTTON_HEIGHT = 18;
	private static final int GAP = 2;

	private final Screen parent;
	private BotSettings settings;
	private TextFieldWidget skinField;
	private TextFieldWidget nameField;

	public PracticeBotOptionsScreen(Screen parent) {
		super(Text.translatable("gui.practicebot.options.title"));
		this.parent = parent;
		this.settings = ClientSpawnPreset.get();
	}

	@Override
	protected void init() {
		int centerX = this.width / 2;
		int y = 28;

		this.addDrawableChild(ButtonWidget.builder(armorLabel(), b -> {
			this.settings = this.settings.withArmorType(this.settings.armorType().next());
			this.clearAndInit();
		}).dimensions(centerX - BUTTON_WIDTH / 2, y, BUTTON_WIDTH, BUTTON_HEIGHT).build());
		y += BUTTON_HEIGHT + GAP;

		y = addStepper(centerX, y,
			() -> Text.translatable("gui.practicebot.options.shield_delay", this.settings.shieldDisableTicks(), String.format("%.2f", this.settings.shieldDisableTicks() / 20.0F)),
			() -> this.settings = this.settings.withShieldDisableTicks(this.settings.shieldDisableTicks() - 1),
			() -> this.settings = this.settings.withShieldDisableTicks(this.settings.shieldDisableTicks() + 1));

		y = addStepper(centerX, y,
			() -> Text.translatable("gui.practicebot.options.wind_boost", String.format("%.2f", this.settings.windBoost())),
			() -> this.settings = this.settings.withWindBoost(this.settings.windBoost() - 0.05F),
			() -> this.settings = this.settings.withWindBoost(this.settings.windBoost() + 0.05F));

		y = addStepper(centerX, y,
			() -> Text.translatable("gui.practicebot.options.move_radius", String.format("%.1f", this.settings.moveRadius())),
			() -> this.settings = this.settings.withMoveRadius(this.settings.moveRadius() - 0.5F),
			() -> this.settings = this.settings.withMoveRadius(this.settings.moveRadius() + 0.5F));

		y = addStepper(centerX, y,
			() -> Text.translatable("gui.practicebot.options.move_speed", String.format("%.2f", this.settings.moveSpeed())),
			() -> this.settings = this.settings.withMoveSpeed(this.settings.moveSpeed() - 0.02F),
			() -> this.settings = this.settings.withMoveSpeed(this.settings.moveSpeed() + 0.02F));

		y = addStepper(centerX, y,
			() -> Text.translatable("gui.practicebot.options.gap_hp", String.format("%.0f", this.settings.gapHealthThreshold())),
			() -> this.settings = this.settings.withGapHealthThreshold(this.settings.gapHealthThreshold() - 1.0F),
			() -> this.settings = this.settings.withGapHealthThreshold(this.settings.gapHealthThreshold() + 1.0F));

		y = addStepper(centerX, y,
			() -> Text.translatable("gui.practicebot.options.regen_delay", this.settings.regenDelayTicks()),
			() -> this.settings = this.settings.withRegenDelayTicks(this.settings.regenDelayTicks() - 5),
			() -> this.settings = this.settings.withRegenDelayTicks(this.settings.regenDelayTicks() + 5));

		y = addStepper(centerX, y,
			() -> Text.translatable("gui.practicebot.options.hurt_cooldown", this.settings.hurtCooldownTicks()),
			() -> this.settings = this.settings.withHurtCooldownTicks(this.settings.hurtCooldownTicks() - 1),
			() -> this.settings = this.settings.withHurtCooldownTicks(this.settings.hurtCooldownTicks() + 1));

		this.addDrawableChild(toggle(centerX, y, "gui.practicebot.options.invulnerable", this.settings.invulnerable(),
			() -> this.settings = this.settings.withInvulnerable(!this.settings.invulnerable())));
		y += BUTTON_HEIGHT + GAP;
		this.addDrawableChild(toggle(centerX, y, "gui.practicebot.options.infinite_totems", this.settings.infiniteTotems(),
			() -> this.settings = this.settings.withInfiniteTotems(!this.settings.infiniteTotems())));
		y += BUTTON_HEIGHT + GAP;
		this.addDrawableChild(toggle(centerX, y, "gui.practicebot.options.no_knockback", this.settings.noKnockback(),
			() -> this.settings = this.settings.withNoKnockback(!this.settings.noKnockback())));
		y += BUTTON_HEIGHT + GAP;
		this.addDrawableChild(toggle(centerX, y, "gui.practicebot.options.look_at", this.settings.lookAtPlayer(),
			() -> this.settings = this.settings.withLookAtPlayer(!this.settings.lookAtPlayer())));
		y += BUTTON_HEIGHT + GAP;
		this.addDrawableChild(toggle(centerX, y, "gui.practicebot.options.regen", this.settings.regen(),
			() -> this.settings = this.settings.withRegen(!this.settings.regen())));
		y += BUTTON_HEIGHT + GAP;
		this.addDrawableChild(toggle(centerX, y, "gui.practicebot.options.glowing", this.settings.glowing(),
			() -> this.settings = this.settings.withGlowing(!this.settings.glowing())));
		y += BUTTON_HEIGHT + GAP;

		this.addDrawableChild(ButtonWidget.builder(showHpLabel(), b -> {
			this.settings = this.settings.withShowHp(this.settings.showHp().next());
			this.clearAndInit();
		}).dimensions(centerX - BUTTON_WIDTH / 2, y, BUTTON_WIDTH, BUTTON_HEIGHT).build());
		y += BUTTON_HEIGHT + GAP;

		this.skinField = new TextFieldWidget(this.textRenderer, centerX - BUTTON_WIDTH / 2, y, BUTTON_WIDTH, BUTTON_HEIGHT, Text.translatable("gui.practicebot.options.skin"));
		this.skinField.setMaxLength(16);
		this.skinField.setText(this.settings.skinName());
		this.skinField.setChangedListener(text -> this.settings = this.settings.withSkinName(text));
		this.addDrawableChild(this.skinField);
		y += BUTTON_HEIGHT + GAP;

		this.nameField = new TextFieldWidget(this.textRenderer, centerX - BUTTON_WIDTH / 2, y, BUTTON_WIDTH, BUTTON_HEIGHT, Text.translatable("gui.practicebot.options.display_name"));
		this.nameField.setMaxLength(32);
		this.nameField.setText(this.settings.displayName());
		this.nameField.setPlaceholder(Text.translatable("gui.practicebot.options.display_name"));
		this.nameField.setChangedListener(text -> this.settings = this.settings.withDisplayName(text));
		this.addDrawableChild(this.nameField);
		y += BUTTON_HEIGHT + GAP + 4;

		this.addDrawableChild(ButtonWidget.builder(Text.translatable("gui.practicebot.options.reset"), b -> {
			this.settings = BotSettings.DEFAULT;
			this.clearAndInit();
		}).dimensions(centerX - BUTTON_WIDTH / 2, y, BUTTON_WIDTH, BUTTON_HEIGHT).build());
		y += BUTTON_HEIGHT + GAP;

		this.addDrawableChild(ButtonWidget.builder(Text.translatable("gui.practicebot.options.done"), b -> this.close())
			.dimensions(centerX - BUTTON_WIDTH / 2, y, BUTTON_WIDTH, BUTTON_HEIGHT).build());
	}

	private int addStepper(int centerX, int y, java.util.function.Supplier<Text> label, Runnable dec, Runnable inc) {
		this.addDrawableChild(ButtonWidget.builder(Text.literal("-"), b -> {
			dec.run();
			this.clearAndInit();
		}).dimensions(centerX - BUTTON_WIDTH / 2, y, 36, BUTTON_HEIGHT).build());
		this.addDrawableChild(ButtonWidget.builder(label.get(), b -> {
		}).dimensions(centerX - BUTTON_WIDTH / 2 + 40, y, BUTTON_WIDTH - 80, BUTTON_HEIGHT).build());
		this.addDrawableChild(ButtonWidget.builder(Text.literal("+"), b -> {
			inc.run();
			this.clearAndInit();
		}).dimensions(centerX + BUTTON_WIDTH / 2 - 36, y, 36, BUTTON_HEIGHT).build());
		return y + BUTTON_HEIGHT + GAP;
	}

	private ButtonWidget toggle(int centerX, int y, String key, boolean value, Runnable action) {
		return ButtonWidget.builder(Text.translatable(key, boolText(value)), b -> {
			action.run();
			this.clearAndInit();
		}).dimensions(centerX - BUTTON_WIDTH / 2, y, BUTTON_WIDTH, BUTTON_HEIGHT).build();
	}

	private Text armorLabel() {
		return Text.translatable("gui.practicebot.options.armor", this.settings.armorType().getId());
	}

	private Text showHpLabel() {
		return Text.translatable("gui.practicebot.options.show_hp", this.settings.showHp().getId());
	}

	private static Text boolText(boolean value) {
		return Text.translatable(value ? "gui.practicebot.options.on" : "gui.practicebot.options.off");
	}

	@Override
	public void close() {
		String skin = this.skinField != null ? this.skinField.getText() : this.settings.skinName();
		String name = this.nameField != null ? this.nameField.getText() : this.settings.displayName();
		ClientSpawnPreset.set(this.settings.withSkinName(skin).withDisplayName(name));
		if (this.client != null) {
			this.client.setScreen(this.parent);
		}
	}

	@Override
	public void render(DrawContext context, int mouseX, int mouseY, float delta) {
		super.render(context, mouseX, mouseY, delta);
		context.drawCenteredTextWithShadow(this.textRenderer, this.title, this.width / 2, 8, 0xFFFFFF);
		context.drawCenteredTextWithShadow(this.textRenderer, Text.translatable("gui.practicebot.options.hint"), this.width / 2, 18, 0xA0A0A0);
	}

	@Override
	public boolean shouldPause() {
		return false;
	}
}
