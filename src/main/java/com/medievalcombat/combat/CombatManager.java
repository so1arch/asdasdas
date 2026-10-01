package com.medievalcombat.combat;

import com.medievalcombat.item.MedievalArmorMaterial;
import com.medievalcombat.item.MedievalSwordItem;
import com.medievalcombat.net.ModNetworking;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.event.player.AttackEntityCallback;
import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ArmorItem;
import net.minecraft.item.ItemStack;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvent;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;

/**
 * KCD-inspired combat, server side:
 *  - stamina (attacks and blocks cost it, armor weight slows regeneration)
 *  - 5 attack zones; the defender's guard zone must match the attack zone to block well
 *  - right after raising the guard (first PARRY_WINDOW ticks) a matching zone = PARRY
 *  - a parry staggers the attacker and gives the defender a "master strike" window
 *  - slashes are resisted by armor more than thrusts; thrusts pierce armor
 */
public final class CombatManager {
    public static final float MAX_STAMINA = 100f;
    private static final int PARRY_WINDOW = 12;       // ticks after raising the guard
    private static final int MASTER_STRIKE_TICKS = 30;

    /** Set while the mod itself deals damage so that our damage hook does not recurse. */
    private static final ThreadLocal<Boolean> INTERNAL = ThreadLocal.withInitial(() -> false);
    private static final Map<UUID, State> STATES = new HashMap<>();

    private CombatManager() {}

    public static final class State {
        float stamina = MAX_STAMINA;
        Zone zone = Zone.THRUST;
        int regenResumeTick = 0;
        int lastAttackTick = -100;
        int masterStrikeUntil = -1;
        float lastSent = -1f;
    }

    public static void register() {
        AttackEntityCallback.EVENT.register(CombatManager::onAttack);
        ServerLivingEntityEvents.ALLOW_DAMAGE.register(CombatManager::onAllowDamage);
        ServerTickEvents.END_SERVER_TICK.register(CombatManager::tick);
        ServerPlayConnectionEvents.DISCONNECT.register((handler, server) -> STATES.remove(handler.player.getUuid()));
    }

    private static State get(ServerPlayerEntity p) {
        return STATES.computeIfAbsent(p.getUuid(), u -> new State());
    }

    public static void setZone(ServerPlayerEntity p, int id) {
        get(p).zone = Zone.byId(id);
    }

    private static int now(ServerPlayerEntity p) {
        return p.getServer().getTicks();
    }

    /** Spends stamina. Returns true if there was enough. */
    private static boolean spend(ServerPlayerEntity p, float amount) {
        State s = get(p);
        boolean enough = s.stamina >= amount;
        s.stamina -= amount;
        int t = now(p);
        s.regenResumeTick = t + 25;
        if (s.stamina < 0f) {
            s.stamina = 0f;
            s.regenResumeTick = t + 50;
        }
        return enough;
    }

    private static boolean isBlocking(LivingEntity e) {
        return e instanceof PlayerEntity && e.isUsingItem() && e.getActiveItem().getItem() instanceof MedievalSwordItem;
    }

    private static boolean facing(LivingEntity defender, LivingEntity attacker) {
        Vec3d look = defender.getRotationVec(1.0f);
        Vec3d to = attacker.getPos().subtract(defender.getPos()).normalize();
        return look.dotProduct(to) > 0.2;
    }

    private static void sound(Entity at, SoundEvent ev, float vol, float pitch) {
        at.getWorld().playSound(null, at.getX(), at.getY(), at.getZ(), ev, SoundCategory.PLAYERS, vol, pitch);
    }

    private static float pieceFactor(ArmorItem.Type t) {
        return switch (t) {
            case HELMET -> 0.15f;
            case CHESTPLATE -> 0.40f;
            case LEGGINGS -> 0.30f;
            case BOOTS -> 0.15f;
        };
    }

    private static float armorWeight(LivingEntity e) {
        float w = 0;
        for (ItemStack s : e.getArmorItems()) {
            if (s.getItem() instanceof ArmorItem a && a.getMaterial() instanceof MedievalArmorMaterial m) {
                w += m.weight * pieceFactor(a.getType());
            }
        }
        return w;
    }

    // ------------------------------------------------------------------ tick

    private static void tick(MinecraftServer server) {
        int t = server.getTicks();
        for (ServerPlayerEntity p : server.getPlayerManager().getPlayerList()) {
            State s = get(p);
            if (t >= s.regenResumeTick && s.stamina < MAX_STAMINA) {
                float rate = isBlocking(p) ? 0.15f : 0.55f;
                float armorMult = Math.max(0.4f, 1.0f - armorWeight(p) * 0.12f);
                s.stamina = Math.min(MAX_STAMINA, s.stamina + rate * armorMult);
            }
            if (s.stamina <= 0f && isBlocking(p)) {
                p.stopUsingItem(); // exhausted: cannot hold the guard
            }
            if (Math.abs(s.stamina - s.lastSent) >= 1f || (s.stamina >= MAX_STAMINA && s.lastSent < MAX_STAMINA)) {
                s.lastSent = s.stamina;
                PacketByteBuf buf = PacketByteBufs.create();
                buf.writeFloat(s.stamina);
                ServerPlayNetworking.send(p, ModNetworking.STAMINA, buf);
            }
        }
    }

    // ---------------------------------------------------------- player attack

