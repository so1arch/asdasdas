package com.medievalcombat;

import com.medievalcombat.combat.CombatManager;
import com.medievalcombat.item.ModItems;
import com.medievalcombat.net.ModNetworking;
import net.fabricmc.api.ModInitializer;

public class MedievalCombat implements ModInitializer {
    public static final String MOD_ID = "medievalcombat";

    @Override
    public void onInitialize() {
        ModItems.register();
        ModNetworking.registerServer();
        CombatManager.register();
    }
}
