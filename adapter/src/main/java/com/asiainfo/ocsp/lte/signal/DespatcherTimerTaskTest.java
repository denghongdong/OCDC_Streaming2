package com.asiainfo.ocsp.lte.signal;

import java.util.TimerTask;

import com.asiainfo.ocdc.lte.process.LteCacheServer;


public class DespatcherTimerTaskTest  extends TimerTask{ 
    
   @Override 
   public void run() { 
   	System.out.println("lbkAllMsg 队列长度===========："+LTEDespatcherServer.lbkAllMsg.size());
   	System.out.println("LTEDespatcherServer receve events counts："+LteCacheServer.msgCount);
   	System.out.println("-------------------------------");
   } 
}
