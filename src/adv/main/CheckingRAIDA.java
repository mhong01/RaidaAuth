package adv.main;

import advclient.common.Authenticator.AuthenticatorResult;
import advclient.common.FrackFixer.FrackFixerResult;
import advclient.common.Grader.GraderResult;
import advclient.common.LossFixer.LossFixerResult;
import advclient.common.Unpacker.UnpackerResult;
import advclient.common.Vaulter.VaulterResult;
import advclient.common.core.*;

import java.awt.*;
import java.io.File;
import java.util.Enumeration;

public class CheckingRAIDA {

    private ServantManager sm;
    private ProgramState ps;

    private GLogger logger;

    public CheckingRAIDA() {
        initSystem();
    }

    public void initSystem() {
        logger = new WLogger();
        String home = System.getProperty("user.home");
        //home += File.separator + "CloudCoinWallet";

        sm = new ServantManager(logger, home);
        if (!sm.init()) {
            resetState();
            ps.errText = "Failed to init program. Make sure you have correct folder permissions (" + home + ")";
            return;
        }

        AppCore.readConfig();
        AppCore.copyTemplatesFromJar();
        resetState();
    }

    public void resetState() {
        ps = new ProgramState();
        if (sm == null)
            return;

//        if (sm.getWallets().length != 0) {
//            setActiveWallet(sm.getWallets()[0]);
//            ps.currentScreen = ProgramState.SCREEN_SHOW_TRANSACTIONS;
//        }
    }

    public boolean checkingRaida() {
        return sm.isRAIDAOK();
    }

    public void setupTempSystem() {
        ps.typedWalletName = "testDefault";
        ps.typedEmail = "";
        ps.typedPassword = "";
        ps.cwalletPasswordRequested = false;
        ps.cwalletRecoveryRequested = false;
        if (!sm.initUser(ps.typedWalletName, ps.typedEmail, ps.typedPassword)) {
            ps.errText = "Failed to init Wallet";
            System.out.println("fail init");
//            return;
        }
        if (sm.getWallets().length > 0) {
            System.out.println("there are wallet");
            Wallet[] wallets = sm.getWallets();
            System.out.println(wallets[0].getName());
        }

        Wallet w = sm.getWalletByName("testDefault");
        System.out.println(w.getName());
        File file = new File("/Users/belphegor/Codes/Raida Auth app/1.CloudCoin.Export.stack");
        System.out.println(file.getAbsolutePath());
        ps.files.add(file.getAbsolutePath());
        String totalCoins = AppCore.calcCoinsFromFilenames(ps.files);
        System.out.println(totalCoins);
        Config.DEFAULT_DEPOSIT_DIR = "/Users/belphegor/Codes/Raida Auth app/";
        AppCore.writeConfig();
        if(!sm.isRAIDAOK()) {
            System.out.println("Raida not OK");
        }
        ps.dstWallet.setPassword(ps.typedPassword);
        sm.setActiveWalletObj(ps.dstWallet);
        for (String filename : ps.files) {
            String name = sm.getActiveWallet().getName();
            AppCore.moveToFolderNoTs(filename, Config.DIR_IMPORT, name);
        }
        sm.startUnpackerService(new UnpackerCb());
    }
    private void setRAIDAProgressCoins(int raidaProcessed, int totalCoinsProcessed, int totalCoins) {

        if (totalCoins == 0)
            return;

        String stc = AppCore.formatNumber(totalCoinsProcessed);
        String tc = AppCore.formatNumber(totalCoins);


    }
    private void setRAIDAFixingProgressCoins(int raidaProcessed, int totalCoinsProcessed, int totalCoins, int fixingRAIDA, int round) {

        String stc = AppCore.formatNumber(totalCoinsProcessed);
        String tc = AppCore.formatNumber(totalCoins);

    }

    class UnpackerCb implements CallbackInterface {

