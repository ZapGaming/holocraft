package fail.holocraft.mixin;

import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.entity.EntityRenderer;
import net.minecraft.client.render.entity.state.EntityRenderState;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.text.Text;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

/** systems: player_as_idol. Lets an idol-drawn player keep their name tag. */
@Mixin(EntityRenderer.class)
public interface EntityRendererAccessor {
    @Invoker("renderLabelIfPresent")
    void holocraft$label(EntityRenderState state, Text text, MatrixStack m, VertexConsumerProvider vcp, int light);
}