    private static ActionResult onAttack(PlayerEntity player, World world, Hand hand, Entity entity, HitResult hit) {
        if (world.isClient || player.isSpectator()) return ActionResult.PASS; // let the packet reach the server
        if (!(player instanceof ServerPlayerEntity attacker)) return ActionResult.PASS;
        ItemStack stack = attacker.getStackInHand(hand);
        if (!(stack.getItem() instanceof MedievalSwordItem sword)) return ActionResult.PASS;
        if (!(entity instanceof LivingEntity target)) return ActionResult.PASS;

        State as = get(attacker);
        int t = now(attacker);
        if (t - as.lastAttackTick < sword.getCooldownTicks()) return ActionResult.SUCCESS; // swinging too fast
        as.lastAttackTick = t;

        Zone zone = as.zone;
        boolean exhausted = as.stamina < sword.getStaminaCost();
        spend(attacker, sword.getStaminaCost());

        float dmg = sword.baseDamage(zone);
        if (exhausted) dmg *= 0.5f;

        boolean master = t <= as.masterStrikeUntil;
        if (master) {
            dmg *= 2.2f;
            as.masterStrikeUntil = -1;
            sound(attacker, SoundEvents.ENTITY_PLAYER_ATTACK_CRIT, 1.0f, 0.8f);
        }

        dmg = applyArmorTypes(target, zone, sword, dmg);

        // defender holds the guard (player vs player)
        if (!master && target instanceof ServerPlayerEntity def && isBlocking(def) && facing(def, attacker)) {
            State ds = get(def);
            MedievalSwordItem defSword = (MedievalSwordItem) def.getActiveItem().getItem();
            boolean zoneMatch = ds.zone == zone;
            boolean perfect = zoneMatch && def.getItemUseTime() <= PARRY_WINDOW;

            if (perfect) {
                sound(def, SoundEvents.BLOCK_ANVIL_PLACE, 0.6f, 2.0f);
                spend(attacker, 25f);
                attacker.addStatusEffect(new StatusEffectInstance(StatusEffects.SLOWNESS, 30, 2, false, false));
                as.lastAttackTick = t + 20; // stagger: cannot attack for a while
                ds.masterStrikeUntil = t + MASTER_STRIKE_TICKS;
                def.sendMessage(Text.translatable("message.medievalcombat.parry"), true);
                attacker.sendMessage(Text.translatable("message.medievalcombat.parried"), true);
                damageTool(stack, attacker, hand);
                return ActionResult.SUCCESS;
            }

            float raw = dmg;
            float cost = raw * (zoneMatch ? 1.5f : 2.5f) * defSword.getBlockCostMult();
            dmg = zoneMatch ? raw * 0.10f : raw * 0.55f;
            boolean broken = ds.stamina < cost;
            spend(def, cost);
            sound(def, SoundEvents.ITEM_SHIELD_BLOCK, 1.0f, 1.0f);
            if (broken) {
                def.stopUsingItem();
                def.getItemCooldownManager().set(defSword, 40);
                def.addStatusEffect(new StatusEffectInstance(StatusEffects.SLOWNESS, 20, 2, false, false));
                def.sendMessage(Text.translatable("message.medievalcombat.guard_broken"), true);
                sound(def, SoundEvents.ITEM_SHIELD_BREAK, 1.0f, 1.0f);
                dmg = raw * 0.8f;
            }
        }

        deal(attacker, target, dmg);
        damageTool(stack, attacker, hand);
        return ActionResult.SUCCESS;
    }

    private static float applyArmorTypes(LivingEntity target, Zone zone, MedievalSwordItem sword, float dmg) {
        float slash = 0f, thrust = 0f;
        for (ItemStack s : target.getArmorItems()) {
            if (s.getItem() instanceof ArmorItem a && a.getMaterial() instanceof MedievalArmorMaterial m) {
                float f = pieceFactor(a.getType());
                slash += m.slashResist * f;
                thrust += m.thrustResist * f;
            }
        }
        float r = zone == Zone.THRUST ? thrust * (1f - sword.getThrustPierce()) : slash;
        return dmg * (1f - Math.min(r, 0.8f));
    }

    private static void deal(ServerPlayerEntity attacker, LivingEntity target, float dmg) {
        INTERNAL.set(true);
        try {
            target.damage(attacker.getDamageSources().playerAttack(attacker), dmg);
        } finally {
            INTERNAL.set(false);
        }
        if (target.isAlive()) {
            float yaw = attacker.getYaw() * 0.017453292f;
            target.takeKnockback(0.4, MathHelper.sin(yaw), -MathHelper.cos(yaw));
        }
        sound(attacker, SoundEvents.ENTITY_PLAYER_ATTACK_STRONG, 1.0f, 1.0f);
    }

    private static void damageTool(ItemStack stack, ServerPlayerEntity p, Hand hand) {
        stack.damage(1, p, pl -> pl.sendToolBreakStatus(hand));
    }

    // ------------------------------------------- mobs / others hitting a blocker

    private static boolean onAllowDamage(LivingEntity entity, DamageSource source, float amount) {
        if (INTERNAL.get()) return true;
        if (!(entity instanceof ServerPlayerEntity p) || !isBlocking(p)) return true;
        Entity direct = source.getSource();
        if (!(direct instanceof LivingEntity atk) || direct instanceof PlayerEntity || source.getAttacker() != direct) return true;
        if (!facing(p, atk)) return true;

        MedievalSwordItem sw = (MedievalSwordItem) p.getActiveItem().getItem();
        float cost = amount * 2.0f * sw.getBlockCostMult();
        boolean broken = get(p).stamina < cost;
        spend(p, cost);
        sound(p, SoundEvents.ITEM_SHIELD_BLOCK, 1.0f, 1.0f);
        float reduced = amount * 0.3f;
        if (broken) {
            p.stopUsingItem();
            p.getItemCooldownManager().set(sw, 40);
            p.sendMessage(Text.translatable("message.medievalcombat.guard_broken"), true);
            reduced = amount * 0.8f;
        }
        INTERNAL.set(true);
        try {
            p.damage(source, reduced);
        } finally {
            INTERNAL.set(false);
        }
        return false;
    }
}
