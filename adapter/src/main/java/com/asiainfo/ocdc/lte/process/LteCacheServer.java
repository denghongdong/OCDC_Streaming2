package com.asiainfo.ocdc.lte.process;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.security.MessageDigest;
import java.util.Arrays;
import java.util.Iterator;
import java.util.Set;
import java.util.concurrent.LinkedBlockingQueue;

public class LteCacheServer {

	/* 缓冲区大小 */
	// 10485760（100M）
	private int BLOCK = 104857600;
	/* 接受数据缓冲区 */
	private ByteBuffer sendbuffer = ByteBuffer.allocate(BLOCK);
	/* 发送数据缓冲区 */
	private ByteBuffer receivebuffer = ByteBuffer.allocate(BLOCK);
	private Selector selector;
	private LinkedBlockingQueue<byte[]> lbkAllMsg;
	public static long msgCount = 0l;
	private boolean msgCount_flg_start = false;
	public LteCacheServer(LinkedBlockingQueue<byte[]> lbkAllMsg, int port) throws IOException {
		this.lbkAllMsg = lbkAllMsg;
		// 打开服务器套接字通道
		ServerSocketChannel serverSocketChannel = ServerSocketChannel.open();
		// 服务器配置为非阻塞
		serverSocketChannel.configureBlocking(false);
		// 检索与此通道关联的服务器套接字
		ServerSocket serverSocket = serverSocketChannel.socket();
		// 进行服务的绑定
		serverSocket.bind(new InetSocketAddress(port));
		// 通过open()方法找到Selector
		selector = Selector.open();
		// 注册到selector，等待连接
		serverSocketChannel.register(selector, SelectionKey.OP_ACCEPT);
	}
	
	/**
	 * 设定是否定时打印条数
	 * @param msgCount_flg_start
	 */
	public void setPrintFlg(boolean msgCount_flg_start){
		this.msgCount_flg_start = msgCount_flg_start;
	}
	// 监听
	public void listen() throws IOException {
		while (true) {
			// 选择一组键，并且相应的通道已经打开
			if (selector.select() == 0)
				continue;
			// 返回此选择器的已选择键集。
			Set<SelectionKey> selectionKeys = selector.selectedKeys();
//			for (SelectionKey key:selector.selectedKeys()) {
//				selector.selectedKeys().remove(key);
//				handleKey(key);
//			}
			Iterator<SelectionKey> iterator = selectionKeys.iterator();
			while (iterator.hasNext()) {
				SelectionKey selectionKey = iterator.next();
				iterator.remove();
				handleKey(selectionKey);
			}
		}
	}

	private boolean is_login = false;
	private long receiveNum = 0l;
	private boolean partPackage_flg = false;
	private int partPackage_size = 0;
	// SDTP 除掉总长度之外的信息
	private byte[] byteContent = null;
	private byte[] lengthByte = null;
	private boolean partlength_flg = false;
	
