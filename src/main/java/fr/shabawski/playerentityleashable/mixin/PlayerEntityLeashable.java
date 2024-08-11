package fr.shabawski.playerentityleashable.mixin;

import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.Leashable;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(PlayerEntity.class)
public abstract class PlayerEntityLeashable extends LivingEntity implements Leashable {

    @Nullable
    private Leashable.LeashData leashData;

    protected PlayerEntityLeashable(EntityType<? extends LivingEntity> entityType, World world) {
        super(entityType, world);
    }

    @Nullable
    @Override
    public Leashable.LeashData getLeashData() {
        return this.leashData;
    }

    @Override
    public void setLeashData(@Nullable Leashable.LeashData leashData) {
        this.leashData = leashData;
    }

    @Inject(at = @At(value = "INVOKE", target = "Lnet/minecraft/nbt/NbtHelper;putDataVersion(Lnet/minecraft/nbt/NbtCompound;)Lnet/minecraft/nbt/NbtCompound;"), method = "writeCustomDataToNbt")
    public void writeCustomDataToNbt(NbtCompound nbt, CallbackInfo ci) {
        this.writeLeashDataToNbt(nbt, this.leashData);
    }

    @Inject(at = @At(value = "INVOKE", target = "Lnet/minecraft/entity/player/PlayerEntity;setUuid(Ljava/util/UUID;)V"), method = "readCustomDataFromNbt")
    public void readCustomDataFromNbt(NbtCompound nbt, CallbackInfo ci) {
        this.leashData = this.readLeashDataFromNbt(nbt);
    }

    @Inject(method = "tickMovement", at = @At("HEAD"), cancellable = true)
    public void onTickMovement(CallbackInfo ci) {
        if (this.leashData != null && this.getLeashHolder() != null) {
            Entity leasher = this.getLeashHolder();

            // Calculate distance to leasher
            if (this == leasher) {
                return;
            }

            double distance = this.distanceTo(leasher);

            System.out.println("Leash holder: " + leasher.getName().getString());
            System.out.println("Distance to leasher: " + distance);

            double maxLeashDistance = 5.0; // Maximum allowed distance

            if (distance > maxLeashDistance) {
                // Calculate movement towards leasher
                double deltaX = leasher.getX() - this.getX();
                double deltaY = leasher.getY() - this.getY();
                double deltaZ = leasher.getZ() - this.getZ();
                double length = Math.sqrt(deltaX * deltaX + deltaY * deltaY + deltaZ * deltaZ);

                if (length > 0) {
                    // Normalize the movement vector and move the player
                    double moveX = deltaX / length * (distance - maxLeashDistance);
                    double moveY = deltaY / length * (distance - maxLeashDistance);
                    double moveZ = deltaZ / length * (distance - maxLeashDistance);

                    // Debugging move values
                    System.out.println("Moving player by: " + moveX + ", " + moveY + ", " + moveZ);

                    this.setPos(this.getX() + moveX, this.getY() + moveY, this.getZ() + moveZ);

                    // Set velocity to zero to prevent further movement
                    this.setVelocity(0, 0, 0);

                    // Cancel the normal movement
                    ci.cancel();
                }
            }
        }
    }

    @Inject(at = @At("HEAD"), method = "tick", cancellable = true)
    public void onTick(CallbackInfo ci) {
        if (this.leashData != null && this.getLeashHolder() != null) {
            Entity leasher = this.getLeashHolder();

            // Calculate distance to leasher
            double distance = this.distanceTo(leasher);
            // System.out.println("Leash holder: " + leasher.getName().getString());
            // System.out.println("Distance to leasher: " + distance);

            double maxLeashDistance = 5.0; // Maximum allowed distance

            if (distance > maxLeashDistance) {
                // Calculate movement towards leasher
                double deltaX = leasher.getX() - this.getX();
                double deltaY = leasher.getY() - this.getY();
                double deltaZ = leasher.getZ() - this.getZ();
                double length = Math.sqrt(deltaX * deltaX + deltaY * deltaY + deltaZ * deltaZ);

                if (length > 0) {
                    // Normalize the movement vector and move the player
                    double moveX = deltaX / length * (distance - maxLeashDistance);
                    double moveY = deltaY / length * (distance - maxLeashDistance);
                    double moveZ = deltaZ / length * (distance - maxLeashDistance);

                    // Debugging move values
                    // System.out.println("Moving player by: " + moveX + ", " + moveY + ", " + moveZ);

                    // Set the new position
                    this.setPosition(this.getX() + moveX, this.getY() + moveY, this.getZ() + moveZ);

                    if (!this.getWorld().isClient) {
                        ServerPlayerEntity serverPlayer = (ServerPlayerEntity) (Object) this;
                        serverPlayer.networkHandler.requestTeleport(this.getX(), this.getY(), this.getZ(), this.getYaw(), this.getPitch());
                    }
                    // Set velocity to zero to prevent further movement
                    this.setVelocity(0, 0, 0);

                    // Cancel further tick processing for movement
                    ci.cancel();
                }
            } else {
                // Prevent player from moving independently
                this.setVelocity(0, 0, 0);
                ci.cancel();
            }
        }
    }
}
