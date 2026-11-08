package com.practicebot.bot;

import org.jspecify.annotations.Nullable;

public enum BotMode {
	SHIELD("shield"),
	STILL("still"),
	WIND("wind"),
	ELYTRA("elytra"),
	ELYTRA_SHIELD("elytra_shield"),
	MOVE("move"),
	GAP("gap"),
	CRYSTAL("crystal");

	private final String id;

	BotMode(String id) {
		this.id = id;
	}

	public String getId() {
		return this.id;
	}

	public boolean usesShield() {
		return this == SHIELD || this == ELYTRA_SHIELD;
	}

	public boolean usesElytra() {
		return this == ELYTRA || this == ELYTRA_SHIELD;
	}

	/** Modes that stay locked in place (no gravity walk). */
	public boolean isFrozen() {
		return this != WIND && this != MOVE;
	}

	public boolean forcesNoKnockback() {
		return this == CRYSTAL;
	}

	public boolean forcesGlowing() {
		return this == CRYSTAL;
	}

	@Nullable
	public static BotMode fromId(String id) {
		for (BotMode mode : values()) {
			if (mode.id.equals(id)) {
				return mode;
			}
		}
		return null;
	}

	/** Longer ids first so tag inference does not steal prefixes. */
	public static BotMode[] orderedForTags() {
		return new BotMode[] {
			ELYTRA_SHIELD, SHIELD, STILL, WIND, ELYTRA, MOVE, GAP, CRYSTAL
		};
	}
}
