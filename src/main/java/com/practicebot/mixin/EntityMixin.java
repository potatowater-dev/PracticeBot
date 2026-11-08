package com.practicebot.mixin;

import com.practicebot.bot.PracticeBotHandler;
import net.minecraft.entity.Entity;
import net.minecraft.entity.decoration.MannequinEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Entity.class)
public abstract class EntityMixin {
	@Inject(method = "addVelocity(DDD)V", at = @At("HEAD"), cancellable = true)
	private void practicebot$blockExternalPush(double deltaX, double deltaY, double deltaZ, CallbackInfo ci) {
		Entity self = (Entity) (Object) this;
		if (self instanceof MannequinEntity mannequin && PracticeBotHandler.shouldIgnoreKnockback(mannequin)) {
			ci.cancel();
		}
	}
}
