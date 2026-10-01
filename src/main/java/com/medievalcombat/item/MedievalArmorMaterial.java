package com.medievalcombat.item;

import com.medievalcombat.MedievalCombat;
import java.util.function.Supplier;
import net.minecraft.item.ArmorItem;
import net.minecraft.item.ArmorMaterial;
import net.minecraft.item.Items;
import net.minecraft.recipe.Ingredient;
import net.minecraft.sound.SoundEvent;
import net.minecraft.sound.SoundEvents;

public enum MedievalArmorMaterial implements ArmorMaterial {
    //            name        dur  prot{boots,legs,chest,helm}  ench  sound                               tough  kb    slash thrust weight  repair
    GAMBESON("gambeson",       8, new int[]{1, 2, 3, 1}, 12, SoundEvents.ITEM_ARMOR_EQUIP_LEATHER, 0f,  0f,    0.10f, 0.06f, 0.5f, () -> Ingredient.ofItems(Items.WHITE_WOOL)),
    MAIL("mail",              16, new int[]{2, 4, 5, 2}, 12, SoundEvents.ITEM_ARMOR_EQUIP_CHAIN,   0f,  0f,    0.22f, 0.08f, 1.5f, () -> Ingredient.ofItems(Items.IRON_INGOT)),
    BRIGANDINE("brigandine",  20, new int[]{2, 5, 6, 2}, 10, SoundEvents.ITEM_ARMOR_EQUIP_IRON,    0.5f, 0f,   0.25f, 0.15f, 2.0f, () -> Ingredient.ofItems(Items.IRON_INGOT)),
    PLATE("plate",            30, new int[]{3, 6, 8, 3}, 9,  SoundEvents.ITEM_ARMOR_EQUIP_IRON,    2.0f, 0.05f, 0.40f, 0.22f, 3.5f, () -> Ingredient.ofItems(ModItems.STEEL_INGOT));

    private static final int[] BASE_DURABILITY = {13, 15, 16, 11}; // boots, legs, chest, helmet

    private final String name;
    private final int durabilityMult;
    private final int[] protection;
    private final int enchantability;
    private final SoundEvent equipSound;
    private final float toughness;
    private final float knockbackResistance;
    public final float slashResist;
    public final float thrustResist;
    public final float weight;
    private final Supplier<Ingredient> repair;

    MedievalArmorMaterial(String name, int durabilityMult, int[] protection, int enchantability, SoundEvent equipSound,
                          float toughness, float knockbackResistance, float slashResist, float thrustResist,
                          float weight, Supplier<Ingredient> repair) {
        this.name = name;
        this.durabilityMult = durabilityMult;
        this.protection = protection;
        this.enchantability = enchantability;
        this.equipSound = equipSound;
        this.toughness = toughness;
        this.knockbackResistance = knockbackResistance;
        this.slashResist = slashResist;
        this.thrustResist = thrustResist;
        this.weight = weight;
        this.repair = repair;
    }

    private static int idx(ArmorItem.Type type) {
        return switch (type) {
            case BOOTS -> 0;
            case LEGGINGS -> 1;
            case CHESTPLATE -> 2;
            case HELMET -> 3;
        };
    }

    @Override public int getDurability(ArmorItem.Type type) { return BASE_DURABILITY[idx(type)] * durabilityMult; }
    @Override public int getProtection(ArmorItem.Type type) { return protection[idx(type)]; }
    @Override public int getEnchantability() { return enchantability; }
    @Override public SoundEvent getEquipSound() { return equipSound; }
    @Override public Ingredient getRepairIngredient() { return repair.get(); }
    /** "modid:name" -> textures/models/armor/<name>_layer_N.png in that namespace. */
    @Override public String getName() { return MedievalCombat.MOD_ID + ":" + name; }
    @Override public float getToughness() { return toughness; }
    @Override public float getKnockbackResistance() { return knockbackResistance; }
}
