package com.practicebot.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.BoolArgumentType;
import com.mojang.brigadier.arguments.FloatArgumentType;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.practicebot.bot.ArmorType;
import com.practicebot.bot.BotMode;
import com.practicebot.bot.BotSettings;
import com.practicebot.bot.PracticeBotHandler;
import net.minecraft.command.CommandRegistryAccess;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;

public final class PBotCommands {
	private PBotCommands() {
	}

	public static void register(CommandDispatcher<ServerCommandSource> dispatcher, CommandRegistryAccess access, CommandManager.RegistrationEnvironment environment) {
		LiteralArgumentBuilder<ServerCommandSource> root = CommandManager.literal("pbot")
			.requires(ServerCommandSource::isExecutedByPlayer);

		for (BotMode mode : BotMode.values()) {
			root.then(modeBranch(mode));
		}

		LiteralArgumentBuilder<ServerCommandSource> remove = CommandManager.literal("remove")
			.executes(PBotCommands::removeNearest)
			.then(CommandManager.literal("all").executes(PBotCommands::removeAll))
			.then(CommandManager.literal("nearest").executes(PBotCommands::removeNearest));
		for (BotMode mode : BotMode.values()) {
			remove.then(CommandManager.literal(mode.getId()).executes(ctx -> removeMode(ctx, mode)));
		}
		root.then(remove);

		root.then(CommandManager.literal("list").executes(PBotCommands::list));
		root.then(CommandManager.literal("resetpops")
			.executes(PBotCommands::resetPopsNearest)
			.then(CommandManager.literal("all").executes(PBotCommands::resetPopsAll))
			.then(CommandManager.literal("nearest").executes(PBotCommands::resetPopsNearest)));

		dispatcher.register(root);
	}

	private static LiteralArgumentBuilder<ServerCommandSource> modeBranch(BotMode mode) {
		LiteralArgumentBuilder<ServerCommandSource> branch = CommandManager.literal(mode.getId())
			.executes(ctx -> spawn(ctx, mode, BotSettings.DEFAULT));

		branch.then(CommandManager.argument("armor", StringArgumentType.word())
			.suggests((ctx, builder) -> {
				for (ArmorType type : ArmorType.values()) {
					builder.suggest(type.getId());
				}
				return builder.buildFuture();
			})
			.executes(ctx -> spawn(ctx, mode, fromArmor(ctx)))
			.then(CommandManager.argument("stun", IntegerArgumentType.integer(0, 100))
				.executes(ctx -> spawn(ctx, mode, fromArmor(ctx).withShieldDisableTicks(IntegerArgumentType.getInteger(ctx, "stun"))))
				.then(CommandManager.argument("wind", FloatArgumentType.floatArg(0.1F, 5.0F))
					.executes(ctx -> spawn(ctx, mode, fromWind(ctx)))
					.then(CommandManager.argument("invuln", BoolArgumentType.bool())
						.executes(ctx -> spawn(ctx, mode, fromWind(ctx).withInvulnerable(BoolArgumentType.getBool(ctx, "invuln"))))
						.then(CommandManager.argument("totems", BoolArgumentType.bool())
							.executes(ctx -> spawn(ctx, mode, fromTotems(ctx)))
							.then(CommandManager.argument("nokb", BoolArgumentType.bool())
								.executes(ctx -> spawn(ctx, mode, fromTotems(ctx).withNoKnockback(BoolArgumentType.getBool(ctx, "nokb"))))
								.then(CommandManager.argument("skin", StringArgumentType.word())
									.executes(ctx -> spawn(ctx, mode, fromSkin(ctx)))
									.then(CommandManager.argument("name", StringArgumentType.greedyString())
										.executes(ctx -> spawn(ctx, mode, fromSkin(ctx).withDisplayName(StringArgumentType.getString(ctx, "name"))))
									)
								)
							)
						)
					)
				)
			)
		);

		return branch;
	}

	private static BotSettings fromArmor(CommandContext<ServerCommandSource> ctx) {
		ArmorType armor = ArmorType.fromId(StringArgumentType.getString(ctx, "armor"));
		return BotSettings.DEFAULT.withArmorType(armor == null ? ArmorType.NETHERITE : armor);
	}

