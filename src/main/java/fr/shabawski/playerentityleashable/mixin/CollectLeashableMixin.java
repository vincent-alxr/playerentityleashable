package fr.shabawski.playerentityleashable.mixin;

import net.minecraft.entity.Entity;
import net.minecraft.entity.Leashable;
import net.minecraft.entity.player.HungerManager;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.LeadItem;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.List;
import java.util.function.Predicate;
import java.util.stream.Collectors;

@Mixin(LeadItem.class)
public class CollectLeashableMixin {
    @Inject(at = @At("HEAD"), method = "collectLeashablesAround", cancellable = true)
    private static void collectLeashablesAroundMixin(World world, BlockPos pos, Predicate<Leashable> predicate, CallbackInfoReturnable<List<Leashable>> cir) {
        double radius = 7.0;
        int x = pos.getX();
        int y = pos.getY();
        int z = pos.getZ();
        Box box = new Box(x - radius, y - radius, z - radius, x + radius, y + radius, z + radius);

        List<Leashable> leashables = world.getEntitiesByClass(Entity.class, box, entity -> {
                    if (entity instanceof PlayerEntity player) {
                        HungerManager hungerManager = player.getHungerManager();
                        int foodLevel = hungerManager.getFoodLevel();
                        if (foodLevel < 6) { // Moins de 30% de la barre de nourriture
                            return false;
                        }
                    }

                    return entity instanceof Leashable && predicate.test((Leashable) entity);
                }).stream()
                .map(entity -> (Leashable) entity)
                .collect(Collectors.toList());

        cir.setReturnValue(leashables);
    }
}
