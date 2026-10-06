package fail.holocraft.client;

import fail.holocraft.HoloCraft;
import net.minecraft.client.render.OverlayTexture;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.math.RotationAxis;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * systems: pixel_models. Any HoloCure sprite frame becomes a thick pixel model the way Minecraft thickens an item
 * icon: the sprite on the front and back, and a side wall along every run of pixels that borders transparency, each
 * wall coloured by the pixels it edges. Built once per frame from the player's own pack and cached.
 */
public final class PixelModels {
    /** One quad: 4 corners of (x, y, u, v) in sprite pixels/uv, a z pair, and its normal. */
    private record Quad(float[] xyuv, float z0, float z1, float nx, float ny, float nz, boolean wall) {}

    public record Mesh(int w, int h, List<Quad> quads) {}

    private static final Map<String, Mesh> CACHE = new HashMap<>();
    private static final Mesh NONE = new Mesh(0, 0, List.of());

    public static void clear() { CACHE.clear(); }

    public static Mesh mesh(String sprite, int frame) {
        HoloSprites.Info info = HoloSprites.info(sprite);
        if (info == null) return NONE;
        int f = Math.floorMod(frame, info.frames());
        return CACHE.computeIfAbsent(sprite + "#" + f, k -> build(sprite, f));
    }

    private static Mesh build(String sprite, int frame) {
        Path png = HoloCureImport.packDir().resolve("assets/holocraft/textures/hc/" + HoloCureImport.texPath(sprite) + "/" + frame + ".png");
        BufferedImage img;
        try (var in = Files.newInputStream(png)) {
            img = ImageIO.read(in);
        } catch (Exception e) {
            HoloCraft.LOG.warn("pixel model: cannot read {}", png);
            return NONE;
        }
        int w = img.getWidth(), h = img.getHeight();
        boolean[] solid = new boolean[w * h];
        for (int y = 0; y < h; y++) for (int x = 0; x < w; x++) solid[y * w + x] = (img.getRGB(x, y) >>> 24) > 127;
        List<Quad> q = new ArrayList<>();
        // front and back: the whole sprite, cutout drops the clear pixels
        q.add(new Quad(new float[]{0, h, 0, 1, w, h, 1, 1, w, 0, 1, 0, 0, 0, 0, 0}, 1, 1, 0, 0, 1, false));
        q.add(new Quad(new float[]{w, h, 1, 1, 0, h, 0, 1, 0, 0, 0, 0, w, 0, 1, 0}, -1, -1, 0, 0, -1, false));
        // horizontal walls: runs along x of pixels whose upper (dy=-1) or lower (dy=+1) neighbour is clear
        for (int dy : new int[]{-1, 1}) {
            for (int y = 0; y < h; y++) {
                int x = 0;
                while (x < w) {
                    if (!edge(solid, w, h, x, y, 0, dy)) { x++; continue; }
                    int x0 = x;
                    while (x < w && edge(solid, w, h, x, y, 0, dy)) x++;
                    float ey = dy < 0 ? y : y + 1, v0 = (y + 0.01f) / h, v1 = (y + 0.99f) / h;
                    q.add(new Quad(new float[]{x0, ey, x0 / (float) w, v0, x, ey, x / (float) w, v0, x, ey, x / (float) w, v1, x0, ey, x0 / (float) w, v1},
                            -1, 1, 0, -dy, 0, true));
                }
            }
        }
        // vertical walls: runs along y of pixels whose left or right neighbour is clear
        for (int dx : new int[]{-1, 1}) {
            for (int x = 0; x < w; x++) {
                int y = 0;
                while (y < h) {
                    if (!edge(solid, w, h, x, y, dx, 0)) { y++; continue; }
                    int y0 = y;
                    while (y < h && edge(solid, w, h, x, y, dx, 0)) y++;
                    float ex = dx < 0 ? x : x + 1, u0 = (x + 0.01f) / w, u1 = (x + 0.99f) / w;
                    q.add(new Quad(new float[]{ex, y0, u0, y0 / (float) h, ex, y0, u1, y0 / (float) h, ex, y, u1, y / (float) h, ex, y, u0, y / (float) h},
                            -1, 1, dx, 0, 0, true));
                }
            }
        }
        return new Mesh(w, h, q);
    }

    private static boolean edge(boolean[] s, int w, int h, int x, int y, int dx, int dy) {
        if (!s[y * w + x]) return false;
        int nx = x + dx, ny = y + dy;
        return nx < 0 || ny < 0 || nx >= w || ny >= h || !s[ny * w + nx];
    }

    /**
     * Draw a sprite frame as a pixel model standing on the current origin: its origin (feet) on the floor, its plane
     * the local XY plane, `depthPx` thick. `size` multiplies HoloCure pixels -> blocks at `pxPerBlock`.
     */
    public static void draw(MatrixStack m, VertexConsumerProvider vcp, String sprite, int frame, float size, float pxPerBlock,
                            int depthPx, int light, int overlay) {
        HoloSprites.Info info = HoloSprites.info(sprite);
        Mesh mesh = mesh(sprite, frame);
        if (info == null || mesh.quads().isEmpty()) return;
        float s = size / pxPerBlock, half = depthPx * s / 2f;
        MatrixStack.Entry e = m.peek();
        VertexConsumer vc = vcp.getBuffer(RenderLayer.getEntityCutoutNoCull(HoloSprites.texture(sprite, frame)));
        for (Quad q : mesh.quads()) {
            float[] c = q.xyuv();
            for (int i = 0; i < 4; i++) {
                float px = c[i * 4], py = c[i * 4 + 1];
                float z = q.wall() ? (i == 0 || i == 1 ? -half : half) : q.z0() * half;
                if (q.wall() && q.nx() != 0) z = (i == 0 || i == 3) ? -half : half;
                vc.vertex(e, (px - info.ox()) * s, (info.oy() - py) * s, z)
                        .color(0xFFFFFFFF).texture(c[i * 4 + 2], c[i * 4 + 3]).overlay(overlay).light(light)
                        .normal(e, q.nx(), q.ny(), q.nz());
            }
        }
    }

    /** A thick pixel model turned to face the camera around the vertical axis; `flip` shows its mirrored side. */
    public static void drawFacing(MatrixStack m, VertexConsumerProvider vcp, String sprite, int frame, float size, float pxPerBlock,
                                  int depthPx, boolean flip, float cameraYaw, int light, int overlay) {
        m.push();
        m.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(180 - cameraYaw + (flip ? 180 : 0)));
        draw(m, vcp, sprite, frame, size, pxPerBlock, depthPx, light, overlay);
        m.pop();
    }

    private PixelModels() {}
}
