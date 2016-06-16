package com.asiainfo.ocsp.lte.signal;

import java.util.TimerTask;


public class DespatcherTimerTaskTest  extends TimerTask{ 
    
   @Override 
   public void run() { 
   	System.out.println("lbkAllMsg 队列长度===========："+LTEDespatcherServer.lbkAllMsg.size());
   } 
}
