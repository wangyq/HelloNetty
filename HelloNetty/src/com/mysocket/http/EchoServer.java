package com.mysocket.http;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.logging.Logger;

public class EchoServer {

	public static final int MAX_ECHO_SERVER_WORKER_THREAD = 2;
	
	/**
	 * 计数器
	 */
	private static int count = 0;
	/**
	 * 取得系统的当前时间
	 * 
	 * @return
	 */
	public static String getCurTime() {
		SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");// 设置日期格式
		// System.out.println(df.format(new Date()));// new Date()为获取当前系统时间
		return df.format(new Date());
	}
	
	public static void main(String[] args) throws IOException {
		//
		// if (args.length != 1) { // Test for correct # of args
		// throw new IllegalArgumentException("Parameter(s): <Port>");
		// }

		//使用的端口号
		int echoServPort = 8080; // Integer.parseInt(args[0]); // Server port

		// Create a server socket to accept client connection requests
		ServerSocket servSock = new ServerSocket(echoServPort);

		Logger logger = Logger.getLogger("practical");

		// 指定worker线程(对客户端进行数据收发线程)的数目。
		Executor service = Executors
				.newFixedThreadPool(MAX_ECHO_SERVER_WORKER_THREAD); // Dispatch
																	// svc
		System.out.println(count + " [" + getCurTime()
				+ "]: Socket Server get ready at Port:" + echoServPort
				+ " for connection. \r\n\t\t\tThreadPoolSize="
				+ MAX_ECHO_SERVER_WORKER_THREAD);
		
		// Run forever, accepting and spawning threads to service each
		// connection
		while (true) {
			Socket clntSock = servSock.accept(); // Block waiting for connection

			count++;
			System.out.println(count + " [" + getCurTime()
					+ "]: Received a connetion from "
					+ clntSock.getRemoteSocketAddress());

			service.execute(new EchoProtocol(clntSock, logger));
		}
		/* NOT REACHED */
	}

}
