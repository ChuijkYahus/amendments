package net.mehvahdjukaar.amendments.client.renderers;


import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.datafixers.util.Pair;
import net.mehvahdjukaar.amendments.common.block.CeilingBannerBlock;
import net.mehvahdjukaar.amendments.common.tile.CeilingBannerBlockTile;
import net.mehvahdjukaar.moonlight.api.client.util.RotHlpr;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BannerRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.resources.model.ModelBakery;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.util.Mth;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.level.block.entity.BannerPattern;
import net.minecraft.world.level.block.state.BlockState;

import java.util.List;

public class CeilingBannerBlockTileRenderer implements BlockEntityRenderer<CeilingBannerBlockTile> {
    private final ModelPart flag;
    private final ModelPart bar;

    public CeilingBannerBlockTileRenderer(BlockEntityRendererProvider.Context context) {
        ModelPart modelpart = context.bakeLayer(ModelLayers.BANNER);
        this.flag = modelpart.getChild("flag");
        this.bar = modelpart.getChild("bar");
    }

    @Override
    public void render(CeilingBannerBlockTile tile, float partialTick, PoseStack poseStack, MultiBufferSource multiBufferSource, int light, int pPackedOverlay) {
        List<Pair<Holder<BannerPattern>, DyeColor>> list = tile.getPatterns();
        if (list != null) {

            poseStack.pushPose();
            long i;

            i = tile.getLevel().getGameTime();
            BlockState blockstate = tile.getBlockState();

            if (blockstate.getValue(CeilingBannerBlock.ATTACHED)) {
                poseStack.translate(0, 0.625, 0);
            }
            poseStack.translate(0.5D, -0.3125 - 0.0208333333333, 0.5D); //1/32 * 2/3

            poseStack.mulPose(RotHlpr.rot(blockstate.getValue(CeilingBannerBlock.FACING)));

            poseStack.pushPose();
            poseStack.scale(-0.6666667F, -0.6666667F, 0.6666667F);
            VertexConsumer buffer = ModelBakery.BANNER_BASE.buffer(multiBufferSource, RenderType::entitySolid);

            this.bar.render(poseStack, buffer, light, pPackedOverlay);
            BlockPos blockpos = tile.getBlockPos();
            float f2 = ((float) Math.floorMod((long) (blockpos.getX() * 7 + blockpos.getY() * 9 + blockpos.getZ() * 13) + i, 100L) + partialTick) / 100.0F;
            this.flag.xRot = (-0.0125F + 0.01F * Mth.cos(((float) Math.PI * 2F) * f2)) * (float) Math.PI;
            this.flag.y = -32.0F;
            BannerRenderer.renderPatterns(poseStack, multiBufferSource, light, pPackedOverlay, this.flag,
                    ModelBakery.BANNER_BASE,  true, tile.getBaseColor(), list);
            poseStack.popPose();
            poseStack.popPose();
        }
    }

}

