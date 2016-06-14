package com.asiainfo.ocdc.lte.process;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Properties;
import java.util.Set;
import java.util.Map.Entry;
import java.util.concurrent.LinkedBlockingQueue;

import kafka.producer.KeyedMessage;

import org.apache.log4j.Logger;

/**
 * @since 2016.05.04
 * @author 宿荣全
 * @see 4G数据LTE的socket接收（信令端）
 */
public class LTETypeSignalPort implements Runnable {
	private  Logger logger = Logger.getLogger(LTETypeSignalPort.class);
	private LinkedBlockingQueue<byte[]> lbkAllMsg = null;
	private LinkedBlockingQueue<ArrayList<KeyedMessage<String, String>>> msg_queue = null;
	private SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
	private HashMap<String, String> topicMap = null;
	private Properties prop = null;
	// 
	private String itemSeparator = "|";

	public LTETypeSignalPort(
			LinkedBlockingQueue<byte[]> lbkAllMsg,
			LinkedBlockingQueue<ArrayList<KeyedMessage<String, String>>> msg_queue,
			Properties prop) {
		this.lbkAllMsg = lbkAllMsg;
		this.msg_queue = msg_queue;
		this.prop = prop;
	}

	// ［配置文件配置项］kafka异步发送数据包条数
	int uuSsendmsgNum = 0;
	int x2SsendmsgNum = 0;
	int uE_MRSsendmsgNum = 0;
	int cell_MRSsendmsgNum = 0;
	int s1_MMESsendmsgNum = 0;
	int s6aSsendmsgNum = 0;
	int s11SsendmsgNum = 0;
	int sGsSsendmsgNum = 0;
	boolean time_flg = false;
	private void setProp(){
		// ［配置文件配置项］kafka异步发送数据包条数
		uuSsendmsgNum = Integer.parseInt(prop.getProperty("socket.producer.sendmsg.uu.size").trim());
		x2SsendmsgNum = Integer.parseInt(prop.getProperty("socket.producer.sendmsg.x2.size").trim());
		uE_MRSsendmsgNum = Integer.parseInt(prop.getProperty("socket.producer.sendmsg.uE_MR.size").trim());
		cell_MRSsendmsgNum = Integer.parseInt(prop.getProperty("socket.producer.sendmsg.cell_MR.size").trim());
		s1_MMESsendmsgNum = Integer.parseInt(prop.getProperty("socket.producer.sendmsg.s1_MME.size").trim());
		s6aSsendmsgNum = Integer.parseInt(prop.getProperty("socket.producer.sendmsg.s6a.size").trim());
		s11SsendmsgNum = Integer.parseInt(prop.getProperty("socket.producer.sendmsg.s11.size").trim());
		sGsSsendmsgNum = Integer.parseInt(prop.getProperty("socket.producer.sendmsg.sGs.size").trim());
		// ［配置文件配置项］是否追加接收到信令时的时间戳
		time_flg = Boolean.valueOf(prop.getProperty("socket.lte.append.time.flg"));
	}

	private ArrayList<KeyedMessage<String, String>> uumsgList = new ArrayList<KeyedMessage<String, String>>();
//	private ArrayList<KeyedMessage<String, String>> x2msgList = new ArrayList<KeyedMessage<String, String>>();
	private ArrayList<KeyedMessage<String, String>> uE_MRmsgList = new ArrayList<KeyedMessage<String, String>>();
	private ArrayList<KeyedMessage<String, String>> cell_MRmsgList = new ArrayList<KeyedMessage<String, String>>();
	private ArrayList<KeyedMessage<String, String>> s1_MMEmsgList = new ArrayList<KeyedMessage<String, String>>();
	private ArrayList<KeyedMessage<String, String>> s6amsgList = new ArrayList<KeyedMessage<String, String>>();
	private ArrayList<KeyedMessage<String, String>> s11msgList = new ArrayList<KeyedMessage<String, String>>();
	private ArrayList<KeyedMessage<String, String>> sGsmsgList = new ArrayList<KeyedMessage<String, String>>();
	
	public void run() {
		setProp();
		// topic map 解析
		topicMap = propAnalysis();
		// 解析信令并分类
		short messageType = 0;
		int sequenceId = 0;
		byte totalContents = 0;
		int totalLength = 0;
		byte[] buffer_lte = null;
		try {
			logger.info("多线程分类处理数据 LTETypeSignalPort启动成功！");
			while (true) {
				int off = 0;
				buffer_lte = lbkAllMsg.take();
				totalLength = buffer_lte.length + 2;
				messageType = ConvToByte.byteToShort(buffer_lte, off);
				off += 2;
				sequenceId = ConvToByte.byteToInt(buffer_lte, off);
				off += 4;
				totalContents = buffer_lte[off++];
				off += 1;
				// 　信令类型
				int signalType = buffer_lte[off + 4] & 0xff;
				// 单接口xdr，接口类型是5的场合，判断为s1-mme接口，否则记录到badmessage
				for (int i = 0; i < (totalContents & 0xff); i++) {
					// 　信令解析
					signalAnalysis(signalType, buffer_lte,off, totalContents, time_flg);
				}
			}
		} catch (Exception e) {
			logger.error("totalLength=" + totalLength + ";messageType="
					+ messageType + ";sequenceId=" + sequenceId
					+ ";totalContents=" + totalContents);
			e.printStackTrace();
		}
	}

