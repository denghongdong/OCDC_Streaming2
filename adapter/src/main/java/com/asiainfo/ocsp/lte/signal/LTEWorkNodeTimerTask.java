package com.asiainfo.ocsp.lte.signal;

import java.util.TimerTask;

public class LTEWorkNodeTimerTask extends TimerTask{ 
    
   @Override 
   public void run() { 
   	int lbkSocketSize = LTEworkNodeServer.lbkSocket.size();
   	int lbkAllMsgSize = LTEworkNodeServer.lbkAllMsg.size();
	int msg_queueSize = LTEworkNodeServer.msg_queue.size();
   	System.out.println("lbkSocket 队列长度===========："+lbkSocketSize);
   	System.out.println("lbkAllMsg 队列长度===========："+lbkAllMsgSize);
	System.out.println("msg_queue 队列长度===========："+msg_queueSize);
 	System.out.println("---------------------------------");
   } 
}

