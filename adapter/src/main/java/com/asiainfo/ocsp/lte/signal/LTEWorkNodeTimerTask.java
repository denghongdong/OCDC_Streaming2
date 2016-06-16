package com.asiainfo.ocsp.lte.signal;

import java.util.TimerTask;

public class LTEWorkNodeTimerTask extends TimerTask{ 
    
   @Override 
   public void run() { 
//   	int lbkSocketSize = LTEworkNodeServer.lbkSocket.size();
   	System.out.println("lbkAllMsg 队列长度===========："+LTEworkNodeServer.lbkAllMsg.size());
	System.out.println("msg_queue 队列长度===========："+LTEworkNodeServer.msg_queue.size());
 	System.out.println("---------------------------------");
   } 
}