	/**
	 * 
	 * @param signalType
	 * @param buffer_lte
	 * @param time_flg
	 */
	private void signalAnalysis(int signalType, byte[] buffer_lte,int off, byte totalContents, boolean time_flg) {

		switch (signalType) {
		 case 1:
		 // Uu_flg
		 uu_Analysis(buffer_lte,off,time_flg);
		 break;
		 case 2:
		 //X2_flg
		 X2Analysis(buffer_lte,off,time_flg);
		 break;
		 case 3:
		 //UE_MR_flg
		uE_MRAnalysis(buffer_lte,off,time_flg);
		 break;
		 case 4:
		 // Cell_MR_flg
		 cell_MRAnalysis(buffer_lte,off,time_flg);
		 break;
		case 5:
		// S1-MME_flg
		s1_MME_Analysis(buffer_lte,off,time_flg);
		break;
		 case 6:
		 // S6a_flg
		 sa6_Analysis(buffer_lte,off,time_flg);
		 break;
		 case 7:
		 //S11_flg
		 s11_Analysis(buffer_lte,off,time_flg);
		 break;
		 case 9:
		 // SGs_flg
		 sGs_Analysis(buffer_lte,off,time_flg);
	 break;
		default:
			break;
		}
		buffer_lte = null;
	}
	
	/**
	 * Ｕｕ　解析 type =１
	 * @param buffer_lte
	 * @param totalContents
	 * @param time_print_flg
	 */
	private void uu_Analysis(byte[] buffer, int off, boolean time_print_flg) {

		StringBuilder sb = new StringBuilder();
		sb.append(ConvToByte.byteToUnsignedShort(buffer, off) + itemSeparator);// Length
		off += 2;
		sb.append(ConvToByte.decodeTBCD(buffer, off, off + 2, true) + itemSeparator);// City
		off += 2;
		sb.append(buffer[off] + itemSeparator);// Interface
		off += 1;
		sb.append(ConvToByte.getHexString(buffer, off, off + 16) + itemSeparator);// XDR
																		// ID
		off += 16;
		sb.append(ConvToByte.byteToUnsignedByte(buffer, off) + itemSeparator);// RAT
		off += 1;
		sb.append(ConvToByte.decodeTBCD(buffer, off, off + 8, true) + itemSeparator);// IMSI
		off += 8;
		sb.append(ConvToByte.decodeTBCD(buffer, off, off + 8, true) + itemSeparator);// IMEI
		off += 8;
		sb.append(ConvToByte.decodeTBCD(buffer, off, off + 16, false) + itemSeparator);// MSISDN
		off += 16;
		sb.append((buffer[off] & 0xff) + itemSeparator);// Procedure Type
		off += 1;
		sb.append(ConvToByte.byteToLong(buffer, off) + itemSeparator);// Procedure Start
															// Time
		off += 8;
		sb.append(ConvToByte.byteToLong(buffer, off) + itemSeparator);// Procedure End
															// Time
		off += 8;
		
		sb.append((buffer[off] & 0xff) + itemSeparator);// Keyword 1
		off += 1;
		sb.append((buffer[off] & 0xff) + itemSeparator);// Keyword 2
		off += 1;
		sb.append((buffer[off] & 0xff) + itemSeparator);// Procedure Status
		off += 1;
		sb.append(ConvToByte.byteToUnsignedShort(buffer, off) + itemSeparator);// PLMN ID
																		
		off += 3;
		sb.append(ConvToByte.byteToUnsignedShort(buffer, off) + itemSeparator);// eNB ID
																		
		off += 4;
		sb.append(ConvToByte.byteToUnsignedInt(buffer, off) + itemSeparator);// Cell ID
																	
		off += 4;
		sb.append(ConvToByte.byteToUnsignedShort(buffer, off) + itemSeparator);// C-RNTI
		
		off += 2;
		
		sb.append(ConvToByte.byteToUnsignedByte(buffer, off) + itemSeparator);// Target eNB ID
		off += 4;
		sb.append(ConvToByte.getHexString(buffer, off, off + 4) + itemSeparator);// Target Cell ID
		off += 4;
		sb.append(ConvToByte.byteToUnsignedShort(buffer, off) + itemSeparator);// Target C-RNTI
		off += 2;
		sb.append(ConvToByte.byteToUnsignedByte(buffer, off) + itemSeparator);// MME UE 
		off += 4;
																	// S1AP ID
		sb.append(ConvToByte.getHexString(buffer, off, off + 4) + itemSeparator);// MME Group ID
		off += 2;
		sb.append(ConvToByte.getIpv4(buffer, off) + itemSeparator);// MME Code
		off += 1;
		sb.append(ConvToByte.getIpv6(buffer, off) + itemSeparator);// M-TMSI
		off += 4;
		sb.append(ConvToByte.getIp(buffer, off) + itemSeparator);// CSFB Indication
		off += 1;
		sb.append(ConvToByte.getIp(buffer, off) + itemSeparator);// Redirected Network
		off += 1;

		int epsBearerNum = ConvToByte.byteToUnsignedByte(buffer, off);
		sb.append(epsBearerNum + itemSeparator);// EPS Bearer Number
		off += 1;
		for (int n = 0; n < epsBearerNum; n++) {
			sb.append(ConvToByte.byteToUnsignedByte(buffer, off) + itemSeparator);// Bearer
																		// 1 ID
			off += 1;
			sb.append(ConvToByte.byteToUnsignedByte(buffer, off) + itemSeparator);// Bearer
																		// 1
																		// Type
			off += 1;
			sb.append(ConvToByte.byteToUnsignedByte(buffer, off) + itemSeparator);// Bearer
																		// 1 QCI
			off += 1;
			sb.append(ConvToByte.byteToUnsignedByte(buffer, off) + itemSeparator);// Bearer
																		// 1
																		// Status
			off += 1;
			sb.append(ConvToByte.byteToUnsignedShort(buffer, off) + itemSeparator);// Bearer
																			// 1
																			// Request
																			// Cause
			off += 2;
			sb.append(ConvToByte.byteToUnsignedShort(buffer, off) + itemSeparator);// Bearer
																			// 1
																			// Failure
																			// Cause
			off += 2;
			sb.append(ConvToByte.byteToUnsignedInt(buffer, off) + itemSeparator);// Bearer
																		// 1 eNB
																		// GTP-TEID
			off += 4;
			sb.append(ConvToByte.byteToUnsignedInt(buffer, off) + itemSeparator);// Bearer
																		// 1 SGW
																		// GTP-TEID
			off += 4;
		}
		buffer = null;
		if (time_flg) {
			sb.append(sdf.format(System.currentTimeMillis()));
		}else {
			sb.deleteCharAt(sb.lastIndexOf("|"));
		}
		String message= sb.toString();
		KeyedMessage<String, String> km = new KeyedMessage<String, String>(topicMap.get("1"),String.valueOf(message.hashCode()),message);
		// 如果消息队列满足sendmsgNum条向kafka队列缓存队列存储
		if (uumsgList.size() == uuSsendmsgNum) {
			msg_queue.offer(uumsgList);
			uumsgList = new ArrayList<KeyedMessage<String, String>>();
		}
		uumsgList.add(km);
	}
	/**
	 * ｘ２　解析 type =２
	 * @param buffer_lte
	 * @param totalContents
	 * @param time_print_flg
	 */
	private void X2Analysis(byte[] buffer_lte, int off, boolean time_print_flg) {

	}
	
