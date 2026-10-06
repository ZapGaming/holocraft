package fail.holocraft.holocure;

import org.apache.commons.compress.compressors.bzip2.BZip2CompressorInputStream;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.Arrays;

/** An ARGB image, plus the decoder for GameMaker texture blobs (PNG, QOI "fioq", bzip2+QOI "2zoq"). */
public record GmImage(int width, int height, int[] argb) {

    public static GmImage decode(byte[] blob) throws IOException {
        if (blob[0] == (byte) 0x89 && blob[1] == 'P') {
            BufferedImage b = ImageIO.read(new ByteArrayInputStream(blob));
            int[] px = b.getRGB(0, 0, b.getWidth(), b.getHeight(), null, 0, b.getWidth());
            return new GmImage(b.getWidth(), b.getHeight(), px);
        }
        if (blob[0] == '2' && blob[1] == 'z' && blob[2] == 'o' && blob[3] == 'q') {
            int headerLen = (blob[8] == 'B' && blob[9] == 'Z') ? 8 : 12;
            try (var in = new BZip2CompressorInputStream(new ByteArrayInputStream(blob, headerLen, blob.length - headerLen))) {
                return qoi(in.readAllBytes());
            }
        }
        if (blob[0] == 'f' && blob[1] == 'i' && blob[2] == 'o' && blob[3] == 'q') return qoi(blob);
        throw new IOException("unknown texture format");
    }

    private static int sx(int v, int bits) {
        v &= (1 << bits) - 1;
        return (v >> (bits - 1)) != 0 ? v - (1 << bits) : v;
    }

    /** GameMaker's QOI variant (UndertaleModTool's QoiConverter documents the same opcodes). */
    static GmImage qoi(byte[] q) {
        ByteBuffer bb = ByteBuffer.wrap(q).order(ByteOrder.LITTLE_ENDIAN);
        int w = bb.getShort(4) & 0xFFFF, h = bb.getShort(6) & 0xFFFF, length = bb.getInt(8);
        int pos = 12, end = 12 + length;
        int r = 0, g = 0, b = 0, a = 255, run = 0;
        int[] index = new int[256];
        int[] out = new int[w * h];
        for (int i = 0; i < out.length; i++) {
            if (run > 0) run--;
            else if (pos < end) {
                int b1 = q[pos++] & 0xFF;
                if ((b1 & 0xC0) == 0x00) {
                    int ix = b1 << 2;
                    r = index[ix]; g = index[ix + 1]; b = index[ix + 2]; a = index[ix + 3];
                } else if ((b1 & 0xE0) == 0x40) {
                    run = b1 & 0x1F;
                } else if ((b1 & 0xE0) == 0x60) {
                    int b2 = q[pos++] & 0xFF;
                    run = (((b1 & 0x1F) << 8) | b2) + 32;
                } else if ((b1 & 0xC0) == 0x80) {
                    r = (r + sx(b1 >> 4, 2)) & 255; g = (g + sx(b1 >> 2, 2)) & 255; b = (b + sx(b1, 2)) & 255;
                } else if ((b1 & 0xE0) == 0xC0) {
                    int m = (b1 << 8) | (q[pos++] & 0xFF);
                    r = (r + sx(m >> 8, 5)) & 255; g = (g + sx(m >> 4, 4)) & 255; b = (b + sx(m, 4)) & 255;
                } else if ((b1 & 0xF0) == 0xE0) {
                    int m = (b1 << 16) | ((q[pos] & 0xFF) << 8) | (q[pos + 1] & 0xFF);
                    pos += 2;
                    r = (r + sx(m >> 15, 5)) & 255; g = (g + sx(m >> 10, 5)) & 255; b = (b + sx(m >> 5, 5)) & 255; a = (a + sx(m, 5)) & 255;
                } else {
                    if ((b1 & 8) != 0) r = q[pos++] & 0xFF;
                    if ((b1 & 4) != 0) g = q[pos++] & 0xFF;
                    if ((b1 & 2) != 0) b = q[pos++] & 0xFF;
                    if ((b1 & 1) != 0) a = q[pos++] & 0xFF;
                }
                int ix = ((r ^ g ^ b ^ a) & 63) << 2;
                index[ix] = r; index[ix + 1] = g; index[ix + 2] = b; index[ix + 3] = a;
            }
            out[i] = (a << 24) | (r << 16) | (g << 8) | b;
        }
        return new GmImage(w, h, out);
    }

