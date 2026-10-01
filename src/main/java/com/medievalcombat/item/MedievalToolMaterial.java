package com.medievalcombat.item;

import java.util.function.Supplier;
import net.minecraft.item.Items;
import net.minecraft.item.ToolMaterial;
import net.minecraft.recipe.Ingredient;

public enum MedievalToolMaterial implements ToolMaterial {
    ARMING(300, 14, () -> Ingredient.ofItems(Items.IRON_INGOT)),
    FALCHION(330, 12, () -> Ingredient.ofItems(Items.IRON_INGOT)),
    DAGGER(220, 14, () -> Ingredient.ofItems(Items.IRON_INGOT)),
    LONGSWORD(700, 12, () -> Ingredient.ofItems(ModItems.STEEL_INGOT)),
    ESTOC(600, 12, () -> Ingredient.ofItems(ModItems.STEEL_INGOT));

    private final int durability;
    private final int enchantability;
    private final Supplier<Ingredient> repair;

    MedievalToolMaterial(int durability, int enchantability, Supplier<Ingredient> repair) {
        this.durability = durability;
        this.enchantability = enchantability;
        this.repair = repair;
    }

    @Override public int getDurability() { return durability; }
    @Override public float getMiningSpeedMultiplier() { return 1.5f; }
    @Override public float getAttackDamage() { return 0f; } // real damage is computed by CombatManager
    @Override public int getMiningLevel() { return 2; }
    @Override public int getEnchantability() { return enchantability; }
    @Override public Ingredient getRepairIngredient() { return repair.get(); }
}