	/**
	 * ＵE_MR　解析 type =3
	 * 
	 * @param buffer_lte
	 * @param time_print_flg
	 */
	private void uE_MRAnalysis(byte[] buffer,int off, boolean time_print_flg) {
		
		StringBuilder sb = new StringBuilder();
		sb.append(ConvToByte.byteToUnsignedShort(buffer, off) + itemSeparator);// Length
		off += 2;
		sb.append(ConvToByte.decodeTBCD(buffer, off, off + 2, true) + itemSeparator);// City
		off += 2;
		sb.append(buffer[off] + itemSeparator);// Interface
		off += 1;
		sb.append(ConvToByte.getHexString(buffer, off, off + 16) + itemSeparator);// XDR
																		// ID
		off += 16;
		sb.append(ConvToByte.byteToUnsignedByte(buffer, off) + itemSeparator);// RAT
		off += 1;
		sb.append(ConvToByte.decodeTBCD(buffer, off, off + 8, true) + itemSeparator);// IMSI
		off += 8;
		sb.append(ConvToByte.decodeTBCD(buffer, off, off + 8, true) + itemSeparator);// IMEI
		off += 8;
		sb.append(ConvToByte.decodeTBCD(buffer, off, off + 16, false) + itemSeparator);// MSISDN
		off += 16;
		sb.append(ConvToByte.byteToUnsignedShort(buffer, off) + itemSeparator);// MME
																		// Group
																		// ID
		off += 2;
		sb.append(ConvToByte.byteToUnsignedByte(buffer, off) + itemSeparator);// MME Code
		off += 1;
		sb.append(ConvToByte.byteToUnsignedInt(buffer, off) + itemSeparator);// MME UE
																	// S1AP ID
		off += 4;
		sb.append(ConvToByte.byteToUnsignedInt(buffer, off) + itemSeparator);// eNB ID
		off += 4;
		sb.append(ConvToByte.byteToUnsignedInt(buffer, off) + itemSeparator);// Cell ID
		off += 4;
		sb.append(ConvToByte.byteToLong(buffer, off) + itemSeparator);// Time
		off += 8;
		sb.append((buffer[off] & 0xff) + itemSeparator);// MR type
		off += 1;
		sb.append((buffer[off] & 0xff) + itemSeparator);// PHR
		off += 1;
		sb.append(ConvToByte.byteToUnsignedShort(buffer, off) + itemSeparator);// eNB
																		// Received
																		// Power
		off += 2;
		sb.append((buffer[off] & 0xff) + itemSeparator);// UL SINR
		off += 1;
		sb.append(ConvToByte.byteToUnsignedShort(buffer, off) + itemSeparator);// TA
		off += 2;
		sb.append(ConvToByte.byteToUnsignedShort(buffer, off) + itemSeparator);// AoA
		off += 2;
		sb.append(ConvToByte.byteToUnsignedShort(buffer, off) + itemSeparator);// Serving
																		// Freq
		off += 2;
		sb.append((buffer[off] & 0xff) + itemSeparator);// Serving RSRP
		off += 1;
		sb.append((buffer[off] & 0xff) + itemSeparator);// Serving RSRQ
		off += 1;
		int neighborCellNum = ConvToByte.byteToUnsignedByte(buffer, off);
		sb.append(neighborCellNum + itemSeparator);// Neighbor Cell Number
		off += 1;
		// System.out.print("uemr sb1=============" + sb.toString());
		for (int n = 0; n < neighborCellNum; n++) {
			sb.append(ConvToByte.byteToUnsignedShort(buffer, off) + itemSeparator);// Neighbor
																			// 1
																			// Cell
																			// PCI
			off += 2;
			sb.append(ConvToByte.byteToUnsignedShort(buffer, off) + itemSeparator);// Neighbor
																			// 1
																			// Freq
			off += 2;
			sb.append(ConvToByte.byteToUnsignedByte(buffer, off) + itemSeparator);// Neighbor
																		// 1
																		// RSRP
			off += 1;
			sb.append(ConvToByte.byteToUnsignedByte(buffer, off) + itemSeparator);// Neighbor
																		// 1
																		// RSRQ
			off += 1;
		}
		buffer = null;
		if (time_flg) {
			sb.append(sdf.format(System.currentTimeMillis()));
		}else {
			sb.deleteCharAt(sb.lastIndexOf("|"));
		}
		String message= sb.toString();
		KeyedMessage<String, String> km = new KeyedMessage<String, String>(topicMap.get("3"),String.valueOf(message.hashCode()),message);
		// 如果消息队列满足sendmsgNum条向kafka队列缓存队列存储
		if (uE_MRmsgList.size() == uE_MRSsendmsgNum) {
			msg_queue.offer(uE_MRmsgList);
			uE_MRmsgList = new ArrayList<KeyedMessage<String, String>>();
		}
		uE_MRmsgList.add(km);
	}