        public void callback(Object result) {

            final Object fresult = result;
            final UnpackerResult ur = (UnpackerResult) fresult;

            if (ur.status == UnpackerResult.STATUS_ERROR) {
                EventQueue.invokeLater(new Runnable() {
                    public void run() {
                        if (!ur.errText.isEmpty())
                            ps.errText = ur.errText;
                        else
                            ps.errText = "Failed to Unpack file(s). Please check the logs";

                        ps.currentScreen = ProgramState.SCREEN_IMPORT_DONE;
                    }
                });

                return;
            }

            ps.duplicates = ur.duplicates;
            ps.failedFiles = ur.failedFiles;

            setRAIDAProgressCoins(0, 0, 0);
            sm.startAuthenticatorService(new AuthenticatorCb());
        }
    }

    class AuthenticatorCb implements CallbackInterface {
        public void callback(Object result) {

            final Object fresult = result;
            final AuthenticatorResult ar = (AuthenticatorResult) fresult;
            if (ar.status == AuthenticatorResult.STATUS_ERROR) {
                EventQueue.invokeLater(new Runnable() {
                    public void run() {
                        if (!ar.errText.isEmpty())
                            ps.errText = "<html><div style='text-align:center; width: 520px'>" + ar.errText + "</div></html>";
                        else
                            ps.errText = "Failed to Authencticate Coins";

                        //ps.errText = "Failed to Authencticate Coins";
                        ps.currentScreen = ProgramState.SCREEN_IMPORT_DONE;
                    }
                });
                return;
            } else if (ar.status == AuthenticatorResult.STATUS_FINISHED) {
                sm.startGraderService(new GraderCb(), ps.duplicates, null);
                return;
            } else if (ar.status == AuthenticatorResult.STATUS_CANCELLED) {
                sm.resumeAll();
                EventQueue.invokeLater(new Runnable() {
                    public void run() {
                        ps.errText = "Operation Cancelled";
                        ps.currentScreen = ProgramState.SCREEN_IMPORT_DONE;
                    }
                });
                return;
            }

            setRAIDAProgressCoins(ar.totalRAIDAProcessed, ar.totalCoinsProcessed, ar.totalCoins);
            //setRAIDAProgress(ar.totalRAIDAProcessed, ar.totalFilesProcessed, ar.totalFiles);
        }
    }

    class GraderCb implements CallbackInterface {
        public void callback(Object result) {
            GraderResult gr = (GraderResult) result;

            ps.statToBankValue = gr.totalAuthenticValue + gr.totalFrackedValue;
            ps.statFailedValue = gr.totalCounterfeitValue;
            ps.statLostValue = gr.totalLostValue;
            ps.statToBank = gr.totalAuthentic + gr.totalFracked;
            ps.statFailed = gr.totalCounterfeit;
            ps.statLost = gr.totalLost + gr.totalUnchecked;
            ps.receiptId = gr.receiptId;

            Wallet w = sm.getActiveWallet();
            if (ps.statToBankValue != 0) {
                Wallet wsrc = ps.srcWallet;
                if (wsrc != null && wsrc.isSkyWallet() && ps.cenvelopes != null) {

                    StringBuilder nsb = new StringBuilder();
                    int wholeTotal = 0;

                    Enumeration<String> enumeration = ps.cenvelopes.keys();
                    while (enumeration.hasMoreElements()) {
                        String key = enumeration.nextElement();
                        String[] data = ps.cenvelopes.get(key);

                        int total = 0;
                        try {
                            total = Integer.parseInt(data[1]);
                        } catch (NumberFormatException e) {
                            continue;
                        }

                        wholeTotal += total;
                        if (!nsb.toString().equals(""))
                            nsb.append(",");

                        nsb.append(data[0]);
                    }

                    if (ps.typedMemo.isEmpty()) {
                        ps.dstWallet.appendTransaction(nsb.toString(), wholeTotal, ps.receiptId);
                    } else {
                        ps.dstWallet.appendTransaction(ps.typedMemo, ps.statToBankValue, ps.receiptId);
                    }

                } else {
                    w.appendTransaction(ps.typedMemo, ps.statToBankValue, ps.receiptId);
                }
            } else {
                // StatToBank == 0
                String memo = "";
                if (ps.statFailedValue > 0) {
                    memo = AppCore.formatNumber(ps.statFailedValue) + " Counterfeit";
                } else {
                    memo = "Failed to Import";
                }
                w.appendTransaction(memo, 0, "COUNTERFEIT");
            }

//            EventQueue.invokeLater(new Runnable() {
//                public void run() {
//                    pbarText.setText("Fixing fracked coins ...");
//                    pbarText.repaint();
//                }
//            });

            sm.startFrackFixerService(new FrackFixerCb());
        }
    }

