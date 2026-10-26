// SPDX-License-Identifier: GPL-3.0-only
package io.github.agzes.afr.client.ui;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;

public final class Items {

	private Items() {}

	public static ItemStack fromId(String id) {
		if (id == null || id.isBlank()) {
			return ItemStack.EMPTY;
		}
		try {
			ResourceLocation rl = ResourceLocation.tryParse(
				id.contains(":") ? id : "minecraft:" + id
			);
			if (rl == null) {
				return ItemStack.EMPTY;
			}
			return new ItemStack(BuiltInRegistries.ITEM.get(rl));
		} catch (Exception e) {
			return ItemStack.EMPTY;
		}
	}
}