	// 处理请求
	private void handleKey(SelectionKey selectionKey) throws IOException {
		// 接受请求
		ServerSocketChannel server = null;
		SocketChannel client = null;

		// 测试此键的通道是否已准备好接受新的套接字socket连接。
		if (selectionKey.isAcceptable()) {
			// 服务端信道
			server = (ServerSocketChannel) selectionKey.channel();
			client = server.accept();
			// 设置关闭时不立即关闭连接，等数据发送完成或3秒后再关闭
			client.socket().setSoLinger(true, 3);
			// 配置为非阻塞
			client.configureBlocking(false);
			// 客户端信息
			String ipinfo = "[" + client.socket().getInetAddress().getHostName() + "]";
			// 注册到selector，等待连接
			client.register(selector, SelectionKey.OP_READ, ipinfo);
		} else if (selectionKey.isReadable()) {
			// 客户端通道
			client = (SocketChannel) selectionKey.channel();
			String hostinfo = (String) selectionKey.attachment();
			
			// 将缓冲区清空
			receivebuffer.clear();
			int result = client.read(receivebuffer);
			if (result > 0) {
				receivebuffer.flip();
				int messageType = 0;
				int sequenceId = 0;
				byte totalContents = 0;
				while (true) {
					if (receivebuffer.remaining() == 0){
						break;
					} else if (receivebuffer.remaining() == 1){
						// 信道中只有一个字节
						if (lengthByte == null){
							lengthByte = new byte[2];
							receivebuffer.get(lengthByte, 0, 1);
							partlength_flg = true;
						} else if (partlength_flg){
							receivebuffer.get(lengthByte, 1, 1);
							int sdtpSize = ConvToByte.byteToShort(lengthByte, 0);
							// SDTP协议包长度-[总长度]占位
							int eLengthSdtpSize = sdtpSize - 2;
							byteContent = new byte[eLengthSdtpSize];
							partlength_flg = false;
						}else {
							// sdtp
							receivebuffer.get(byteContent, partPackage_size, 1);
							// 记录断包信息
							partPackage_size = partPackage_size + 1;
							// 读取断包数据
							if (partPackage_size == byteContent.length){
								messageType = ConvToByte.byteToShort(byteContent, 0);
								sequenceId = ConvToByte.byteToInt(byteContent, 2);
								totalContents = byteContent[6];
								lbkAllMsg.offer(byteContent);
								requestFun(messageType,sequenceId,totalContents,client);
								initPoint();
							}else {
								partPackage_flg = true;
							}
						}
						break;
					} else {
						// elementsSize > 1
						if (lengthByte == null){
							lengthByte = new byte[2];
							receivebuffer.get(lengthByte, 0, 2);
							int sdtpSize = ConvToByte.byteToShort(lengthByte, 0);
							int eLengthSdtpSize = sdtpSize - 2;
							byteContent = new byte[eLengthSdtpSize];
						}else {
							if (partlength_flg) {
								// 读入length剩余的一个字节,其他要读入到body中去。
								receivebuffer.get(lengthByte, 1, 1);
								int sdtpSize = ConvToByte.byteToShort(lengthByte, 0);
								// SDTP协议包长度-[总长度]占位
								int eLengthSdtpSize = sdtpSize - 2;
								byteContent = new byte[eLengthSdtpSize];
								partlength_flg =false;
							}else {
								// 全部是包体
							}
						}
						// 无断包
						if (!partPackage_flg){
							// 检查缓冲区内是否还够一个完整的SDTP协议包
							if (receivebuffer.remaining() >= byteContent.length){
								// 取SDTP协议包(除掉总长度占位)
								receivebuffer.get(byteContent, 0, byteContent.length);
								messageType = ConvToByte.byteToShort(byteContent, 0);
								sequenceId = ConvToByte.byteToInt(byteContent, 2);
								totalContents = byteContent[6];
								lbkAllMsg.offer(byteContent);
								requestFun(messageType,sequenceId,totalContents,client);
								initPoint();
							}else {
								// 记录断包信息
								partPackage_size = receivebuffer.remaining();
								// 读取断包数据
								receivebuffer.get(byteContent, 0, partPackage_size);
								partPackage_flg = true;
								break;
							}
						}else {
							// 断包处理
							// 断包剩余长度
							int shengyuSize = byteContent.length - partPackage_size;
							if (receivebuffer.remaining() >= shengyuSize){
								// 读取剩余断包数据
								receivebuffer.get(byteContent, partPackage_size, shengyuSize);
								messageType = ConvToByte.byteToShort(byteContent, 0);
								sequenceId = ConvToByte.byteToInt(byteContent, 2);
								totalContents = byteContent[6];
								lbkAllMsg.offer(byteContent);
								requestFun(messageType,sequenceId,totalContents,client);
								initPoint();
							} else {
								// 二次断包情况
								int partPackage_size_n = receivebuffer.remaining();
								receivebuffer.get(byteContent, partPackage_size, partPackage_size_n);
								partPackage_size = partPackage_size + partPackage_size_n;
								partPackage_flg = true;
								break;
							}
						}
					}
				}
			} else if (result == -1) {
				initSocketParam();
				System.out.println("socket 客户端" + hostinfo + "关闭联接！");
				selectionKey.cancel();  
                   if (selectionKey.channel() != null) {  
                	   selectionKey.channel().close();  
                   }  
			}
		}
	}
	
	private void initSocketParam() {
		is_login = false;
		receiveNum = 0l;
		initPoint();
	}
	