	/**
	 * Cell_MR　解析 type =4
	 * 
	 * @param buffer_lte
	 * @param time_print_flg
	 */
	private void cell_MRAnalysis(byte[] buffer, int off, boolean time_print_flg) {

		StringBuilder sb = new StringBuilder();
		sb.append(ConvToByte.byteToUnsignedShort(buffer, off) + itemSeparator);// Length
		off += 2;
		sb.append(ConvToByte.decodeTBCD(buffer, off, off + 2, true) + itemSeparator);// City
		off += 2;
		sb.append(buffer[off] + itemSeparator);// Interface
		off += 1;
		sb.append(ConvToByte.getHexString(buffer, off, off + 16) + itemSeparator);// XDR
																		// ID
		off += 16;
		sb.append(ConvToByte.byteToUnsignedByte(buffer, off) + itemSeparator);// RAT
		off += 1;
		sb.append(ConvToByte.decodeTBCD(buffer, off, off + 8, true) + itemSeparator);// IMSI
		off += 8;
		sb.append(ConvToByte.decodeTBCD(buffer, off, off + 8, true) + itemSeparator);// IMEI
		off += 8;
		sb.append(ConvToByte.decodeTBCD(buffer, off, off + 16, false) + itemSeparator);// MSISDN
		off += 16;
		sb.append(ConvToByte.byteToUnsignedInt(buffer, off) + itemSeparator);// eNB ID
		off += 4;
		sb.append(ConvToByte.byteToUnsignedInt(buffer, off) + itemSeparator);// Cell ID
		off += 4;
		sb.append(ConvToByte.byteToLong(buffer, off) + itemSeparator);// Time
		off += 8;
		sb.append(ConvToByte.getHexString(buffer, off, 20) + itemSeparator);// eNB
																	// Received
																	// Interfere
		off += 20;
		sb.append(ConvToByte.getHexString(buffer, off, 9) + itemSeparator);// UL Packet
																	// Loss
		off += 9;
		sb.append(ConvToByte.getHexString(buffer, off, 9) + itemSeparator);// DL packet
		buffer = null;												 // Loss
		if (time_flg) {
			sb.append(sdf.format(System.currentTimeMillis()));
		}else {
			sb.deleteCharAt(sb.lastIndexOf("|"));
		}
		String message= sb.toString();
		KeyedMessage<String, String> km = new KeyedMessage<String, String>(topicMap.get("4"),String.valueOf(message.hashCode()),message);
		// 如果消息队列满足sendmsgNum条向kafka队列缓存队列存储
		if (cell_MRmsgList.size() == cell_MRSsendmsgNum) {
			msg_queue.offer(cell_MRmsgList);
			cell_MRmsgList = new ArrayList<KeyedMessage<String, String>>();
		}
		cell_MRmsgList.add(km);
	}