    class FrackFixerCb implements CallbackInterface {
        public void callback(Object result) {
            FrackFixerResult fr = (FrackFixerResult) result;

            if (fr.status == FrackFixerResult.STATUS_PROCESSING) {
                //setRAIDAFixingProgress(fr.totalRAIDAProcessed, fr.totalFilesProcessed, fr.totalFiles, fr.fixingRAIDA, fr.round);
                setRAIDAFixingProgressCoins(fr.totalRAIDAProcessed, fr.totalCoinsProcessed, fr.totalCoins, fr.fixingRAIDA, fr.round);
                return;
            }

            if (fr.status == FrackFixerResult.STATUS_ERROR) {
                ps.errText = "Failed to fix coins";
            }

            if (fr.status == FrackFixerResult.STATUS_CANCELLED) {
                sm.resumeAll();
//                EventQueue.invokeLater(new Runnable() {
//                    public void run() {
//                        ps.errText = "Operation Cancelled";
//                        if (isTransferring())
//                            ps.currentScreen = ProgramState.SCREEN_TRANSFER_DONE;
//                        else if (isWithdrawing()) {
//                            ps.currentScreen = ProgramState.SCREEN_WITHDRAW_DONE;
//                        } else
//                            ps.currentScreen = ProgramState.SCREEN_IMPORT_DONE;
//                        showScreen();
//                    }
//                });
                return;
            }

            if (fr.status == FrackFixerResult.STATUS_FINISHED) {
                if (fr.fixed + fr.failed > 0) {
                }
            }

//            EventQueue.invokeLater(new Runnable() {
//                public void run() {
//                    pbarText.setText("Recovering lost coins ...");
//                    pbarText.repaint();
//                }
//            });

            sm.startLossFixerService(new LossFixerCb());
        }
    }

    class LossFixerCb implements CallbackInterface {
        public void callback(final Object result) {
            LossFixerResult lr = (LossFixerResult) result;

            if (lr.status == LossFixerResult.STATUS_PROCESSING) {
                return;
            }

            if (lr.status == LossFixerResult.STATUS_CANCELLED) {
                ps.errText = "Operation Cancelled";
                sm.resumeAll();
            }



            if (ps.coinIDinFix != null) {
                ps.coinIDinFix.setNotUpdated();
                EventQueue.invokeLater(new Runnable() {
                    public void run() {

                        boolean fixed = true;
                        CloudCoin cc = AppCore.findCoinBySN(Config.DIR_BANK, ps.srcWallet.getName(), ps.coinIDinFix.getIDCoin().sn);
                        if (cc == null) {
                            cc = AppCore.findCoinBySN(Config.DIR_FRACKED, ps.srcWallet.getName(), ps.coinIDinFix.getIDCoin().sn);
                            if (cc == null) {
                                ps.currentScreen = ProgramState.SCREEN_TRANSFER;
                                ps.errText = "Failed to find fixed coin. Please, check main.log file";
                                return;
                            }

                            fixed = false;
                        }


                        if (!AppCore.moveToFolderNewName(cc.originalFile, AppCore.getIDDir(), null, ps.coinIDinFix.getName() + ".stack")) {
                            ps.currentScreen = ProgramState.SCREEN_TRANSFER;
                            ps.errText = "Failed to move ID Coin. Please, check main.log file";
                            return;
                        }

                        ps.currentScreen = ProgramState.SCREEN_TRANSFER;
                        if (fixed)
                            ps.errText = "ID Coin has been fixed. Please try again";
                        else
                            ps.errText = "Failed to fix ID Coin. Please try again later";

                        ps.coinIDinFix.setIDCoin(cc);
                    }
                });
                return;
            }

            if (lr.recovered > 0) {
                sm.getActiveWallet().appendTransaction("LossFixer Recovered", lr.recoveredValue, lr.receiptId);
            }


            if (sm.getActiveWallet().isEncrypted()) {
//                EventQueue.invokeLater(new Runnable() {
//                    public void run() {
//                        pbarText.setText("Encrypting coins ...");
//                        pbarText.repaint();
//                    }
//                });

                sm.startVaulterService(new VaulterCb());
            } else {
                EventQueue.invokeLater(new Runnable() {
                    public void run() {
                        if (isFixing()) {
                            ps.currentScreen = ProgramState.SCREEN_FIX_DONE;
                        } else {
                            if (isTransferring())
                                ps.currentScreen = ProgramState.SCREEN_TRANSFER_DONE;
                            else if (isWithdrawing()) {
                                ps.currentScreen = ProgramState.SCREEN_WITHDRAW_DONE;
                            } else
                                ps.currentScreen = ProgramState.SCREEN_IMPORT_DONE;
                        }
                    }
                });
            }
        }
    }

