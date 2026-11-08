package com.practicebot.bot;

import net.minecraft.item.Item;
import net.minecraft.item.Items;
import org.jspecify.annotations.Nullable;

public enum ArmorType {
	NONE("none", null, null, null, null),
	LEATHER("leather", Items.LEATHER_HELMET, Items.LEATHER_CHESTPLATE, Items.LEATHER_LEGGINGS, Items.LEATHER_BOOTS),
	CHAIN("chain", Items.CHAINMAIL_HELMET, Items.CHAINMAIL_CHESTPLATE, Items.CHAINMAIL_LEGGINGS, Items.CHAINMAIL_BOOTS),
	IRON("iron", Items.IRON_HELMET, Items.IRON_CHESTPLATE, Items.IRON_LEGGINGS, Items.IRON_BOOTS),
	GOLD("gold", Items.GOLDEN_HELMET, Items.GOLDEN_CHESTPLATE, Items.GOLDEN_LEGGINGS, Items.GOLDEN_BOOTS),
	DIAMOND("diamond", Items.DIAMOND_HELMET, Items.DIAMOND_CHESTPLATE, Items.DIAMOND_LEGGINGS, Items.DIAMOND_BOOTS),
	NETHERITE("netherite", Items.NETHERITE_HELMET, Items.NETHERITE_CHESTPLATE, Items.NETHERITE_LEGGINGS, Items.NETHERITE_BOOTS);

	private final String id;
	@Nullable
	private final Item helmet;
	@Nullable
	private final Item chestplate;
	@Nullable
	private final Item leggings;
	@Nullable
	private final Item boots;

	ArmorType(String id, @Nullable Item helmet, @Nullable Item chestplate, @Nullable Item leggings, @Nullable Item boots) {
		this.id = id;
		this.helmet = helmet;
		this.chestplate = chestplate;
		this.leggings = leggings;
		this.boots = boots;
	}

	public String getId() {
		return this.id;
	}

	@Nullable
	public Item getHelmet() {
		return this.helmet;
	}

	@Nullable
	public Item getChestplate() {
		return this.chestplate;
	}

	@Nullable
	public Item getLeggings() {
		return this.leggings;
	}

	@Nullable
	public Item getBoots() {
		return this.boots;
	}

	public ArmorType next() {
		ArmorType[] values = values();
		return values[(this.ordinal() + 1) % values.length];
	}

	@Nullable
	public static ArmorType fromId(String id) {
		for (ArmorType type : values()) {
			if (type.id.equalsIgnoreCase(id)) {
				return type;
			}
		}
		return null;
	}
}