	/**
	 * Ｓ1_MME　解析　type =5
	 * @param buffer_lte
	 * @param totalContents 条数
	 * @param time_print_flg
	 */
	private void s1_MME_Analysis(byte[] buffer, int off, boolean time_print_flg) {
		StringBuilder sb = new StringBuilder();
		sb.append(ConvToByte.byteToUnsignedShort(buffer, off) + itemSeparator);// Length
		off += 2;
		sb.append(ConvToByte.decodeTBCD(buffer, off, off + 2, true) + itemSeparator);// City
		off += 2;
		sb.append(buffer[off] + itemSeparator);// Interface
		off += 1;
		sb.append(ConvToByte.getHexString(buffer, off, off + 16) + itemSeparator);// XDR
																		// ID
		off += 16;
		sb.append(ConvToByte.byteToUnsignedByte(buffer, off) + itemSeparator);// RAT
		off += 1;
		sb.append(ConvToByte.decodeTBCD(buffer, off, off + 8, true) + itemSeparator);// IMSI
		off += 8;
		sb.append(ConvToByte.decodeTBCD(buffer, off, off + 8, true) + itemSeparator);// IMEI
		off += 8;
		sb.append(ConvToByte.decodeTBCD(buffer, off, off + 16, false) + itemSeparator);// MSISDN
		off += 16;
		sb.append((buffer[off] & 0xff) + itemSeparator);// Procedure Type
		off += 1;
		sb.append(ConvToByte.byteToLong(buffer, off) + itemSeparator);// Procedure Start
															// Time
		off += 8;
		sb.append(ConvToByte.byteToLong(buffer, off) + itemSeparator);// Procedure End
															// Time
		off += 8;
		sb.append((buffer[off] & 0xff) + itemSeparator);// Procedure Status
		off += 1;
		sb.append(ConvToByte.byteToUnsignedShort(buffer, off) + itemSeparator);// Request
																		// Cause
		off += 2;
		sb.append(ConvToByte.byteToUnsignedShort(buffer, off) + itemSeparator);// Failure
																		// Cause
		off += 2;
		sb.append((buffer[off] & 0xff) + itemSeparator);// Keyword 1
		off += 1;
		sb.append((buffer[off] & 0xff) + itemSeparator);// Keyword 2
		off += 1;
		sb.append((buffer[off] & 0xff) + itemSeparator);// Keyword 3
		off += 1;
		sb.append((buffer[off] & 0xff) + itemSeparator);// Keyword 4
		off += 1;
		sb.append(ConvToByte.byteToUnsignedInt(buffer, off) + itemSeparator);// MME UE
																	// S1AP ID
		off += 4;
		sb.append(ConvToByte.byteToUnsignedShort(buffer, off) + itemSeparator);// Old MME
																		// Group
																		// ID
		off += 2;
		sb.append(ConvToByte.byteToUnsignedByte(buffer, off) + itemSeparator);// Old MME
																	// Code
		off += 1;
		sb.append(ConvToByte.getHexString(buffer, off, off + 4) + itemSeparator);// Old
																		// M-TMSI
		off += 4;
		sb.append(ConvToByte.byteToUnsignedShort(buffer, off) + itemSeparator);// MME
																		// Group
																		// ID
		off += 2;
		sb.append(ConvToByte.byteToUnsignedByte(buffer, off) + itemSeparator);// MME Code
		off += 1;
		sb.append(ConvToByte.getHexString(buffer, off, off + 4) + itemSeparator);// M-TMSI
		off += 4;
		sb.append(ConvToByte.getHexString(buffer, off, off + 4) + itemSeparator);// TMSI
		off += 4;
		sb.append(ConvToByte.getIpv4(buffer, off) + itemSeparator);// USER_IPv4
		off += 4;
		sb.append(ConvToByte.getIpv6(buffer, off) + itemSeparator);// USER_IPv6
		off += 16;
		sb.append(ConvToByte.getIp(buffer, off) + itemSeparator);// MME IP Add
		off += 16;
		sb.append(ConvToByte.getIp(buffer, off) + itemSeparator);// eNB IP Add
		off += 16;
		sb.append(ConvToByte.byteToUnsignedShort(buffer, off) + itemSeparator);// MME Port
		off += 2;
		sb.append(ConvToByte.byteToUnsignedShort(buffer, off) + itemSeparator);// eNB Port
		off += 2;
		sb.append(ConvToByte.byteToUnsignedShort(buffer, off) + itemSeparator);// TAC
		off += 2;
		sb.append(ConvToByte.byteToUnsignedInt(buffer, off) + itemSeparator);// Cell ID
		off += 4;
		sb.append(ConvToByte.byteToUnsignedShort(buffer, off) + itemSeparator);// Other
																		// TAC
		off += 2;
		sb.append(ConvToByte.byteToUnsignedInt(buffer, off) + itemSeparator);// Other ECI
		off += 4;
		sb.append(new String(buffer, off, 32).trim() + itemSeparator);// APN
		off += 32;

		int epsBearerNum = ConvToByte.byteToUnsignedByte(buffer, off);
		sb.append(epsBearerNum + itemSeparator);// EPS Bearer Number
		off += 1;
		for (int n = 0; n < epsBearerNum; n++) {
			sb.append(ConvToByte.byteToUnsignedByte(buffer, off) + itemSeparator);// Bearer
																		// 1 ID
			off += 1;
			sb.append(ConvToByte.byteToUnsignedByte(buffer, off) + itemSeparator);// Bearer
																		// 1
																		// Type
			off += 1;
			sb.append(ConvToByte.byteToUnsignedByte(buffer, off) + itemSeparator);// Bearer
																		// 1 QCI
			off += 1;
			sb.append(ConvToByte.byteToUnsignedByte(buffer, off) + itemSeparator);// Bearer
																		// 1
																		// Status
			off += 1;
			sb.append(ConvToByte.byteToUnsignedShort(buffer, off) + itemSeparator);// Bearer
																			// 1
																			// Request
																			// Cause
			off += 2;
			sb.append(ConvToByte.byteToUnsignedShort(buffer, off) + itemSeparator);// Bearer
																			// 1
																			// Failure
																			// Cause
			off += 2;
			sb.append(ConvToByte.byteToUnsignedInt(buffer, off) + itemSeparator);// Bearer
																		// 1 eNB
																		// GTP-TEID
			off += 4;
			sb.append(ConvToByte.byteToUnsignedInt(buffer, off) + itemSeparator);// Bearer
																		// 1 SGW
																		// GTP-TEID
			off += 4;
		}
		buffer = null;
		if (time_flg) {
			sb.append(sdf.format(System.currentTimeMillis()));
		}else {
			sb.deleteCharAt(sb.lastIndexOf(itemSeparator));
		}
		String message= sb.toString();
		KeyedMessage<String, String> km = new KeyedMessage<String, String>(topicMap.get("5"),String.valueOf(message.hashCode()),message);
		// 如果消息队列满足sendmsgNum条向kafka队列缓存队列存储
		if (s1_MMEmsgList.size() == s1_MMESsendmsgNum) {
			msg_queue.offer(s1_MMEmsgList);
			s1_MMEmsgList = new ArrayList<KeyedMessage<String, String>>();
		}
		s1_MMEmsgList.add(km);
	}
	
