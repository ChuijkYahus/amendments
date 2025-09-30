package net.mehvahdjukaar.amendments.client;

import com.google.common.collect.ImmutableSet;
import com.google.gson.JsonElement;
import net.mehvahdjukaar.amendments.Amendments;
import net.mehvahdjukaar.amendments.client.renderers.LanternRendererExtension;
import net.mehvahdjukaar.amendments.common.block.WallLanternBlock;
import net.mehvahdjukaar.amendments.configs.ClientConfigs;
import net.mehvahdjukaar.moonlight.api.item.IThirdPersonSpecialItemRenderer;
import net.mehvahdjukaar.moonlight.api.platform.ClientHelper;
import net.mehvahdjukaar.moonlight.api.resources.RPUtils;
import net.mehvahdjukaar.moonlight.api.util.Utils;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.block.BlockModelShaper;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

//handles wall lanterns and jar special models stuff. reloaded by dynamic pack early
public class WallLanternModelsManager {

    private static final Map<Block, ResourceLocation> SPECIAL_MOUNT_TEXTURES = new IdentityHashMap<>();
    private static final Map<Block, ResourceLocation> SPECIAL_LANTERN_MODELS = new IdentityHashMap<>();

    //early reload so we can register these models
    public static void refreshModels(ResourceManager manager) {
        reloadTextures(manager);
        reloadModels(manager);
    }

    private static void reloadModels(ResourceManager manager) {
        SPECIAL_LANTERN_MODELS.clear();
        for (Block l : BlockScanner.getInstance().getLanterns()) {

            ResourceLocation reg = Utils.getID(l);
            String namespace = (reg.getNamespace().equals("minecraft") || reg.getNamespace().equals(Amendments.MOD_ID)) ? "" : reg.getNamespace() + "/";
            String s = "block/custom_wall_lanterns/" + namespace + reg.getPath();
            ResourceLocation fullPath = Amendments.res("models/" + s + ".json");
            var resource = manager.getResource(fullPath);
            if (resource.isPresent()) {
                SPECIAL_LANTERN_MODELS.put(l, Amendments.res(s));
            }
        }
    }

    private static void reloadTextures(ResourceManager manager) {
        SPECIAL_MOUNT_TEXTURES.clear();
        for (Block l : BlockScanner.getInstance().getLanterns()) {

            ResourceLocation reg = Utils.getID(l);
            String namespace = (reg.getNamespace().equals("minecraft") || reg.getNamespace().equals(Amendments.MOD_ID)) ? "" : reg.getNamespace() + "/";
            String s = "textures/block/wall_lanterns/" + namespace + reg.getPath() + ".json";
            ResourceLocation fullPath = Amendments.res(s);
            var resource = manager.getResource(fullPath);
            if (resource.isPresent()) {
                try (var stream = resource.get().open()) {
                    JsonElement bsElement = RPUtils.deserializeJson(stream);

                    String texture = RPUtils.findFirstResourceInJsonRecursive(bsElement);
                    if (!texture.isEmpty()) SPECIAL_MOUNT_TEXTURES.put(l, new ResourceLocation(texture));

                } catch (Exception ignored) {
                }
            }
        }
    }

    @Nullable
    public static TextureAtlasSprite getTexture(Block block) {
        var res = SPECIAL_MOUNT_TEXTURES.get(block);
        if (res == null) return null;
        return Minecraft.getInstance().getTextureAtlas(TextureAtlas.LOCATION_BLOCKS).apply(res);
    }

    public static void registerSpecialModels(ClientHelper.SpecialModelEvent event) {
        SPECIAL_LANTERN_MODELS.values().forEach(event::register);
    }

    //returns the normal or custom wall lantern model
    public static BakedModel getModel(BlockModelShaper blockModelShaper, BlockState lantern) {
        var special = SPECIAL_LANTERN_MODELS.get(lantern.getBlock());
        if (special != null) {
            return ClientHelper.getModel(Minecraft.getInstance().getModelManager(), special);
        }
        return blockModelShaper.getBlockModel(lantern);
    }

}