    class VaulterCb implements CallbackInterface {
        public void callback(final Object result) {
            final Object fresult = result;
            VaulterResult vresult = (VaulterResult) fresult;
            if (vresult.status != VaulterResult.STATUS_FINISHED)
                ps.errText = "Failed to decrypt/encrypt coins";

            EventQueue.invokeLater(new Runnable() {
                public void run() {
                    if (isDepositing()) {
                        ps.currentScreen = ProgramState.SCREEN_IMPORT_DONE;
                    } else if (isTransferring() || isMakingChange()) {
                        ps.currentScreen = ProgramState.SCREEN_TRANSFER_DONE;
                    } else if (isFixing()) {
                        ps.currentScreen = ProgramState.SCREEN_FIX_DONE;
                    } else if (isWithdrawing()) {
                        ps.currentScreen = ProgramState.SCREEN_WITHDRAW_DONE;
                    } else if (isBackupping()) {
                        ps.currentScreen = ProgramState.SCREEN_BACKUP_DONE;
                    }

                }
            });
        }
    }

    public boolean isMakingChange() {
        if (ps.currentScreen == ProgramState.SCREEN_MAKING_CHANGE)
            return true;

        return false;
    }

    public boolean isDepositing() {
        if (ps.currentScreen == ProgramState.SCREEN_PREDEPOSIT ||
                ps.currentScreen == ProgramState.SCREEN_DEPOSIT ||
                ps.currentScreen == ProgramState.SCREEN_IMPORTING ||
                ps.currentScreen == ProgramState.SCREEN_IMPORT_DONE ||
                ps.currentScreen == ProgramState.SCREEN_DEPOSIT_LEFTOVER)

            return true;

        return false;
    }

    public boolean isTransferring() {
        if (ps.currentScreen == ProgramState.SCREEN_TRANSFER ||
                ps.currentScreen == ProgramState.SCREEN_CONFIRM_TRANSFER ||
                ps.currentScreen == ProgramState.SCREEN_SENDING ||
                ps.currentScreen == ProgramState.SCREEN_TRANSFER_DONE)
            return true;

        return false;
    }

    public boolean isWithdrawing() {
        if (ps.currentScreen == ProgramState.SCREEN_WITHDRAW ||
                ps.currentScreen == ProgramState.SCREEN_CONFIRM_WITHDRAW ||
                ps.currentScreen == ProgramState.SCREEN_WITHDRAWING ||
                ps.currentScreen == ProgramState.SCREEN_WITHDRAW_DONE)
            return true;

        return false;

    }



    public boolean isFixing() {
        if (ps.currentScreen == ProgramState.SCREEN_FIX_FRACKED ||
                ps.currentScreen == ProgramState.SCREEN_FIXING_FRACKED ||
                ps.currentScreen == ProgramState.SCREEN_FIX_DONE)
            return true;

        return false;
    }
    public boolean isBackupping() {
        if (ps.currentScreen == ProgramState.SCREEN_BACKUP ||
                ps.currentScreen == ProgramState.SCREEN_BACKUP_DONE ||
                ps.currentScreen == ProgramState.SCREEN_DOING_BACKUP)
            return true;

        return false;
    }
}