	private static BotSettings fromWind(CommandContext<ServerCommandSource> ctx) {
		return fromArmor(ctx)
			.withShieldDisableTicks(IntegerArgumentType.getInteger(ctx, "stun"))
			.withWindBoost(FloatArgumentType.getFloat(ctx, "wind"));
	}

	private static BotSettings fromTotems(CommandContext<ServerCommandSource> ctx) {
		return fromWind(ctx)
			.withInvulnerable(BoolArgumentType.getBool(ctx, "invuln"))
			.withInfiniteTotems(BoolArgumentType.getBool(ctx, "totems"));
	}

	private static BotSettings fromSkin(CommandContext<ServerCommandSource> ctx) {
		return fromTotems(ctx)
			.withNoKnockback(BoolArgumentType.getBool(ctx, "nokb"))
			.withSkinName(StringArgumentType.getString(ctx, "skin"));
	}

	private static int spawn(CommandContext<ServerCommandSource> context, BotMode mode, BotSettings settings) {
		ServerPlayerEntity player = context.getSource().getPlayer();
		if (player == null) {
			return 0;
		}
		PracticeBotHandler.spawn(player, mode, settings.clamped());
		context.getSource().sendFeedback(() -> Text.translatable("commands.practicebot.spawned", mode.getId()), false);
		return 1;
	}

	private static int removeNearest(CommandContext<ServerCommandSource> context) {
		ServerPlayerEntity player = context.getSource().getPlayer();
		if (player == null) {
			return 0;
		}
		if (PracticeBotHandler.removeNearest(player)) {
			context.getSource().sendFeedback(() -> Text.translatable("commands.practicebot.removed"), false);
			return 1;
		}
		context.getSource().sendError(Text.translatable("commands.practicebot.none"));
		return 0;
	}

	private static int removeAll(CommandContext<ServerCommandSource> context) {
		ServerPlayerEntity player = context.getSource().getPlayer();
		if (player == null) {
			return 0;
		}
		int count = PracticeBotHandler.removeAll(player);
		context.getSource().sendFeedback(() -> Text.translatable("commands.practicebot.removed_count", count), false);
		return count;
	}

	private static int removeMode(CommandContext<ServerCommandSource> context, BotMode mode) {
		ServerPlayerEntity player = context.getSource().getPlayer();
		if (player == null) {
			return 0;
		}
		int count = PracticeBotHandler.removeByMode(player, mode);
		context.getSource().sendFeedback(() -> Text.translatable("commands.practicebot.removed_mode", count, mode.getId()), false);
		return count;
	}

	private static int list(CommandContext<ServerCommandSource> context) {
		ServerPlayerEntity player = context.getSource().getPlayer();
		if (player == null) {
			return 0;
		}
		var lines = PracticeBotHandler.listNearby(player);
		if (lines.isEmpty()) {
			context.getSource().sendError(Text.translatable("commands.practicebot.none"));
			return 0;
		}
		context.getSource().sendFeedback(() -> Text.translatable("commands.practicebot.list_header", lines.size()), false);
		for (Text line : lines) {
			context.getSource().sendFeedback(() -> line, false);
		}
		return lines.size();
	}

	private static int resetPopsNearest(CommandContext<ServerCommandSource> context) {
		ServerPlayerEntity player = context.getSource().getPlayer();
		if (player == null) {
			return 0;
		}
		if (PracticeBotHandler.resetPopsNearest(player) == 0) {
			context.getSource().sendError(Text.translatable("commands.practicebot.none"));
			return 0;
		}
		context.getSource().sendFeedback(() -> Text.translatable("commands.practicebot.reset_pops"), false);
		return 1;
	}

	private static int resetPopsAll(CommandContext<ServerCommandSource> context) {
		ServerPlayerEntity player = context.getSource().getPlayer();
		if (player == null) {
			return 0;
		}
		int n = PracticeBotHandler.resetPopsAll(player);
		context.getSource().sendFeedback(() -> Text.translatable("commands.practicebot.reset_pops_count", n), false);
		return n;
	}
}
