package fail.holocraft.client;

/** What the server last said about tonight's stage (Payloads.Stage). */
public final class ClientStage {
    public static volatile String stage = "";
    public static volatile boolean active;
    public static volatile int cleared;
    public static volatile boolean boss;

    private ClientStage() {}
}
