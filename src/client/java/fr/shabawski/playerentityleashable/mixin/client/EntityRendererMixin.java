package fr.shabawski.playerentityleashable.mixin.client;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.render.LightmapTextureManager;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.entity.EntityRenderDispatcher;
import net.minecraft.client.render.entity.EntityRenderer;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityAttachmentType;
import net.minecraft.entity.Leashable;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.LightType;
import org.joml.Matrix4f;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(EntityRenderer.class)
public class EntityRendererMixin<T extends Entity> {

    @Final
    @Mutable
    @Shadow
    protected EntityRenderDispatcher dispatcher;
    @Final
    @Mutable
    @Shadow
    private TextRenderer textRenderer;

    @Inject(at = @At("HEAD"), method = "render", cancellable = true)
    public void render(T entity, float yaw, float tickDelta, MatrixStack matrices, VertexConsumerProvider vertexConsumers, int light, CallbackInfo ci) {

        if (this.hasLabel(entity)) {
            this.renderLabelIfPresent(entity, entity.getDisplayName(), matrices, vertexConsumers, light, tickDelta);
        }

        if (entity instanceof PlayerEntity && entity instanceof Leashable leashable) {
            Entity leashHolder = leashable.getLeashHolder();
            if (leashHolder != null) {
                renderCustomLeash(entity, tickDelta, matrices, vertexConsumers, leashHolder);
                ci.cancel();
            }
        }
    }


    private <E extends Entity> void renderCustomLeash(T entity, float tickDelta, MatrixStack matrices, VertexConsumerProvider vertexConsumers, E leashHolder) {
        matrices.push();
        Vec3d vec3d = leashHolder.getLeashPos(tickDelta);
        double d = (double)(entity.lerpYaw(tickDelta) * 0.017453292F) + 1.5707963267948966;
        Vec3d vec3d2 = entity.getLeashOffset(tickDelta);

        // Adjusting the offset to move the leash attachment point to the chest
        double chestYOffset = 0.5; // Adjust this value as necessary for proper chest height
        vec3d2 = vec3d2.add(0, -chestYOffset, 0); // Move downwards by chestYOffset

        double e = Math.cos(d) * vec3d2.z + Math.sin(d) * vec3d2.x;
        double f = Math.sin(d) * vec3d2.z - Math.cos(d) * vec3d2.x;
        double g = MathHelper.lerp((double)tickDelta, entity.prevX, entity.getX()) + e;
        double h = MathHelper.lerp((double)tickDelta, entity.prevY, entity.getY()) + vec3d2.y;
        double i = MathHelper.lerp((double)tickDelta, entity.prevZ, entity.getZ()) + f;
        matrices.translate(e, vec3d2.y, f);

        float j = (float)(vec3d.x - g);
        float k = (float)(vec3d.y - h);
        float l = (float)(vec3d.z - i);
        float m = 0.025F;
        VertexConsumer vertexConsumer = vertexConsumers.getBuffer(RenderLayer.getLeash());
        Matrix4f matrix4f = matrices.peek().getPositionMatrix();
        float n = MathHelper.inverseSqrt(j * j + l * l) * 0.025F / 2.0F;
        float o = l * n;
        float p = j * n;

        BlockPos blockPos = BlockPos.ofFloored(entity.getCameraPosVec(tickDelta));
        BlockPos blockPos2 = BlockPos.ofFloored(leashHolder.getCameraPosVec(tickDelta));
        int q = entity.isOnFire() ? 15 : entity.getWorld().getLightLevel(LightType.BLOCK, blockPos);
        int r = leashHolder.isOnFire() ? 15 : entity.getWorld().getLightLevel(LightType.BLOCK, blockPos2);
        int s = entity.getWorld().getLightLevel(LightType.SKY, blockPos);
        int t = entity.getWorld().getLightLevel(LightType.SKY, blockPos2);

        int u;
        for (u = 0; u <= 24; ++u) {
            renderCustomLeashSegment(vertexConsumer, matrix4f, j, k, l, q, r, s, t, 0.025F, 0.025F, o, p, u, false);
        }

        for (u = 24; u >= 0; --u) {
            renderCustomLeashSegment(vertexConsumer, matrix4f, j, k, l, q, r, s, t, 0.025F, 0.0F, o, p, u, true);
        }

        matrices.pop();
    }

    private void renderCustomLeashSegment(VertexConsumer vertexConsumer, Matrix4f matrix, float leashedEntityX, float leashedEntityY, float leashedEntityZ, int leashedEntityBlockLight, int leashHolderBlockLight, int leashedEntitySkyLight, int leashHolderSkyLight, float f, float g, float h, float i, int segmentIndex, boolean isLeashKnot) {
        float j = (float)segmentIndex / 24.0F;
        int k = (int)MathHelper.lerp(j, (float)leashedEntityBlockLight, (float)leashHolderBlockLight);
        int l = (int)MathHelper.lerp(j, (float)leashedEntitySkyLight, (float)leashHolderSkyLight);
        int m = LightmapTextureManager.pack(k, l);
        float n = segmentIndex % 2 == (isLeashKnot ? 1 : 0) ? 0.7F : 1.0F;
        float o = 0.5F * n;
        float p = 0.4F * n;
        float q = 0.3F * n;
        float r = leashedEntityX * j;
        float s = leashedEntityY > 0.0F ? leashedEntityY * j * j : leashedEntityY - leashedEntityY * (1.0F - j) * (1.0F - j);
        float t = leashedEntityZ * j;
        vertexConsumer.vertex(matrix, r - h, s + g, t + i).color(o, p, q, 1.0F).light(m);
        vertexConsumer.vertex(matrix, r + h, s + f - g, t - i).color(o, p, q, 1.0F).light(m);
    }

    public boolean hasLabel(T entity) {
        return entity.shouldRenderName() || entity.hasCustomName() && entity == this.dispatcher.targetedEntity;
    }

    public void renderLabelIfPresent(T entity, Text text, MatrixStack matrices, VertexConsumerProvider vertexConsumers, int light, float tickDelta) {
        double d = this.dispatcher.getSquaredDistanceToCamera(entity);
        if (!(d > 4096.0)) {
            Vec3d vec3d = entity.getAttachments().getPointNullable(EntityAttachmentType.NAME_TAG, 0, entity.getYaw(tickDelta));
            if (vec3d != null) {
                boolean bl = !entity.isSneaky();
                int i = "deadmau5".equals(text.getString()) ? -10 : 0;
                matrices.push();
                matrices.translate(vec3d.x, vec3d.y + 0.5, vec3d.z);
                matrices.multiply(this.dispatcher.getRotation());
                matrices.scale(0.025F, -0.025F, 0.025F);
                Matrix4f matrix4f = matrices.peek().getPositionMatrix();
                float f = MinecraftClient.getInstance().options.getTextBackgroundOpacity(0.25F);
                int j = (int)(f * 255.0F) << 24;
                TextRenderer textRenderer = this.getTextRenderer();
                float g = (float)(-textRenderer.getWidth(text) / 2);
                textRenderer.draw(text, g, (float)i, 553648127, false, matrix4f, vertexConsumers, bl ? TextRenderer.TextLayerType.SEE_THROUGH : TextRenderer.TextLayerType.NORMAL, j, light);
                if (bl) {
                    textRenderer.draw(text, g, (float)i, -1, false, matrix4f, vertexConsumers, TextRenderer.TextLayerType.NORMAL, 0, light);
                }

                matrices.pop();
            }
        }
    }

    public TextRenderer getTextRenderer() {
        return this.textRenderer;
    }
}