    public int get(int x, int y) { return argb[y * width + x]; }

    public static GmImage blank(int w, int h) { return new GmImage(w, h, new int[w * h]); }

    public GmImage crop(int x, int y, int w, int h) {
        int[] o = new int[w * h];
        for (int j = 0; j < h; j++)
            for (int i = 0; i < w; i++)
                if (x + i >= 0 && y + j >= 0 && x + i < width && y + j < height) o[j * w + i] = get(x + i, y + j);
        return new GmImage(w, h, o);
    }

    /** Nearest-neighbour scale, which is what pixel art wants. */
    public GmImage scale(int w, int h) {
        int[] o = new int[w * h];
        for (int j = 0; j < h; j++)
            for (int i = 0; i < w; i++) o[j * w + i] = get(Math.min(width - 1, i * width / w), Math.min(height - 1, j * height / h));
        return new GmImage(w, h, o);
    }

    /** Scale to cover w x h keeping aspect, centred crop. */
    public GmImage cover(int w, int h) {
        double s = Math.max((double) w / width, (double) h / height);
        int sw = (int) Math.ceil(width * s), sh = (int) Math.ceil(height * s);
        return scale(sw, sh).crop((sw - w) / 2, (sh - h) / 2, w, h);
    }

    /** Nine-slice into w x h, keeping `border` source pixels at each edge. */
    public GmImage nineSlice(int w, int h, int border) {
        int bs = Math.min(border, Math.min(width, height) / 2);
        int bd = Math.min(bs, Math.min(w, h) / 2);
        int[] o = new int[w * h];
        for (int j = 0; j < h; j++) {
            int sy = j < bd ? j * bs / Math.max(1, bd) : j >= h - bd ? height - (h - j) * bs / Math.max(1, bd) : bs + (j - bd) * (height - 2 * bs) / Math.max(1, h - 2 * bd);
            for (int i = 0; i < w; i++) {
                int sxp = i < bd ? i * bs / Math.max(1, bd) : i >= w - bd ? width - (w - i) * bs / Math.max(1, bd) : bs + (i - bd) * (width - 2 * bs) / Math.max(1, w - 2 * bd);
                o[j * w + i] = get(Math.min(width - 1, Math.max(0, sxp)), Math.min(height - 1, Math.max(0, sy)));
            }
        }
        return new GmImage(w, h, o);
    }

    public GmImage tint(int rgb) {
        int tr = (rgb >> 16) & 255, tg = (rgb >> 8) & 255, tb = rgb & 255;
        int[] o = Arrays.copyOf(argb, argb.length);
        for (int i = 0; i < o.length; i++) {
            int c = o[i];
            o[i] = (c & 0xFF000000) | ((((c >> 16) & 255) * tr / 255) << 16) | ((((c >> 8) & 255) * tg / 255) << 8) | ((c & 255) * tb / 255);
        }
        return new GmImage(width, height, o);
    }

    /** Alpha-composite `top` at (x, y). */
    public void draw(GmImage top, int x, int y) {
        for (int j = 0; j < top.height; j++)
            for (int i = 0; i < top.width; i++) {
                int tx = x + i, ty = y + j;
                if (tx < 0 || ty < 0 || tx >= width || ty >= height) continue;
                int s = top.get(i, j), sa = s >>> 24;
                if (sa == 0) continue;
                if (sa == 255) { argb[ty * width + tx] = s; continue; }
                int dcol = argb[ty * width + tx], da = dcol >>> 24;
                int oa = sa + da * (255 - sa) / 255;
                int[] ch = new int[3];
                for (int k = 0; k < 3; k++) {
                    int sc = (s >> (16 - 8 * k)) & 255, dc = (dcol >> (16 - 8 * k)) & 255;
                    ch[k] = oa == 0 ? 0 : (sc * sa + dc * da * (255 - sa) / 255) / oa;
                }
                argb[ty * width + tx] = (oa << 24) | (ch[0] << 16) | (ch[1] << 8) | ch[2];
            }
    }

    public void writePng(java.nio.file.Path path) throws IOException {
        BufferedImage b = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        b.setRGB(0, 0, width, height, argb, 0, width);
        java.nio.file.Files.createDirectories(path.getParent());
        ImageIO.write(b, "png", path.toFile());
    }
}
