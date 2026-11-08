package com.practicebot.mixin;

import net.minecraft.entity.LivingEntity;
import net.minecraft.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(LivingEntity.class)
public interface LivingEntityAccessor {
	@Accessor("activeItemStack")
	void practicebot$setActiveItemStack(ItemStack stack);

	@Accessor("itemUseTimeLeft")
	void practicebot$setItemUseTimeLeft(int ticks);
}
