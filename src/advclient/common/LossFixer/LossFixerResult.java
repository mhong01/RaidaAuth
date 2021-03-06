package advclient.common.LossFixer;

public class LossFixerResult {
    public static int STATUS_PROCESSING = 1;
    public static int STATUS_FINISHED = 2;
    public static int STATUS_ERROR = 3;
    public static int STATUS_CANCELLED = 4;

    
    public int totalFilesProcessed;
    public int totalFiles;
    public int totalRAIDAProcessed;
    
    public int totalCoins;
    public int totalCoinsProcessed;

    
    public int recovered;
    public int failed;
    public int status;

    public int recoveredValue;
    public String receiptId;
    
    public LossFixerResult() {
        status = STATUS_PROCESSING;
    }
}
