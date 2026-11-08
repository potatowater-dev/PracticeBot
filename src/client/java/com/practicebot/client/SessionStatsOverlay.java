package com.practicebot.client;

import com.practicebot.bot.BotMode;
import com.practicebot.bot.PracticeBotAttachments;
import com.practicebot.bot.PracticeBotHandler;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.entity.decoration.MannequinEntity;
import net.minecraft.text.Text;
import net.minecraft.util.math.Vec3d;

import java.util.Comparator;
import java.util.List;

public final class SessionStatsOverlay {
	public static boolean visible;

	private SessionStatsOverlay() {
	}

	public static void toggle() {
		visible = !visible;
	}

	public static void render(DrawContext context, MinecraftClient client) {
		if (!visible || client.player == null || client.world == null || client.options.hudHidden) {
			return;
		}

		List<MannequinEntity> bots = PracticeBotHandler.findNearby(client.player, 64.0);
		Vec3d pos = client.player.getEntityPos();
		MannequinEntity nearest = bots.stream()
			.min(Comparator.comparingDouble(bot -> bot.squaredDistanceTo(pos)))
			.orElse(null);

		int x = 8;
		int y = 8;
		context.fill(x - 2, y - 2, x + 160, y + 54, 0x66000000);
		context.drawTextWithShadow(client.textRenderer, Text.translatable("gui.practicebot.stats.title"), x, y, 0xFFFFFF);
		y += 12;
		context.drawTextWithShadow(client.textRenderer, Text.translatable("gui.practicebot.stats.nearby", bots.size()), x, y, 0xCFCFCF);
		y += 12;

		if (nearest == null) {
			context.drawTextWithShadow(client.textRenderer, Text.translatable("gui.practicebot.stats.none"), x, y, 0xA0A0A0);
			return;
		}

		PracticeBotAttachments.Data data = nearest.getAttached(PracticeBotAttachments.DATA);
		BotMode mode = PracticeBotHandler.getMode(nearest);
		String modeId = mode == null ? "?" : mode.getId();
		float last = data == null ? 0.0F : data.lastDamage();
		int pops = data == null ? 0 : data.totemPops();

		context.drawTextWithShadow(client.textRenderer, Text.translatable("gui.practicebot.stats.nearest", modeId), x, y, 0xCFCFCF);
		y += 12;
		context.drawTextWithShadow(client.textRenderer,
			Text.translatable("gui.practicebot.stats.hp", String.format("%.0f", nearest.getHealth()), String.format("%.0f", nearest.getMaxHealth())),
			x, y, 0xCFCFCF);
		y += 12;
		context.drawTextWithShadow(client.textRenderer, Text.translatable("gui.practicebot.stats.last", String.format("%.1f", last)), x, y, 0xCFCFCF);
		y += 12;
		context.drawTextWithShadow(client.textRenderer, Text.translatable("gui.practicebot.stats.pops", pops), x, y, 0xCFCFCF);
	}
}
