package com.practicebot.client;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.mojang.serialization.JsonOps;
import com.practicebot.bot.BotSettings;
import net.fabricmc.loader.api.FabricLoader;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;

public final class ClientSpawnPreset {
	private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
	private static final Path CONFIG_PATH = FabricLoader.getInstance().getConfigDir().resolve("practicebot-preset.json");
	private static BotSettings settings = BotSettings.DEFAULT;
	private static boolean loaded;

	private ClientSpawnPreset() {
	}

	public static BotSettings get() {
		ensureLoaded();
		return settings;
	}

	public static void set(BotSettings next) {
		settings = next.clamped();
		save();
	}

	public static void reset() {
		settings = BotSettings.DEFAULT;
		save();
	}

	private static void ensureLoaded() {
		if (loaded) {
			return;
		}
		loaded = true;
		if (!Files.isRegularFile(CONFIG_PATH)) {
			return;
		}
		try (Reader reader = Files.newBufferedReader(CONFIG_PATH)) {
			JsonObject json = GSON.fromJson(reader, JsonObject.class);
			if (json != null) {
				settings = BotSettings.CODEC.parse(JsonOps.INSTANCE, json)
					.result()
					.orElse(BotSettings.DEFAULT)
					.clamped();
			}
		} catch (Exception ignored) {
			settings = BotSettings.DEFAULT;
		}
	}

	private static void save() {
		try {
			Files.createDirectories(CONFIG_PATH.getParent());
			var encoded = BotSettings.CODEC.encodeStart(JsonOps.INSTANCE, settings).result();
			if (encoded.isEmpty()) {
				return;
			}
			try (Writer writer = Files.newBufferedWriter(CONFIG_PATH)) {
				GSON.toJson(encoded.get(), writer);
			}
		} catch (IOException ignored) {
		}
	}
}
