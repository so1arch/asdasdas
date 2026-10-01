package com.medievalcombat.item;

import com.medievalcombat.MedievalCombat;
import java.util.ArrayList;
import java.util.List;
import net.fabricmc.fabric.api.item.v1.FabricItemSettings;
import net.fabricmc.fabric.api.itemgroup.v1.FabricItemGroup;
import net.minecraft.item.ArmorItem;
import net.minecraft.item.Item;
import net.minecraft.item.ItemGroup;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

public final class ModItems {
    private static final List<Item> ALL = new ArrayList<>();

    public static final Item STEEL_INGOT = add("steel_ingot", new Item(new FabricItemSettings()));

    //                                                     material                      slash thrust cd  stam blockMult pierce
    public static final Item ARMING_SWORD = add("arming_sword", new MedievalSwordItem(MedievalToolMaterial.ARMING,    6.5f, 5.0f, 10, 14f, 1.0f, 0.10f, new FabricItemSettings()));
    public static final Item LONGSWORD    = add("longsword",    new MedievalSwordItem(MedievalToolMaterial.LONGSWORD, 8.0f, 7.0f, 14, 20f, 0.8f, 0.25f, new FabricItemSettings()));
    public static final Item FALCHION     = add("falchion",     new MedievalSwordItem(MedievalToolMaterial.FALCHION,  8.5f, 3.5f, 13, 18f, 1.2f, 0.00f, new FabricItemSettings()));
    public static final Item DAGGER       = add("dagger",       new MedievalSwordItem(MedievalToolMaterial.DAGGER,    3.5f, 5.5f,  6,  8f, 1.6f, 0.50f, new FabricItemSettings()));
    public static final Item ESTOC        = add("estoc",        new MedievalSwordItem(MedievalToolMaterial.ESTOC,     3.5f, 9.0f, 12, 16f, 1.3f, 0.40f, new FabricItemSettings()));

    static {
        for (MedievalArmorMaterial m : MedievalArmorMaterial.values()) {
            String n = m.name().toLowerCase();
            add(n + "_helmet", new ArmorItem(m, ArmorItem.Type.HELMET, new FabricItemSettings()));
            add(n + "_chestplate", new ArmorItem(m, ArmorItem.Type.CHESTPLATE, new FabricItemSettings()));
            add(n + "_leggings", new ArmorItem(m, ArmorItem.Type.LEGGINGS, new FabricItemSettings()));
            add(n + "_boots", new ArmorItem(m, ArmorItem.Type.BOOTS, new FabricItemSettings()));
        }
    }

    private ModItems() {}

    private static Item add(String name, Item item) {
        Registry.register(Registries.ITEM, new Identifier(MedievalCombat.MOD_ID, name), item);
        ALL.add(item);
        return item;
    }

    public static void register() {
        ItemGroup group = FabricItemGroup.builder()
                .icon(() -> new ItemStack(LONGSWORD))
                .displayName(Text.translatable("itemGroup.medievalcombat"))
                .entries((ctx, entries) -> ALL.forEach(entries::add))
                .build();
        Registry.register(Registries.ITEM_GROUP, new Identifier(MedievalCombat.MOD_ID, "main"), group);
    }
}
