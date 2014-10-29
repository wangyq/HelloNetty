package com.mysocket.http;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.sun.org.apache.bcel.internal.generic.NEW;

public class EchoProtocol implements Runnable {
	private static final int BUFSIZE = 65535; // Size (in bytes) of I/O buffer
	private Socket clntSock; // Socket connect to client
	private Logger logger; // Server logger

	public EchoProtocol(Socket clntSock, Logger logger) {
		this.clntSock = clntSock;
		this.logger = logger;
	}

	private void readData(InputStream in) throws IOException{
		int recvMsgSize; // Size of received message
		int totalBytesEchoed = 0; // Bytes received from client
		byte[] echoBuffer = new byte[BUFSIZE]; // Receive Buffer
		// Receive until client closes connection, indicated by -1
		
		//while ((recvMsgSize = in.read(echoBuffer)) != -1) {
		if( (recvMsgSize = in.read(echoBuffer)) != -1 ){ //只读取一次
			System.out.println(new String(echoBuffer));
			//out.write(echoBuffer, 0, recvMsgSize);
			totalBytesEchoed += recvMsgSize;
		}
	}
	
	private void writeData(OutputStream out) throws IOException{
		String strHttp1_1_200OK = "HTTP/1.1 200 OK \r\n\r\n";  //HTTP报文头部
		strHttp1_1_200OK += "Hello,world! my socket http server!";
		out.write(strHttp1_1_200OK.getBytes());
	}
	
	public void handleEchoClient() {
		InputStream in = null;
		OutputStream out = null;
		
		try {
			// Get the input and output I/O streams from socket
			in = clntSock.getInputStream();
			out = clntSock.getOutputStream();

			//先读入内容
			readData(in);
			//然后写入回应
			writeData(out);

		} catch (IOException ex) {
			logger.log(Level.WARNING, "Exception in echo protocol", ex);
		}
		finally{
			//关闭输入输出流
			try {
				if( null != in ) in.close();
				if( null!= out ) out.close();
			} catch (Exception e) {
				// TODO: handle exception
			}
		}
	}

	/**
	 * 线程函数开始执行的起点
	 */
	public void run() {
		try {
			//进行读写处理
			handleEchoClient();
			
		} catch (Exception e) {
			// TODO: handle exception
		} finally {
			try {
				//保证关闭socket
				//否则下次的连接会被拒绝
				clntSock.close();
			} catch (IOException e) {
			}
		}

	}
}
