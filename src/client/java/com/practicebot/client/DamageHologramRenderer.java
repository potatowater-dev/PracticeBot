package com.practicebot.client;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.util.math.MathHelper;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/** Screen-space floating damage numbers (world camera APIs vary too much across snapshots). */
public final class DamageHologramRenderer {
	private static final List<Hologram> HOLOGRAMS = new ArrayList<>();

	private DamageHologramRenderer() {
	}

	public static void spawn(double x, double y, double z, float amount) {
		MinecraftClient client = MinecraftClient.getInstance();
		int baseX = client.getWindow().getScaledWidth() / 2;
		int baseY = client.getWindow().getScaledHeight() / 2 - 40;
		int jitter = (HOLOGRAMS.size() % 5) * 12 - 24;
		HOLOGRAMS.add(new Hologram(baseX + jitter, baseY, amount, 35));
	}

	public static void tick() {
		Iterator<Hologram> it = HOLOGRAMS.iterator();
		while (it.hasNext()) {
			Hologram h = it.next();
			h.age++;
			h.y -= 0.8F;
			if (h.age >= h.life) {
				it.remove();
			}
		}
	}

	public static void renderHud(DrawContext context) {
		MinecraftClient client = MinecraftClient.getInstance();
		if (client.player == null || HOLOGRAMS.isEmpty()) {
			return;
		}
		for (Hologram h : HOLOGRAMS) {
			float life = (float) h.age / h.life;
			int alpha = MathHelper.clamp((int) ((1.0F - life) * 255.0F), 0, 255);
			int color = (alpha << 24) | 0xFF5555;
			String label = String.format("%.1f", h.amount);
			context.drawCenteredTextWithShadow(client.textRenderer, label, (int) h.x, (int) h.y, color);
		}
	}

	private static final class Hologram {
		final float x;
		float y;
		final float amount;
		final int life;
		int age;

		Hologram(float x, float y, float amount, int life) {
			this.x = x;
			this.y = y;
			this.amount = amount;
			this.life = life;
		}
	}
}
