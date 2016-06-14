package com.asiainfo.ocdc.lte.process;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;
import java.util.concurrent.LinkedBlockingQueue;

public class serverss {

	/**
	 * 
	 */
	private static ServerSocket serverSocket;
//	private static LinkedBlockingQueue<SocketChannel> lbkSKey = new  LinkedBlockingQueue<SocketChannel>();
//	private static Selector selector;
	public static void main(String[] args) throws InterruptedException {

//		try {
//			// --------------------单客户端-----------------------------------------
//			// 打开监听信道
//			// ServerSocketChannel listenerChannel = ServerSocketChannel.open();
//			// // 与本地端口绑定
//			// listenerChannel.socket().bind(new InetSocketAddress(10112));
//			// // 设置为非阻塞模式
//			// listenerChannel.configureBlocking(false);
//			//
//			// // 创建选择器
//			// Selector selector = Selector.open();
//			// // 将选择器绑定到监听信道,只有非阻塞信道才可以注册选择器.并在注册过程中指出该信道可以进行Accept操作
//			// listenerChannel.register(selector, SelectionKey.OP_ACCEPT);
//			// // 超时时间，单位毫秒
//			// int TimeOut = 10000;
//			// // 等待某信道就绪(或超时)
//			// if (selector.select(TimeOut) == 0) {
//			// System.out.print("等待客户端信道就绪！");
//			// }
//			// // 获取客户端SocketChannel
//			// SocketChannel clientChannel= listenerChannel.accept();
//			// clientChannel.configureBlocking(false);
//			// ByteBuffer buffer = ByteBuffer.allocate(1024);
//			// // 得到并清空缓冲区
//			// System.out.println(buffer.capacity());
//			// System.out.println(buffer.position());
//			// System.out.println(buffer.limit());
//			// buffer.clear();
//			// // 读取信息获得读取的字节数
//			// clientChannel.read(buffer);
//			// buffer.flip();
//			// byte[] bt = new byte[buffer.limit()];
//			// buffer.get(bt);
//			// System.out.println("context:"+new String(bt));
//			// } catch (IOException e) {
//			// e.printStackTrace();
//			// }
//
//			// ---------------------多客户端----------------------------------------
//			
//			
//			// 打开监听信道
//			ServerSocketChannel serverSocketChannel = ServerSocketChannel.open();
//			// 与本地端口绑定
//			serverSocketChannel.socket().bind(new InetSocketAddress(10112));
//			// 设置为非阻塞模式
//			serverSocketChannel.configureBlocking(false);
//			ByteBuffer sendbuffer = ByteBuffer.allocate(1024);
//			// 创建选择器
//			selector = Selector.open();
//			// 将选择器绑定到监听信道,只有非阻塞信道才可以注册选择器.并在注册过程中指出该信道可以进行Accept操作
//			//// 注册到selector，等待连接
//			serverSocketChannel.register(selector, SelectionKey.OP_ACCEPT);
//			System.out.println("Server Start----:");
//			// 超时时间，单位毫秒
////			int TimeOut = 8000;
//
//			int lnkedid = 1;
//			// 反复循环,等待IO
//			while (true) {
//				// 等待某信道就绪(或超时(论循探测时间))  选择一组键，并且相应的通道已经打开
//				if (selector.select() == 0) {
//					System.out.println("等待客户端信道就绪！");
//					continue;
//				} else {
//					System.out.println("链路ID：" + lnkedid++);
//				}
//
//				// 取得迭代器.selectedKeys()中包含了每个准备好某一I/O操作的信道的SelectionKey
//				Iterator<SelectionKey> iterator = selector.selectedKeys().iterator();
//
//				while (iterator.hasNext()) {
//					SelectionKey key = iterator.next();
//					// 移除处理过的键
//					iterator.remove();
//					
//					if (key.isAcceptable()) {
//						// 有客户端连接请求时
//						SocketChannel clientChannel = ((ServerSocketChannel) key.channel()).accept();
//						clientChannel.configureBlocking(false);
//						clientChannel.register(key.selector(), SelectionKey.OP_READ,ByteBuffer.allocate(1024));
//					}
//					if (key.isReadable()) {
//						// 从客户端读取数据
//						// 获得与客户端通信的信道
//						SocketChannel clientChannel = (SocketChannel) key.channel();
//						// 得到并清空缓冲区
//						ByteBuffer buffer = (ByteBuffer) key.attachment();
//						buffer.clear();
//						// 读取信息获得读取的字节数
//						long bytesRead = clientChannel.read(buffer);
//						if (bytesRead == -1) {
//							System.out.println("客户端socket关闭！");
//							clientChannel.close();
//						} else if (bytesRead > 1) {
////							clientChannel.register(selector, SelectionKey.OP_WRITE);
//							System.out.println("readSize:=====" + bytesRead);
//							buffer.flip();
//							byte[] bt = new byte[buffer.limit()];
//							buffer.get(bt);
//							System.out.println("context:" + new String(bt));
//							sendbuffer.clear();
//							sendbuffer.put("shoudao!".getBytes());
//							sendbuffer.flip();
//							clientChannel.write(sendbuffer);
//						
//						}
////						key.interestOps(SelectionKey.OP_READ | SelectionKey.OP_WRITE);
////						lbkSKey.offer(clientChannel);
////						System.out.println("lbkSKey Size:"+lbkSKey.size()+ "bytesRead:"+bytesRead);
////						new Thread(new SelectKeyProcess(lbkSKey,buffer)).start();
//						
//					} if (key.isValid() && key.isWritable()) {
//						// 客户端可写时
//						// 获得与客户端通信的信道
////						SocketChannel clientChannel = (SocketChannel) key.channel();
////						// 得到并清空缓冲区
////						ByteBuffer buffer = (ByteBuffer) key.attachment();
//					}
//			
//				}
//			}
//		} catch (IOException e) {
//			e.printStackTrace();
//		}

		// -----------------------------------------------------------
		 try {
		 serverSocket = new ServerSocket(10112);
		 Socket socket = serverSocket.accept();
		
		 // Thread.currentThread().sleep(532232366000l);
		 ObjectInputStream is = new ObjectInputStream(new BufferedInputStream(socket.getInputStream()));
		 long time = System.currentTimeMillis();
		 System.out.println("start:"+ time);
		 // socket.shutdownOutput();
		 int index =0;
		 while(true){
			try {
				String obj = (String)is.readObject();
				index++;
				System.out.println("index:"+index+ ":"+obj);
			} catch (ClassNotFoundException e) {
				e.printStackTrace();
			}
		 }
		 
		 } catch (IOException e) {
		 e.printStackTrace();
		 }
	}
	
//    // 监听
//    private void listen() throws IOException {
//        while (true) {
//            // 选择一组键，并且相应的通道已经打开
//            selector.select();
//            // 返回此选择器的已选择键集。
//            Set<SelectionKey> selectionKeys = selector.selectedKeys();
//            Iterator<SelectionKey> iterator = selectionKeys.iterator();
//            while (iterator.hasNext()) {
//                SelectionKey selectionKey = iterator.next();
//                iterator.remove();
//                handleKey(selectionKey);
//            }
//        }
//    }
    

}


class SelectKeyProcess implements Runnable{
	private LinkedBlockingQueue<SocketChannel> lbkSKey =null;
	private ByteBuffer buffer = null;
	public SelectKeyProcess(LinkedBlockingQueue<SocketChannel> lbkSKey,ByteBuffer buffer) {
		this.lbkSKey = lbkSKey;
		this.buffer = buffer;
	}
	public void run() {
		try {
			while (true){
				SocketChannel clientChannel = lbkSKey.take();
				System.out.println("lbkSKey Size:"+lbkSKey.size());
				buffer.clear();
				// 读取信息获得读取的字节数
				long bytesRead = clientChannel.read(buffer);
				if (bytesRead == -1) {
					System.out.println("客户端socket关闭！");
					clientChannel.close();
				} else {
					System.out.println("readSize:=====" + bytesRead);
					buffer.flip();
					byte[] bt = new byte[buffer.limit()];
					buffer.get(bt);
					System.out.println("context:" + new String(bt));
				
				}
				
				
			
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
}