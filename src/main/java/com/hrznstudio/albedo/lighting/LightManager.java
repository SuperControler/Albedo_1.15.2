package com.hrznstudio.albedo.lighting;

import com.hrznstudio.albedo.Albedo;
import com.hrznstudio.albedo.ConfigManager;
import com.hrznstudio.albedo.event.GatherLightsEvent;
import com.hrznstudio.albedo.util.ShaderManager;
import com.mojang.blaze3d.matrix.MatrixStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.ActiveRenderInfo;
import net.minecraft.client.renderer.Matrix4f;
import net.minecraft.client.renderer.Vector3f;
import net.minecraft.client.renderer.culling.ClippingHelperImpl;
import net.minecraft.entity.Entity;
import net.minecraft.entity.item.ItemEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.potion.Effects;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import net.minecraftforge.client.ForgeHooksClient;
import net.minecraftforge.client.event.EntityViewRenderEvent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.common.util.LazyOptional;

import java.util.ArrayList;
import java.util.Comparator;

public class LightManager {
    public static Vec3d cameraPos;
    public static ClippingHelperImpl camera;
    public static ArrayList<Light> lights = new ArrayList<Light>();
    public static DistComparator distComparator = new DistComparator();

    public static void uploadLights() {
        ShaderManager shader = ShaderManager.getCurrentShader();
        shader.setUniform("lightCount", lights.size());
        for (int i = 0; i < Math.min(ConfigManager.maxLights.get(), lights.size()); i++) {
            if (i < lights.size()) {
                Light l = lights.get(i);
                shader.setUniform("lights[" + i + "].position", l.x, l.y, l.z);
                shader.setUniform("lights[" + i + "].color", l.r, l.g, l.b, l.a);
                shader.setUniform("lights[" + i + "].heading", l.rx, l.ry, l.rz);
                shader.setUniform("lights[" + i + "].angle", l.angle);
            } else {
                //shader.setUniform("lights[" + i + "].position", 0, 0, 0);
                //shader.setUniform("lights[" + i + "].color", 0, 0, 0, 0);
                //shader.setUniform("lights[" + i + "].heading", 0, 0, 0);
                //shader.setUniform("lights[" + i + "].angle", 0);
            }
        }
    }

    private static Vec3d interpolate(Entity entity, float partialTicks) {
        return new Vec3d(
                entity.lastTickPosX + (entity.getPosX() - entity.lastTickPosX) * partialTicks,
                entity.lastTickPosY + (entity.getPosY() - entity.lastTickPosY) * partialTicks,
                entity.lastTickPosZ + (entity.getPosZ() - entity.lastTickPosZ) * partialTicks
        );
    }

    public static void update(World world) {
        Minecraft mc = Minecraft.getInstance();
        Entity cameraEntity = mc.getRenderViewEntity();
        if (cameraEntity != null) {
            cameraPos = interpolate(cameraEntity, mc.getRenderPartialTicks());
            float partialTicks = mc.getRenderPartialTicks();
            MatrixStack matrixstack = new MatrixStack();
            matrixstack.getLast().getMatrix().mul(mc.gameRenderer.getProjectionMatrix(mc.gameRenderer.getActiveRenderInfo(), partialTicks, true));
            assert mc.player != null;
            float f = MathHelper.lerp(partialTicks, mc.player.prevTimeInPortal, mc.player.timeInPortal);
            if (f > 0.0F) {
                int i = 20;
                if (mc.player.isPotionActive(Effects.NAUSEA)) {
                    i = 7;
                }
                float f1 = 5.0F / (f * f + 5.0F) - f * 0.04F;
                f1 = f1 * f1;
                Vector3f vector3f = new Vector3f(0.0F, MathHelper.SQRT_2 / 2.0F, MathHelper.SQRT_2 / 2.0F);
                matrixstack.rotate(vector3f.rotationDegrees(((float)mc.player.ticksExisted + partialTicks) * (float)i));
                matrixstack.scale(1.0F / f1, 1.0F, 1.0F);
                float f2 = -((float)mc.player.ticksExisted + partialTicks) * (float)i;
                matrixstack.rotate(vector3f.rotationDegrees(f2));
            }
            Matrix4f matrix4f = matrixstack.getLast().getMatrix();

            matrixstack = new MatrixStack();
            EntityViewRenderEvent.CameraSetup cameraSetup = ForgeHooksClient.onCameraSetup(mc.gameRenderer, mc.gameRenderer.getActiveRenderInfo(), partialTicks);
            ActiveRenderInfo activeRenderInfo = mc.gameRenderer.getActiveRenderInfo();
            activeRenderInfo.setAnglesInternal(cameraSetup.getYaw(), cameraSetup.getPitch());

            matrixstack.rotate(Vector3f.ZP.rotationDegrees(cameraSetup.getRoll()));
            matrixstack.rotate(Vector3f.XP.rotationDegrees(activeRenderInfo.getPitch()));
            matrixstack.rotate(Vector3f.YP.rotationDegrees(activeRenderInfo.getYaw() + 180.0F));

            camera = new ClippingHelperImpl(matrixstack.getLast().getMatrix(), matrix4f);
            camera.setCameraPosition(cameraPos.x, cameraPos.y, cameraPos.z);
        } else {
            if (cameraPos == null) {
                cameraPos = new Vec3d(0, 0, 0);
            }
            camera = null;
            return;
        }

        GatherLightsEvent event = new GatherLightsEvent(lights, ConfigManager.maxDistance.get(), cameraPos, camera);
        MinecraftForge.EVENT_BUS.post(event);

        int maxDist = ConfigManager.maxDistance.get();

        for (Entity e : world.getEntitiesWithinAABB(Entity.class, new AxisAlignedBB(
                cameraPos.x - maxDist,
                cameraPos.y - maxDist,
                cameraPos.z - maxDist,
                cameraPos.x + maxDist,
                cameraPos.y + maxDist,
                cameraPos.z + maxDist
        ))) {
            if (e instanceof ItemEntity) {
                LazyOptional<ILightProvider> provider = ((ItemEntity) e).getItem().getCapability(Albedo.LIGHT_PROVIDER_CAPABILITY);
                provider.ifPresent(p -> p.gatherLights(event, e));
            }
            LazyOptional<ILightProvider> provider = e.getCapability(Albedo.LIGHT_PROVIDER_CAPABILITY);
            provider.ifPresent(p -> p.gatherLights(event, e));
            for (ItemStack itemStack : e.getHeldEquipment()) {
                provider = itemStack.getCapability(Albedo.LIGHT_PROVIDER_CAPABILITY);
                provider.ifPresent(p -> p.gatherLights(event, e));
            }
            for (ItemStack itemStack : e.getArmorInventoryList()) {
                provider = itemStack.getCapability(Albedo.LIGHT_PROVIDER_CAPABILITY);
                provider.ifPresent(p -> p.gatherLights(event, e));
            }
        }

        for (TileEntity t : world.loadedTileEntityList) {
            LazyOptional<ILightProvider> provider = t.getCapability(Albedo.LIGHT_PROVIDER_CAPABILITY);
            provider.ifPresent(p -> p.gatherLights(event, null));
        }

        lights.sort(distComparator);
    }

    public static void clear() {
        lights.clear();
    }

    public static class DistComparator implements Comparator<Light> {
        @Override
        public int compare(Light a, Light b) {
            double dist1 = cameraPos.squareDistanceTo(a.x, a.y, a.z);
            double dist2 = cameraPos.squareDistanceTo(b.x, b.y, b.z);
            return Double.compare(dist1, dist2);
        }
    }
}