	/**
	 * sa6　解析 type =6
	 * @param buffer_lte
	 * @param totalContents
	 * @param time_print_flg
	 */
	private void sa6_Analysis(byte[] buffer,int off, boolean time_print_flg) {

		StringBuilder sb = new StringBuilder();
		sb.append(ConvToByte.byteToUnsignedShort(buffer, off) + itemSeparator);// Length
		off += 2;
		sb.append(ConvToByte.decodeTBCD(buffer, off, off + 2, true) + itemSeparator);// City
		off += 2;
		sb.append(buffer[off] + itemSeparator);// Interface
		off += 1;
		sb.append(ConvToByte.getHexString(buffer, off, off + 16) + itemSeparator);// XDR
																		// ID
		off += 16;
		sb.append(ConvToByte.byteToUnsignedByte(buffer, off) + itemSeparator);// RAT
		off += 1;
		sb.append(ConvToByte.decodeTBCD(buffer, off, off + 8, true) + itemSeparator);// IMSI
		off += 8;
		sb.append(ConvToByte.decodeTBCD(buffer, off, off + 8, true) + itemSeparator);// IMEI
		off += 8;
		sb.append(ConvToByte.decodeTBCD(buffer, off, off + 16, false) + itemSeparator);// MSISDN
		off += 16;
		sb.append((buffer[off] & 0xff) + itemSeparator);// Procedure Type
		off += 1;
		sb.append(ConvToByte.byteToLong(buffer, off) + itemSeparator);// Procedure Start
															// Time
		off += 8;
		sb.append(ConvToByte.byteToLong(buffer, off) + itemSeparator);// Procedure End
															// Time
		off += 8;
		sb.append((buffer[off] & 0xff) + itemSeparator);// Procedure Status
		off += 1;
		sb.append(ConvToByte.byteToUnsignedShort(buffer, off) + itemSeparator);// Cause
		off += 2;
		sb.append(ConvToByte.getIpv4(buffer, off) + itemSeparator);// USER_IPv4
		off += 4;
		sb.append(ConvToByte.getIpv6(buffer, off) + itemSeparator);// USER_IPv6
		off += 16;
		sb.append(ConvToByte.getIp(buffer, off) + itemSeparator);// MME Address
		off += 16;
		sb.append(ConvToByte.getIp(buffer, off) + itemSeparator);// HSS Address
		off += 16;
		sb.append(ConvToByte.byteToUnsignedShort(buffer, off) + itemSeparator);// MME Port
		off += 2;
		sb.append(ConvToByte.byteToUnsignedShort(buffer, off) + itemSeparator);// HSS Port
		off += 2;
		sb.append(ConvToByte.getHexString(buffer, off, 44) + itemSeparator);// Origin-Realm
		off += 44;
		sb.append(ConvToByte.getHexString(buffer, off, 44) + itemSeparator);// Destination-Realm
		off += 44;
		sb.append(ConvToByte.getHexString(buffer, off, 64) + itemSeparator);// Origin-Host
		off += 64;
		sb.append(ConvToByte.getHexString(buffer, off, 64) + itemSeparator);// Destination-Host
		off += 64;
		sb.append(ConvToByte.byteToUnsignedInt(buffer, 4) + itemSeparator);// Application-ID
		off += 4;
		sb.append((buffer[off] & 0xff) + itemSeparator);// Subscriber-Status
		off += 1;
		sb.append((buffer[off] & 0xff) + itemSeparator);// Access-Restriction-Data
		off += 1;
		buffer = null;
		if (time_flg) {
			sb.append(sdf.format(System.currentTimeMillis()));
		}else {
			sb.deleteCharAt(sb.lastIndexOf("|"));
		}
		String message= sb.toString();
		KeyedMessage<String, String> km = new KeyedMessage<String, String>(topicMap.get("6"),String.valueOf(message.hashCode()),message);
		// 如果消息队列满足sendmsgNum条向kafka队列缓存队列存储
		if (s6amsgList.size() == s6aSsendmsgNum) {
			msg_queue.offer(s6amsgList);
			s6amsgList = new ArrayList<KeyedMessage<String, String>>();
		}
		s6amsgList.add(km);
	}
	
