package com.practicebot.mixin;

import net.minecraft.component.type.ProfileComponent;
import net.minecraft.entity.decoration.MannequinEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(MannequinEntity.class)
public interface MannequinEntityAccessor {
	@Invoker("setImmovable")
	void invokeSetImmovable(boolean immovable);

	@Invoker("setMannequinProfile")
	void invokeSetMannequinProfile(ProfileComponent profile);

	@Invoker("setHideDescription")
	void invokeSetHideDescription(boolean hideDescription);
}
