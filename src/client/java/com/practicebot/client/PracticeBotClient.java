package com.practicebot.client;

import com.practicebot.client.gui.PracticeBotScreen;
import com.practicebot.network.DamageHologramPayload;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import net.minecraft.util.Identifier;
import org.lwjgl.glfw.GLFW;

public class PracticeBotClient implements ClientModInitializer {
	public static final KeyBinding.Category CATEGORY = KeyBinding.Category.create(Identifier.of("practicebot", "main"));
	public static KeyBinding openGuiKey;
	public static KeyBinding statsKey;

	@Override
	public void onInitializeClient() {
		ClientSpawnPreset.get();

		openGuiKey = KeyBindingHelper.registerKeyBinding(new KeyBinding(
			"key.practicebot.open_gui",
			InputUtil.Type.KEYSYM,
			GLFW.GLFW_KEY_B,
			CATEGORY
		));
		statsKey = KeyBindingHelper.registerKeyBinding(new KeyBinding(
			"key.practicebot.toggle_stats",
			InputUtil.Type.KEYSYM,
			GLFW.GLFW_KEY_H,
			CATEGORY
		));

		ClientPlayNetworking.registerGlobalReceiver(DamageHologramPayload.ID, (payload, context) -> {
			context.client().execute(() -> DamageHologramRenderer.spawn(payload.x(), payload.y(), payload.z(), payload.amount()));
		});

		ClientTickEvents.END_CLIENT_TICK.register(client -> {
			while (openGuiKey.wasPressed()) {
				if (client.player != null) {
					client.setScreen(new PracticeBotScreen());
				}
			}
			while (statsKey.wasPressed()) {
				SessionStatsOverlay.toggle();
			}
			DamageHologramRenderer.tick();
		});

		HudRenderCallback.EVENT.register((context, tickCounter) -> {
			DamageHologramRenderer.renderHud(context);
			SessionStatsOverlay.render(context, net.minecraft.client.MinecraftClient.getInstance());
		});
	}
}