	private void initPoint () {
		partPackage_flg = false;
		partPackage_size = 0;
		byteContent = null;
		lengthByte = null;
		partlength_flg = false;
	}
	
	
	private void requestFun( int messageType,int sequenceId,byte totalContents,SocketChannel client) {
		byte[] requestArray = null;
		int off = 7;
		if (is_login && messageType == 5) {
			if (msgCount_flg_start){
				msgCount++;
			}
			// sdtp数据校验用
			receiveNum = receiveNum + totalContents;
			// 数据应答
//			requestArray = responseNotifyEventData(sequenceId,totalContents);
		} else if (is_login && messageType == 3) {// linkCheck 链路检测
			System.out.println("linkCheck 链路检测 messageType:"+messageType);
			requestArray = responseLinkCheck(sequenceId, totalContents);
		} else if (is_login && messageType == 7) {// linkDataCheck
													// 链路数据发送校验
			
			System.out.println("链路数据发送校验 messageType:"+messageType);
			int sendflag = ConvToByte.byteToInt(byteContent, off);
			off += 4;
			int sendDataInfo = ConvToByte.byteToInt(byteContent, off);
			requestArray = responseLinkDataCheck(sequenceId,totalContents, sendflag, sendDataInfo,(int) receiveNum);
			receiveNum = 0;
		} else if (messageType == 1) {// verNego 版本协商
			System.out.println("verNego 版本协商 messageType:"+messageType);
			requestArray = responseVerNego(sequenceId, totalContents);
		} else if (messageType == 2) {// linkAuth 权限验证
			byte[] loginId = new byte[12];
			byte[] digestArray = new byte[16];
			byte[] timestamp = new byte[4];
			byte[] rand = new byte[2];
			System.arraycopy(byteContent, off, loginId, 0, 12);
			off += 12;
			System.arraycopy(byteContent, off, digestArray, 0, 16);
			off += 16;
			System.arraycopy(byteContent, off, timestamp, 0, 4);
			off += 4;
			System.arraycopy(byteContent, off, rand, 0, 2);
			requestArray = responseLinkAuth(sequenceId, totalContents,
					loginId, digestArray, timestamp, rand);
			is_login = true;
			System.out.println("linkAuth 权限验证 messageType:" + messageType);
		} else if (messageType == 4) {// linkRel 连接释放
			requestArray = responseLinkRel(sequenceId, totalContents);
			System.out.println("linkRel 连接释放 messageType:"+messageType);
		} else {
			 System.out.println("未验证权限或者messageType不是1，2，3, 7 messageType:"+messageType);
		}
		
		if (requestArray != null) {
			// 将缓冲区清空以备下次写入
			sendbuffer.clear();
			// 向缓冲区中输入数据
			sendbuffer.put(requestArray);
			// 将缓冲区各标志复位,因为向里面put了数据标志被改变要想从中读取数据发向服务器,就要复位
			sendbuffer.flip();
			// 输出到通道
			try {
				client.write(sendbuffer);
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}
	
//	private byte[] responseNotifyEventData(int sequenceId, byte totalContents) {
//		short totalLength = 10;
//		int messageType = 0x8005;
//		byte reslut = 1;
//
//		byte[] totalLengthArray = ConvToByte.shortToByte(totalLength);
//		byte[] messageTypeArray = ConvToByte.intToByte(messageType);
//		byte[] sequenceIdArray = ConvToByte.intToByte(sequenceId);
//
//		byte[] requestArray = new byte[totalLength];
//		int pos = 0;
//		System.arraycopy(totalLengthArray, 0, requestArray, pos, 2);
//		pos += 2;
//		System.arraycopy(messageTypeArray, 2, requestArray, pos, 2);
//		pos += 2;
//		System.arraycopy(sequenceIdArray, 0, requestArray, pos, 4);
//		pos += 4;
//		requestArray[pos] = totalContents;
//		pos++;
//		requestArray[pos] = reslut;
//		return requestArray;
//	}

	private byte[] responseVerNego(int sequenceId, byte totalContents) {
		short totalLength = 10;
		int messageType = 0x8001;
		byte reslut = 1;

		byte[] totalLengthArray = ConvToByte.shortToByte(totalLength);
		byte[] messageTypeArray = ConvToByte.intToByte(messageType);
		byte[] sequenceIdArray = ConvToByte.intToByte(sequenceId);

		byte[] requestArray = new byte[totalLength];
		int pos = 0;
		System.arraycopy(totalLengthArray, 0, requestArray, pos, 2);
		pos += 2;
		System.arraycopy(messageTypeArray, 2, requestArray, pos, 2);
		pos += 2;
		System.arraycopy(sequenceIdArray, 0, requestArray, pos, 4);
		pos += 4;
		requestArray[pos] = totalContents;
		pos++;
		requestArray[pos] = reslut;
		return requestArray;
	}

	private byte[] responseLinkAuth(int sequenceId, byte totalContents,
			byte[] loginId, byte[] digestArray, byte[] timestamp, byte[] rand) {
		short totalLength = 74;
		int messageType = 0x8002;
		byte reslut = 0;
		String passwd = "asiainfo123";

		byte[] totalLengthArray = ConvToByte.shortToByte(totalLength);
		byte[] messageTypeArray = ConvToByte.intToByte(messageType);
		byte[] sequenceIdArray = ConvToByte.intToByte(sequenceId);

		int pos = 0;
		try {
			// 获取MD5编码器
			MessageDigest md = MessageDigest.getInstance("SHA-256");
			// 计算密码对应的SHA256编码值
			byte[] sha_passwd = md.digest(passwd.getBytes());
			StringBuilder sb = new StringBuilder();
			for (byte b : sha_passwd)
				sb.append(String.format("%02x", b));
			// 拼接临时字段，以计算digest，和客户端发送的值比对

			String str_tmp = (new String(loginId)) + sb.toString()
					+ ConvToByte.byteToInt(timestamp, 0) + "rand="
					+ ConvToByte.byteToShort(rand, 0);

			// 计算digest
			byte[] digest = md.digest(str_tmp.getBytes());
			// 比对计算出的digest和客户端发送过来的digest是否相等，相等返回1，否则返回0
			reslut = Arrays.equals(digest, digestArray) ? (byte) 1 : (byte) 0;

			sb.delete(0, sb.length());
			for (byte b : digestArray)
				sb.append(String.format("%02x", b));
			sb.delete(0, sb.length());
			for (byte b : digest)
				sb.append(String.format("%02x", b));

			reslut = 1;// 屏蔽验证，测试数据接收
		} catch (Exception e) {
			e.printStackTrace();
		}

		byte[] requestArray = new byte[totalLength];
		pos = 0;
		System.arraycopy(totalLengthArray, 0, requestArray, pos, 2);
		pos += 2;
		System.arraycopy(messageTypeArray, 2, requestArray, pos, 2);
		pos += 2;
		System.arraycopy(sequenceIdArray, 0, requestArray, pos, 4);
		pos += 4;
		requestArray[pos] = totalContents;
		pos++;
		requestArray[pos] = reslut;
		pos++;
		System.arraycopy(digestArray, 0, requestArray, pos, digestArray.length);

		return requestArray;
	}

	private byte[] responseLinkCheck(int sequenceId, byte totalContents) {
		short totalLength = 9;
		int messageType = 0x8003;

		byte[] totalLengthArray = ConvToByte.shortToByte(totalLength);
		byte[] messageTypeArray = ConvToByte.intToByte(messageType);
		byte[] sequenceIdArray = ConvToByte.intToByte(sequenceId);

		byte[] requestArray = new byte[totalLength];
		int pos = 0;
		System.arraycopy(totalLengthArray, 0, requestArray, pos, 2);
		pos += 2;
		System.arraycopy(messageTypeArray, 2, requestArray, pos, 2);
		pos += 2;
		System.arraycopy(sequenceIdArray, 0, requestArray, pos, 4);
		pos += 4;
		requestArray[pos] = totalContents;

		System.out.println("LinkCheck ");
		return requestArray;
	}

	private byte[] responseLinkRel(int sequenceId, byte totalContents) {
		short totalLength = 10;
		int messageType = 0x8004;
		byte reslut = 1;

		byte[] totalLengthArray = ConvToByte.shortToByte(totalLength);
		byte[] messageTypeArray = ConvToByte.intToByte(messageType);
		byte[] sequenceIdArray = ConvToByte.intToByte(sequenceId);

		byte[] requestArray = new byte[totalLength];
		int pos = 0;
		System.arraycopy(totalLengthArray, 0, requestArray, pos, 2);
		pos += 2;
		System.arraycopy(messageTypeArray, 2, requestArray, pos, 2);
		pos += 2;
		System.arraycopy(sequenceIdArray, 0, requestArray, pos, 4);
		pos += 4;
		requestArray[pos] = totalContents;
		pos++;
		requestArray[pos] = reslut;
		return requestArray;
	}

	private byte[] responseLinkDataCheck(int sequenceId, byte totalContents,
			int sendflag, int sendDataInfo, int receiveDatainfo) {
		short totalLength = 22;
		int messageType = 0x8007;
		byte reslut = 0;
		byte[] totalLengthArray = ConvToByte.shortToByte(totalLength);
		byte[] messageTypeArray = ConvToByte.intToByte(messageType);
		byte[] sequenceIdArray = ConvToByte.intToByte(sequenceId);
		byte[] sendflagArray = ConvToByte.intToByte(sendflag);
		byte[] sendDataInfoArray = ConvToByte.intToByte(sendDataInfo);
		byte[] recciveDatainfoArray = ConvToByte.intToByte(receiveDatainfo);

		if (sendDataInfo == receiveDatainfo) {
			reslut = 0;
		} else if (sendDataInfo > receiveDatainfo) {
			reslut = 1;
		} else if (sendDataInfo < receiveDatainfo) {
			reslut = 2;
		}

		byte[] requestArray = new byte[totalLength];
		int pos = 0;
		System.arraycopy(totalLengthArray, 0, requestArray, pos, 2);
		pos += 2;
		System.arraycopy(messageTypeArray, 2, requestArray, pos, 2);
		pos += 2;
		System.arraycopy(sequenceIdArray, 0, requestArray, pos, 4);
		pos += 4;
		requestArray[pos] = totalContents;
		pos++;
		System.arraycopy(sendflagArray, 0, requestArray, pos, 4);
		pos += 4;
		requestArray[pos] = reslut;
		pos++;
		System.arraycopy(sendDataInfoArray, 0, requestArray, pos, 4);
		pos += 4;
		System.arraycopy(recciveDatainfoArray, 0, requestArray, pos, 4);
		return requestArray;
	}
}