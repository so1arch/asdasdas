package com.medievalcombat.client;

import com.medievalcombat.combat.Zone;
import com.medievalcombat.item.MedievalSwordItem;
import com.medievalcombat.net.ModNetworking;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.util.math.MathHelper;

/**
 * The attack/guard zone follows the mouse: flick left/right/up/down to pick a zone,
 * hold the crosshair still for ~0.7 s to return to the centre (thrust).
 */
@Environment(EnvType.CLIENT)
public class MedievalCombatClient implements ClientModInitializer {
    private static final float THRESHOLD = 3.0f; // degrees per tick
    private static final int RETURN_TO_CENTER_TICKS = 14;

    private static float stamina = 100f;
    private static Zone zone = Zone.THRUST;
    private static Zone sentZone = null;
    private static float prevYaw, prevPitch;
    private static boolean initialised = false;
    private static int idleTicks = 0;

    @Override
    public void onInitializeClient() {
        ClientPlayNetworking.registerGlobalReceiver(ModNetworking.STAMINA, (client, handler, buf, sender) -> {
            float v = buf.readFloat();
            client.execute(() -> stamina = v);
        });
        ClientTickEvents.END_CLIENT_TICK.register(MedievalCombatClient::tick);
        HudRenderCallback.EVENT.register(MedievalCombatClient::renderHud);
    }

    private static void tick(MinecraftClient mc) {
        ClientPlayerEntity p = mc.player;
        if (p == null || mc.currentScreen != null) {
            initialised = false;
            return;
        }
        float yaw = p.getYaw(), pitch = p.getPitch();
        if (!initialised) {
            prevYaw = yaw;
            prevPitch = pitch;
            initialised = true;
            return;
        }
        float dy = MathHelper.wrapDegrees(yaw - prevYaw);
        float dp = pitch - prevPitch;
        prevYaw = yaw;
        prevPitch = pitch;

        if (!(p.getMainHandStack().getItem() instanceof MedievalSwordItem)) {
            zone = Zone.THRUST;
            sentZone = null;
            return;
        }

        if (Math.abs(dy) >= THRESHOLD || Math.abs(dp) >= THRESHOLD) {
            idleTicks = 0;
            if (Math.abs(dy) >= Math.abs(dp)) {
                zone = dy < 0 ? Zone.LEFT : Zone.RIGHT;
            } else {
                zone = dp < 0 ? Zone.UP : Zone.DOWN;
            }
        } else if (++idleTicks > RETURN_TO_CENTER_TICKS) {
            zone = Zone.THRUST;
        }

        if (zone != sentZone && ClientPlayNetworking.canSend(ModNetworking.ZONE)) {
            PacketByteBuf buf = PacketByteBufs.create();
            buf.writeByte(zone.ordinal());
            ClientPlayNetworking.send(ModNetworking.ZONE, buf);
            sentZone = zone;
        }
    }

    private static void renderHud(DrawContext ctx, float tickDelta) {
        MinecraftClient mc = MinecraftClient.getInstance();
        ClientPlayerEntity p = mc.player;
        if (p == null || mc.options.hudHidden) return;
        if (!(p.getMainHandStack().getItem() instanceof MedievalSwordItem)) return;

        int w = ctx.getScaledWindowWidth();
        int h = ctx.getScaledWindowHeight();

        // stamina bar above the health row
        int bw = 80, x = w / 2 - bw / 2, y = h - 58;
        ctx.fill(x - 1, y - 1, x + bw + 1, y + 5, 0xAA000000);
        int filled = (int) (bw * MathHelper.clamp(stamina / 100f, 0f, 1f));
        int color = stamina > 50 ? 0xFF5BC236 : stamina > 20 ? 0xFFE0B72E : 0xFFD03A2E;
        ctx.fill(x, y, x + filled, y + 4, color);

        // zone widget to the right of the crosshair
        int cx = w / 2 + 36, cy = h / 2;
        zoneBox(ctx, cx, cy - 11, Zone.UP);
        zoneBox(ctx, cx - 11, cy, Zone.LEFT);
        zoneBox(ctx, cx + 11, cy, Zone.RIGHT);
        zoneBox(ctx, cx, cy + 11, Zone.DOWN);
        zoneBox(ctx, cx, cy, Zone.THRUST);
    }

    private static void zoneBox(DrawContext ctx, int cx, int cy, Zone z) {
        int c = z == zone ? 0xFFFFD34E : 0x66FFFFFF;
        ctx.fill(cx - 4, cy - 4, cx + 4, cy + 4, c);
    }
}