	/**
	 * S11　解析 type =7
	 * @param buffer_lte
	 * @param totalContents
	 * @param time_print_flg
	 */
	private void s11_Analysis(byte[] buffer,int off, boolean time_print_flg) {

		StringBuilder sb = new StringBuilder();
		sb.append(ConvToByte.byteToUnsignedShort(buffer, off) + itemSeparator);// Length
		off += 2;
		sb.append(ConvToByte.decodeTBCD(buffer, off, off + 2, true) + itemSeparator);// City
		off += 2;
		sb.append(buffer[off] + itemSeparator);// Interface
		off += 1;
		sb.append(ConvToByte.getHexString(buffer, off, off + 16) + itemSeparator);// XDR
																		// ID
		off += 16;
		sb.append(ConvToByte.byteToUnsignedByte(buffer, off) + itemSeparator);// RAT
		off += 1;
		sb.append(ConvToByte.decodeTBCD(buffer, off, off + 8, true) + itemSeparator);// IMSI
		off += 8;
		sb.append(ConvToByte.decodeTBCD(buffer, off, off + 8, true) + itemSeparator);// IMEI
		off += 8;
		sb.append(ConvToByte.decodeTBCD(buffer, off, off + 16, false) + itemSeparator);// MSISDN
		off += 16;
		sb.append((buffer[off] & 0xff) + itemSeparator);// Procedure Type
		off += 1;
		sb.append(ConvToByte.byteToLong(buffer, off) + itemSeparator);// Procedure Start
															// Time
		off += 8;
		sb.append(ConvToByte.byteToLong(buffer, off) + itemSeparator);// Procedure End
															// Time
		off += 8;
		sb.append((buffer[off] & 0xff) + itemSeparator);// Procedure Status
		off += 1;
		sb.append(ConvToByte.byteToUnsignedShort(buffer, off) + itemSeparator);// Failure
																		// Cause
		off += 2;
		sb.append(ConvToByte.byteToUnsignedShort(buffer, off) + itemSeparator);// Request
																		// Cause
		off += 2;
		sb.append(ConvToByte.getIpv4(buffer, off) + itemSeparator);// USER_IPv4
		off += 4;
		sb.append(ConvToByte.getIpv6(buffer, off) + itemSeparator);// USER_IPv6
		off += 16;
		sb.append(ConvToByte.getIp(buffer, off) + itemSeparator);// MME Address
		off += 16;
		sb.append(ConvToByte.getIp(buffer, off) + itemSeparator);// SGW/Old MME Address
		off += 16;
		sb.append(ConvToByte.byteToUnsignedShort(buffer, off) + itemSeparator);// MME Port
		off += 2;
		sb.append(ConvToByte.byteToUnsignedShort(buffer, off) + itemSeparator);// SGW/Old
																		// MME
																		// Port
		off += 2;
		sb.append(ConvToByte.byteToUnsignedInt(buffer, off) + itemSeparator);// MME
																	// Control
																	// TEID
		off += 4;
		sb.append(ConvToByte.byteToUnsignedInt(buffer, off) + itemSeparator);// Old MME
																	// /SGW
																	// Control
																	// TEID
		off += 4;
		// System.out.print("s11 sb1=============" + sb.toString());
		sb.append(ConvToByte.getHexString2(buffer, off, off + 32) + itemSeparator); // APN
		// sb.append(ConvToByte.getHexString2(buffer, off, off + 32) + itemSeparator);
		// //APN
		// System.out.println("s11======"+sb.toString());
		// sb.append(new String(buffer,off,32).trim()+ itemSeparator);//APN
		// System.out.print("s11 sb2=============" + sb.toString());

		off += 32;
		int epsBearerNum = ConvToByte.byteToUnsignedByte(buffer, off);
		sb.append(epsBearerNum + itemSeparator);// EPS Bearer Number
		off += 1;
		for (int n = 0; n < epsBearerNum; n++) {
			sb.append(ConvToByte.byteToUnsignedByte(buffer, off) + itemSeparator);// Bearer
																		// 1 ID
			off += 1;
			sb.append(ConvToByte.byteToUnsignedByte(buffer, off) + itemSeparator);// Bearer
																		// 1
																		// Type
			off += 1;
			sb.append(ConvToByte.byteToUnsignedByte(buffer, off) + itemSeparator);// Bearer
																		// 1 QCI
			off += 1;
			sb.append(ConvToByte.byteToUnsignedByte(buffer, off) + itemSeparator);// Bearer
																		// 1
																		// Status
			off += 1;
			sb.append(ConvToByte.byteToUnsignedInt(buffer, off) + itemSeparator);// Bearer
																		// 1 eNB
																		// GTP-TEID
			off += 4;
			sb.append(ConvToByte.byteToUnsignedInt(buffer, off) + itemSeparator);// Bearer
																		// 1 SGW
																		// GTP-TEID
			off += 4;
		}
		buffer = null;
		if (time_flg) {
			sb.append(sdf.format(System.currentTimeMillis()));
		}else {
			sb.deleteCharAt(sb.lastIndexOf("|"));
		}
		String message= sb.toString();
		KeyedMessage<String, String> km = new KeyedMessage<String, String>(topicMap.get("7"),String.valueOf(message.hashCode()),message);
		// 如果消息队列满足sendmsgNum条向kafka队列缓存队列存储
		if (s11msgList.size() == s11SsendmsgNum) {
			msg_queue.offer(s11msgList);
			s11msgList = new ArrayList<KeyedMessage<String, String>>();
		}
		s11msgList.add(km);
	}
	
