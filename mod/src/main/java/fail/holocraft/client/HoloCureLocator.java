package fail.holocraft.client;

import fail.holocraft.HoloCraft;
import net.fabricmc.loader.api.FabricLoader;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Properties;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Finds the player's own HoloCure install. Order: HOLOCRAFT_HOLOCURE env (Melty can pass the game folder),
 * config/holocraft.properties holocure_dir, then every Steam library (libraryfolders.vdf), then itch.
 */
public final class HoloCureLocator {
    public static final String STEAM_APP = "2420510";

    public static Path find() {
        for (Path p : candidates()) {
            if (Files.isRegularFile(p.resolve("data.win"))) {
                HoloCraft.LOG.info("HoloCure found at {}", p);
                return p;
            }
        }
        HoloCraft.LOG.warn("HoloCure not found; looked in {}", candidates());
        return null;
    }

    static List<Path> candidates() {
        Set<Path> out = new LinkedHashSet<>();
        String env = System.getenv("HOLOCRAFT_HOLOCURE");
        if (env != null && !env.isBlank()) out.add(Path.of(env));
        Path cfg = FabricLoader.getInstance().getConfigDir().resolve("holocraft.properties");
        if (Files.isRegularFile(cfg)) {
            Properties pr = new Properties();
            try (var in = Files.newInputStream(cfg)) {
                pr.load(in);
                String d = pr.getProperty("holocure_dir");
                if (d != null && !d.isBlank()) out.add(Path.of(d));
            } catch (IOException ignored) {}
        }
        for (Path steam : steamRoots()) {
            out.add(steam.resolve("steamapps/common/HoloCure"));
            for (Path lib : libraries(steam.resolve("steamapps/libraryfolders.vdf")))
                out.add(lib.resolve("steamapps/common/HoloCure"));
        }
        String appdata = System.getenv("APPDATA");
        if (appdata != null) out.add(Path.of(appdata, "itch", "apps", "holocure"));
        return new ArrayList<>(out);
    }

    static List<Path> steamRoots() {
        List<Path> l = new ArrayList<>();
        for (String env : new String[]{"ProgramFiles(x86)", "ProgramFiles"}) {
            String v = System.getenv(env);
            if (v != null) l.add(Path.of(v, "Steam"));
        }
        l.add(Path.of("C:/Program Files (x86)/Steam"));
        String home = System.getProperty("user.home");
        l.add(Path.of(home, ".steam/steam"));
        l.add(Path.of(home, ".local/share/Steam"));
        l.add(Path.of(home, "Library/Application Support/Steam"));
        try {
            Process proc = new ProcessBuilder("reg", "query", "HKCU\\Software\\Valve\\Steam", "/v", "SteamPath").redirectErrorStream(true).start();
            String o = new String(proc.getInputStream().readAllBytes());
            Matcher m = Pattern.compile("SteamPath\\s+REG_SZ\\s+(.+)").matcher(o);
            if (m.find()) l.add(0, Path.of(m.group(1).trim()));
        } catch (Exception ignored) {}
        return l;
    }

    static List<Path> libraries(Path vdf) {
        List<Path> l = new ArrayList<>();
        try {
            String s = Files.readString(vdf);
            Matcher m = Pattern.compile("\"path\"\\s+\"([^\"]+)\"").matcher(s);
            while (m.find()) l.add(Path.of(m.group(1).replace("\\\\", "\\")));
        } catch (Exception ignored) {}
        return l;
    }

    private HoloCureLocator() {}
}
