package fail.holocraft.client.duck;

/** What player_as_idol needs on a player's render state: which idol to draw, which frame, which way she faces. */
public interface IdolRenderState {
    void holocraft$set(String sprite, int frame, int depthPx, boolean flip);
    String holocraft$sprite();
    int holocraft$frame();
    int holocraft$depthPx();
    boolean holocraft$flip();
}
