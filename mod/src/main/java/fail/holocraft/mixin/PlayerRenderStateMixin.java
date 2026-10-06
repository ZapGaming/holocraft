package fail.holocraft.mixin;

import fail.holocraft.client.duck.IdolRenderState;
import net.minecraft.client.render.entity.state.PlayerEntityRenderState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;

/** systems: player_as_idol. Carries the idol to draw from updateRenderState to render. */
@Mixin(PlayerEntityRenderState.class)
public abstract class PlayerRenderStateMixin implements IdolRenderState {
    @Unique private String holocraft$sprite;
    @Unique private int holocraft$frame, holocraft$depthPx;
    @Unique private boolean holocraft$flip;

    @Override public void holocraft$set(String sprite, int frame, int depthPx, boolean flip) {
        holocraft$sprite = sprite; holocraft$frame = frame; holocraft$depthPx = depthPx; holocraft$flip = flip;
    }
    @Override public String holocraft$sprite() { return holocraft$sprite; }
    @Override public int holocraft$frame() { return holocraft$frame; }
    @Override public int holocraft$depthPx() { return holocraft$depthPx; }
    @Override public boolean holocraft$flip() { return holocraft$flip; }
}
