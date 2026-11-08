package com.practicebot.bot;

import org.jspecify.annotations.Nullable;

public enum ShowHpMode {
	WHEN_HURT("when_hurt"),
	ALWAYS("always"),
	NEVER("never");

	private final String id;

	ShowHpMode(String id) {
		this.id = id;
	}

	public String getId() {
		return this.id;
	}

	public ShowHpMode next() {
		ShowHpMode[] values = values();
		return values[(this.ordinal() + 1) % values.length];
	}

	@Nullable
	public static ShowHpMode fromId(String id) {
		for (ShowHpMode mode : values()) {
			if (mode.id.equalsIgnoreCase(id)) {
				return mode;
			}
		}
		return null;
	}
}
