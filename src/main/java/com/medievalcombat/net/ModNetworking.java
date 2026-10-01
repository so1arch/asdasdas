package com.medievalcombat.net;

import com.medievalcombat.MedievalCombat;
import com.medievalcombat.combat.CombatManager;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.util.Identifier;

public final class ModNetworking {
    /** C2S: current attack/guard zone (byte). */
    public static final Identifier ZONE = new Identifier(MedievalCombat.MOD_ID, "zone");
    /** S2C: current stamina (float). */
    public static final Identifier STAMINA = new Identifier(MedievalCombat.MOD_ID, "stamina");

    private ModNetworking() {}

    public static void registerServer() {
        ServerPlayNetworking.registerGlobalReceiver(ZONE, (server, player, handler, buf, responseSender) -> {
            int id = buf.readByte();
            server.execute(() -> CombatManager.setZone(player, id));
        });
    }
}
