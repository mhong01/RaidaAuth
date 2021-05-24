package advclient.common.Vaulter;

public class VaulterResult {
    public static int STATUS_PROCESSING = 1;
    public static int STATUS_FINISHED = 2;
    public static int STATUS_ERROR = 3;

    public int status;
    
    public String errText;

    public VaulterResult() {
        status = STATUS_PROCESSING;
    }
}
