/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package adv.main;

import advclient.common.core.GLogger;
import advclient.common.core.GLoggerInterface;

/**
 *
 * @author Александр
 */
public class WLogger extends GLogger implements GLoggerInterface {

    @Override
    public void onLog(int level, String tag, String message) {
        String levelStr; 
        
        if (level == GL_DEBUG) {
         //   System.out.println("tag: " + tag + " " + message);
            levelStr = "[DEBUG]";
        } else if (level == GL_VERBOSE) {
//            System.out.println("tag: " + tag + " " + message);
            levelStr = "[VERBOSE]";
        } else if (level == GL_ERROR) {
  //          System.out.println("tag: " + tag + " " + message);
            levelStr = "[ERROR]";
        } else {
            levelStr = "[INFO]";
        }
        
        logCommon(tag + " " + levelStr + " " + message);
    }
}