	/**
	 * SGｓ　解析 type =９
	 * @param buffer_lte
	 * @param totalContents
	 * @param time_print_flg
	 */
	private void sGs_Analysis(byte[] buffer, int off, boolean time_print_flg) {

		StringBuilder sb = new StringBuilder();
		sb.append(ConvToByte.byteToUnsignedShort(buffer, off) + itemSeparator);// Length
		off += 2;
		sb.append(ConvToByte.decodeTBCD(buffer, off, off + 2, true) + itemSeparator);// City
		off += 2;
		sb.append(buffer[off] + itemSeparator);// Interface
		off += 1;
		sb.append(ConvToByte.getHexString(buffer, off, off + 16) + itemSeparator);// XDR
																		// ID
		off += 16;
		sb.append(ConvToByte.byteToUnsignedByte(buffer, off) + itemSeparator);// RAT
		off += 1;
		sb.append(ConvToByte.decodeTBCD(buffer, off, off + 8, true) + itemSeparator);// IMSI
		off += 8;
		sb.append(ConvToByte.decodeTBCD(buffer, off, off + 8, true) + itemSeparator);// IMEI
		off += 8;
		sb.append(ConvToByte.decodeTBCD(buffer, off, off + 16, false) + itemSeparator);// MSISDN
		off += 16;
		sb.append((buffer[off] & 0xff) + itemSeparator);// Procedure Type
		off += 1;
		sb.append(ConvToByte.byteToLong(buffer, off) + itemSeparator);// Procedure Start
															// Time
		off += 8;
		sb.append(ConvToByte.byteToLong(buffer, off) + itemSeparator);// Procedure End
															// Time
		off += 8;
		sb.append((buffer[off] & 0xff) + itemSeparator);// Procedure Status
		off += 1;
		sb.append((buffer[off] & 0xff) + itemSeparator);// Sgs Cause
		off += 1;
		sb.append((buffer[off] & 0xff) + itemSeparator);// Reject Cause
		off += 1;
		sb.append((buffer[off] & 0xff) + itemSeparator);// CP Cause
		off += 1;
		sb.append((buffer[off] & 0xff) + itemSeparator);// RP Cause
		off += 1;
		sb.append(ConvToByte.getIpv4(buffer, off) + itemSeparator);// USER_IPv4
		off += 4;
		sb.append(ConvToByte.getIpv6(buffer, off) + itemSeparator);// USER_IPv6
		off += 16;
		sb.append(ConvToByte.getIp(buffer, off) + itemSeparator);// MME IP Add
		off += 16;
		sb.append(ConvToByte.getIp(buffer, off) + itemSeparator);// MSC Server OP Add
		off += 16;
		sb.append(ConvToByte.byteToUnsignedShort(buffer, off) + itemSeparator);// MME Port
		off += 2;
		sb.append(ConvToByte.byteToUnsignedShort(buffer, off) + itemSeparator);// MSC
																		// Server
																		// Port
		off += 2;
		sb.append((buffer[off] & 0xff) + itemSeparator);// Service Indicator
		off += 1;
		sb.append(ConvToByte.getHexString(buffer, off, 55) + itemSeparator);// MME Name
		off += 55;
		sb.append(ConvToByte.byteToUnsignedInt(buffer, off) + itemSeparator);// TMSI
		off += 4;
		sb.append(ConvToByte.byteToUnsignedShort(buffer, off) + itemSeparator);// New LAC
		off += 2;
		sb.append(ConvToByte.byteToUnsignedShort(buffer, off) + itemSeparator);// Old LAC
		off += 2;
		sb.append(ConvToByte.byteToUnsignedShort(buffer, off) + itemSeparator);// TAC
		off += 2;
		sb.append(ConvToByte.byteToUnsignedInt(buffer, off) + itemSeparator);// Cell ID
		off += 4;
		sb.append(ConvToByte.getHexString(buffer, off, 24) + itemSeparator);// Calling ID
		off += 24;
		sb.append((buffer[off] & 0xff) + itemSeparator);// VLR Name Length
		off += 1;
		sb.append(ConvToByte.getHexString(buffer, off, buffer.length) + itemSeparator);
		buffer = null;
		if (time_flg) {
			sb.append(sdf.format(System.currentTimeMillis()));
		}else {
			sb.deleteCharAt(sb.lastIndexOf(itemSeparator));
		}
		String message= sb.toString();
		KeyedMessage<String, String> km = new KeyedMessage<String, String>(topicMap.get("9"),String.valueOf(message.hashCode()),message);
		// 如果消息队列满足sendmsgNum条向kafka队列缓存队列存储
		if (sGsmsgList.size() == sGsSsendmsgNum) {
			msg_queue.offer(sGsmsgList);
			sGsmsgList = new ArrayList<KeyedMessage<String, String>>();
		}
		sGsmsgList.add(km);
	}
	
	/**
	 * 根据接口型类ID取topic名字
	 * @return
	 */
	private HashMap<String, String> propAnalysis() {
		HashMap<String, String> topicMap = new HashMap<String, String>();
		// msgType_100=topic_lte_common
		Set<Entry<Object, Object>> entrySet = prop.entrySet();
		Iterator<Entry<Object, Object>> it = entrySet.iterator();
		while (it.hasNext()) {
			Entry<Object, Object> kvEntry = it.next();
			String key = (String) kvEntry.getKey();
			if ((key.trim()).startsWith("msgType_")) {
				// value：装载topic 名字
				String value = (String) kvEntry.getValue();
				String[] keyvalue = key.split("_");
				// 100 --> topic_lte_common
				topicMap.put(keyvalue[1], value);
			}
		}
		return topicMap;
	}
}