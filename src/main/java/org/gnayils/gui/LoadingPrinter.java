package org.gnayils.gui;

import java.util.Timer;
import java.util.TimerTask;

public  class LoadingPrinter extends TimerTask {

    private String prefixDesc;
    private int index;

    @Override
    public void run() {
        if (index % 6 == 0) {
            System.out.print("\r");
            System.out.print(prefixDesc);
        }
        System.out.print(".");
        index++;
    }

    private static Timer timer = new Timer();
    private static LoadingPrinter currentLoadingPrinter;

    public static void print(String prefixDesc) {
        stopCurrentPrint();
        currentLoadingPrinter = new LoadingPrinter();
        currentLoadingPrinter.prefixDesc = prefixDesc;
        timer.schedule(currentLoadingPrinter, 0, 500);
    }

    public static void stopCurrentPrint() {
        if(currentLoadingPrinter != null)
            currentLoadingPrinter.cancel();
        timer.purge();
    }
}
