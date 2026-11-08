package com.practicebot.mixin;

import com.practicebot.bot.PracticeBotHandler;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.decoration.MannequinEntity;
import net.minecraft.server.world.ServerWorld;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(LivingEntity.class)
public abstract class LivingEntityMixin {
	@Inject(method = "tick", at = @At("HEAD"))
	private void practicebot$hydrateClientShieldPose(CallbackInfo ci) {
		LivingEntity self = (LivingEntity) (Object) this;
		if (!self.getEntityWorld().isClient() || !(self instanceof MannequinEntity)) {
			return;
		}
		if (!self.isUsingItem() || !self.getActiveItem().isEmpty()) {
			return;
		}
		var stack = self.getStackInHand(self.getActiveHand());
		if (stack.isEmpty()) {
			return;
		}
		LivingEntityAccessor accessor = (LivingEntityAccessor) self;
		accessor.practicebot$setActiveItemStack(stack);
		accessor.practicebot$setItemUseTimeLeft(stack.getMaxUseTime(self));
	}

	@Inject(method = "tick", at = @At("TAIL"))
	private void practicebot$tickBot(CallbackInfo ci) {
		LivingEntity self = (LivingEntity) (Object) this;
		if (self instanceof MannequinEntity mannequin && PracticeBotHandler.getMode(mannequin) != null) {
			PracticeBotHandler.tickBot(mannequin);
		}
	}

	@Inject(method = "takeShieldHit", at = @At("TAIL"))
	private void practicebot$shortShieldStun(ServerWorld world, LivingEntity attacker, CallbackInfo ci) {
		LivingEntity self = (LivingEntity) (Object) this;
		if (!(self instanceof MannequinEntity mannequin)) {
			return;
		}
		if (PracticeBotHandler.getMode(mannequin) == null) {
			return;
		}
		if (attacker.getWeaponDisableBlockingForSeconds() > 0.0F) {
			PracticeBotHandler.disableShield(mannequin);
		}
	}

	@Inject(method = "handleFallDamage", at = @At("HEAD"), cancellable = true)
	private void practicebot$noWindFallDamage(double fallDistance, float damagePerDistance, DamageSource damageSource, CallbackInfoReturnable<Boolean> cir) {
		LivingEntity self = (LivingEntity) (Object) this;
		if (self instanceof MannequinEntity mannequin && PracticeBotHandler.shouldIgnoreFallDamage(mannequin)) {
			cir.setReturnValue(false);
		}
	}

	@Inject(method = "takeKnockback", at = @At("HEAD"), cancellable = true)
	private void practicebot$noKnockback(double strength, double x, double z, CallbackInfo ci) {
		LivingEntity self = (LivingEntity) (Object) this;
		if (self instanceof MannequinEntity mannequin && PracticeBotHandler.shouldIgnoreKnockback(mannequin)) {
			ci.cancel();
		}
	}

	@Inject(method = "damage", at = @At("RETURN"))
	private void practicebot$onDamaged(ServerWorld world, DamageSource source, float amount, CallbackInfoReturnable<Boolean> cir) {
		if (!cir.getReturnValue()) {
			return;
		}
		LivingEntity self = (LivingEntity) (Object) this;
		if (self instanceof MannequinEntity mannequin && PracticeBotHandler.getMode(mannequin) != null) {
			PracticeBotHandler.onBotDamaged(mannequin, source, amount);
		}
	}

	@Inject(method = "tryUseDeathProtector", at = @At("RETURN"))
	private void practicebot$onTotem(DamageSource source, CallbackInfoReturnable<Boolean> cir) {
		if (!cir.getReturnValue()) {
			return;
		}
		LivingEntity self = (LivingEntity) (Object) this;
		if (self instanceof MannequinEntity mannequin && PracticeBotHandler.getMode(mannequin) != null) {
			PracticeBotHandler.onTotemPop(mannequin);
		}
	}
}
