package net.mehvahdjukaar.amendments.client.renderers;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.mehvahdjukaar.amendments.AmendmentsClient;
import net.mehvahdjukaar.amendments.common.IBetterJukebox;
import net.mehvahdjukaar.amendments.configs.ClientConfigs;
import net.mehvahdjukaar.amendments.integration.CompatObjects;
import net.mehvahdjukaar.amendments.integration.EtchedCompat;
import net.mehvahdjukaar.moonlight.api.client.util.RotHlpr;
import net.mehvahdjukaar.moonlight.api.client.util.VertexUtil;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.resources.model.Material;
import net.minecraft.world.level.block.JukeboxBlock;
import net.minecraft.world.level.block.entity.JukeboxBlockEntity;
import net.minecraft.world.phys.Vec3;

public class JukeboxTileRenderer implements BlockEntityRenderer<JukeboxBlockEntity> {

    public JukeboxTileRenderer(BlockEntityRendererProvider.Context context) {
    }

    @Override
    public void render(JukeboxBlockEntity blockEntity, float partialTick, PoseStack poseStack, MultiBufferSource bufferSource, int packedLight, int packedOverlay) {
        var item = blockEntity.getTheItem();

        if (!item.isEmpty() && blockEntity.getBlockState().getValue(JukeboxBlock.HAS_RECORD)) {
            if (ClientConfigs.JUKEBOX_SPIN.get()) {
                Vec3 pivot = ClientConfigs.JUKEBOX_SPIN_PIVOT.get();
                poseStack.translate(pivot.x / 16, pivot.y / 16, pivot.z / 16);
                poseStack.mulPose(Axis.YP.rotationDegrees(((IBetterJukebox) blockEntity).amendments$getRotation(partialTick)));
                poseStack.translate((8 - pivot.x) / 16, (15.25 - pivot.y) / 16, (8 - pivot.z) / 16);
            } else {
                if (ClientConfigs.JUKEBOX_MODEL.get()) {
                    // poseStack.translate(0, -0.5 / 16f, 0);
                }
                poseStack.translate(0.5, 15.25 / 16f, 0.5);
            }
            poseStack.mulPose(RotHlpr.X90);


            Material material = AmendmentsClient.getRecordMaterial(item.getItem());
            VertexConsumer builder;
            int upLight = LevelRenderer.getLightColor(blockEntity.getLevel(), blockEntity.getBlockPos().above(1));
            int lu = upLight & '\uffff';
            int lv = upLight >> 16 & '\uffff';


            // etched shit
            if (item.getItem() == CompatObjects.ETCHED_DISC.get()) {
                EtchedCompat.drawDisc(item, poseStack, bufferSource, lu, lv);
            } else {
                builder = material.buffer(bufferSource, RenderType::entityCutout);
                VertexUtil.addQuad(builder, poseStack, -0.5f, -0.5f, 0.5f, 0.5f, lu, lv);
            }
        }
    }


}
