package com.medievalcombat.item;

import com.medievalcombat.combat.Zone;
import java.util.List;
import net.minecraft.client.item.TooltipContext;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.SwordItem;
import net.minecraft.item.ToolMaterial;
import net.minecraft.item.Item;
import net.minecraft.text.Text;
import net.minecraft.util.Hand;
import net.minecraft.util.TypedActionResult;
import net.minecraft.util.UseAction;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;

/**
 * A medieval sword. Right click = hold guard. Left click = attack in the current zone
 * (the zone follows the mouse movement, see the client class). All damage is calculated
 * in CombatManager using the numbers stored here.
 */
public class MedievalSwordItem extends SwordItem {
    private final float slashDamage;
    private final float thrustDamage;
    private final int cooldownTicks;
    private final float staminaCost;
    private final float blockCostMult;
    private final float thrustPierce;

    public MedievalSwordItem(ToolMaterial material, float slashDamage, float thrustDamage, int cooldownTicks,
                             float staminaCost, float blockCostMult, float thrustPierce, Item.Settings settings) {
        // vanilla attributes are used only for the tooltip / attack speed display
        super(material, Math.round(slashDamage) - 1, 20.0f / cooldownTicks - 4.0f, settings);
        this.slashDamage = slashDamage;
        this.thrustDamage = thrustDamage;
        this.cooldownTicks = cooldownTicks;
        this.staminaCost = staminaCost;
        this.blockCostMult = blockCostMult;
        this.thrustPierce = thrustPierce;
    }

    public float baseDamage(Zone zone) {
        return zone == Zone.THRUST ? thrustDamage : slashDamage * zone.slashMult;
    }

    public int getCooldownTicks() { return cooldownTicks; }
    public float getStaminaCost() { return staminaCost; }
    public float getBlockCostMult() { return blockCostMult; }
    /** Fraction of the target's armor resistance ignored by a thrust. */
    public float getThrustPierce() { return thrustPierce; }

    @Override
    public TypedActionResult<ItemStack> use(World world, PlayerEntity user, Hand hand) {
        ItemStack stack = user.getStackInHand(hand);
        user.setCurrentHand(hand);
        return TypedActionResult.consume(stack);
    }

    @Override
    public UseAction getUseAction(ItemStack stack) {
        return UseAction.BLOCK;
    }

    @Override
    public int getMaxUseTime(ItemStack stack) {
        return 72000;
    }

    @Override
    public void appendTooltip(ItemStack stack, @Nullable World world, List<Text> tooltip, TooltipContext context) {
        tooltip.add(Text.translatable("tooltip.medievalcombat.damage", fmt(slashDamage), fmt(thrustDamage)));
        tooltip.add(Text.translatable("tooltip.medievalcombat.stamina", fmt(staminaCost)));
    }

    private static String fmt(float f) {
        return f == (int) f ? Integer.toString((int) f) : String.format("%.1f", f);
    }
}
