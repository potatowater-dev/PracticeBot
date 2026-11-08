package com.practicebot.mixin;

import com.practicebot.bot.BotMode;
import com.practicebot.bot.BotSettings;
import com.practicebot.bot.PracticeBotAttachments;
import com.practicebot.bot.PracticeBotHandler;
import net.minecraft.entity.decoration.MannequinEntity;
import net.minecraft.storage.ReadView;
import net.minecraft.storage.WriteView;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Extra NBT backup for mode/settings so bots survive rejoin even if attachments drop.
 */
@Mixin(MannequinEntity.class)
public abstract class MannequinEntityMixin {
	private static final String MODE_KEY = "PracticeBotMode";
	private static final String SHIELD_KEY = "PracticeBotShieldDisable";
	private static final String SETTINGS_KEY = "PracticeBotSettings";
	private static final String POPS_KEY = "PracticeBotPops";
	private static final String HOME_X = "PracticeBotHomeX";
	private static final String HOME_Y = "PracticeBotHomeY";
	private static final String HOME_Z = "PracticeBotHomeZ";
	private static final String HAS_HOME = "PracticeBotHasHome";

	@Inject(method = "writeCustomData", at = @At("TAIL"))
	private void practicebot$write(WriteView view, CallbackInfo ci) {
		MannequinEntity self = (MannequinEntity) (Object) this;
		PracticeBotAttachments.Data data = self.getAttached(PracticeBotAttachments.DATA);
		if (data == null) {
			return;
		}
		view.putString(MODE_KEY, data.mode());
		view.putInt(SHIELD_KEY, data.shieldDisabledTicks());
		view.put(SETTINGS_KEY, BotSettings.CODEC, data.settings());
		view.putInt(POPS_KEY, data.totemPops());
		view.putDouble(HOME_X, data.homeX());
		view.putDouble(HOME_Y, data.homeY());
		view.putDouble(HOME_Z, data.homeZ());
		view.putBoolean(HAS_HOME, data.hasHome());
	}

	@Inject(method = "readCustomData", at = @At("TAIL"))
	private void practicebot$read(ReadView view, CallbackInfo ci) {
		MannequinEntity self = (MannequinEntity) (Object) this;
		view.getOptionalString(MODE_KEY).ifPresent(modeId -> {
			BotMode mode = BotMode.fromId(modeId);
			if (mode == null) {
				return;
			}
			BotSettings settings = view.read(SETTINGS_KEY, BotSettings.CODEC).orElse(BotSettings.DEFAULT);
			int shield = view.getInt(SHIELD_KEY, 0);
			PracticeBotHandler.setBotData(self, mode, settings, shield);
			PracticeBotAttachments.Data data = self.getAttached(PracticeBotAttachments.DATA);
			if (data == null) {
				return;
			}
			int pops = view.getInt(POPS_KEY, 0);
			boolean hasHome = view.getBoolean(HAS_HOME, false);
			PracticeBotAttachments.Data next = data.withTotemPops(pops);
			if (hasHome) {
				next = next.withHome(view.getDouble(HOME_X, self.getX()), view.getDouble(HOME_Y, self.getY()), view.getDouble(HOME_Z, self.getZ()));
			}
			self.setAttached(PracticeBotAttachments.DATA, next);
		});
	}
}
