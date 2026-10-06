package fail.holocraft.client;

import fail.holocraft.world.PropBlock;
import fail.holocraft.world.PropBlockEntity;
import net.minecraft.client.render.OverlayTexture;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.block.entity.BlockEntityRenderer;
import net.minecraft.client.render.block.entity.BlockEntityRendererFactory;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.math.RotationAxis;
import net.minecraft.util.math.Vec3d;

/**
 * systems: prop_renderer. From the Minecraft camera a prop is a pixel model built from its HoloCure sprite (props.shape3d:
 * two crossed models for round things, one for flat things); from the top-down camera it is the flat sprite (flat_view).
 */
public class PropRenderer implements BlockEntityRenderer<PropBlockEntity> {
    public PropRenderer(BlockEntityRendererFactory.Context ctx) {}

    @Override
    public void render(PropBlockEntity be, float tickDelta, MatrixStack m, VertexConsumerProvider vcp, int light, int overlay) {
        if (!(be.getCachedState().getBlock() instanceof PropBlock block)) return;
        var row = block.row();
        if (HoloSprites.info(row.sprite()) == null) return;
        m.push();
        m.translate(0.5, 0, 0.5);
        if (ViewMode.flat()) {
            FlatView.draw(m, vcp, row.sprite(), 0, 1f, false, be.getPos().getZ() + 0.5, light, OverlayTexture.DEFAULT_UV);
        } else {
            PixelModels.draw(m, vcp, row.sprite(), 0, 1f, SpriteEntityRenderer.PX_PER_BLOCK, row.depthPx(), light, OverlayTexture.DEFAULT_UV);
            if (row.shape3d().equals("cross")) {
                m.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(90));
                PixelModels.draw(m, vcp, row.sprite(), 0, 1f, SpriteEntityRenderer.PX_PER_BLOCK, row.depthPx(), light, OverlayTexture.DEFAULT_UV);
            }
        }
        m.pop();
    }

    @Override
    public boolean rendersOutsideBoundingBox(PropBlockEntity be) { return true; }

    @Override
    public int getRenderDistance() { return 128; }

    @Override
    public boolean isInRenderDistance(PropBlockEntity be, Vec3d pos) {
        return Vec3d.ofCenter(be.getPos()).isInRange(pos, getRenderDistance());
    }
}
