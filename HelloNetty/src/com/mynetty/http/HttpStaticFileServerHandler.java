/*
 * Copyright 2009 Red Hat, Inc.
 *
 * Red Hat licenses this file to you under the Apache License, version 2.0
 * (the "License"); you may not use this file except in compliance with the
 * License.  You may obtain a copy of the License at:
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.  See the
 * License for the specific language governing permissions and limitations
 * under the License.
 */
package com.mynetty.http;

import static org.jboss.netty.handler.codec.http.HttpHeaders.*;
import static org.jboss.netty.handler.codec.http.HttpHeaders.Names.*;
import static org.jboss.netty.handler.codec.http.HttpMethod.*;
import static org.jboss.netty.handler.codec.http.HttpResponseStatus.*;
import static org.jboss.netty.handler.codec.http.HttpVersion.*;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.RandomAccessFile;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.util.Iterator;
import java.util.Set;

import org.jboss.netty.buffer.ChannelBuffer;
import org.jboss.netty.buffer.ChannelBuffers;
import org.jboss.netty.channel.Channel;
import org.jboss.netty.channel.ChannelFuture;
import org.jboss.netty.channel.ChannelFutureListener;
import org.jboss.netty.channel.ChannelFutureProgressListener;
import org.jboss.netty.channel.ChannelHandlerContext;
import org.jboss.netty.channel.DefaultFileRegion;
import org.jboss.netty.channel.ExceptionEvent;
import org.jboss.netty.channel.FileRegion;
import org.jboss.netty.channel.MessageEvent;
import org.jboss.netty.channel.SimpleChannelUpstreamHandler;
import org.jboss.netty.handler.codec.frame.TooLongFrameException;
import org.jboss.netty.handler.codec.http.DefaultHttpResponse;
import org.jboss.netty.handler.codec.http.HttpMessage;
import org.jboss.netty.handler.codec.http.HttpRequest;
import org.jboss.netty.handler.codec.http.HttpResponse;
import org.jboss.netty.handler.codec.http.HttpResponseStatus;
import org.jboss.netty.handler.ssl.SslHandler;
import org.jboss.netty.handler.stream.ChunkedFile;
import org.jboss.netty.util.CharsetUtil;

import com.sun.org.apache.bcel.internal.generic.NEW;

/**
 * @author <a href="http://www.jboss.org/netty/">The Netty Project</a>
 * @author <a href="http://gleamynode.net/">Trustin Lee</a>
 */
public class HttpStaticFileServerHandler extends SimpleChannelUpstreamHandler {

	@Override
	public void messageReceived(ChannelHandlerContext ctx, MessageEvent e)
			throws Exception {
		HttpRequest request = (HttpRequest) e.getMessage();

		//打印头部信息
		printHeaders( request );
		
		// 不是POST方法也不是GET方法，返回。
		if ((request.getMethod() != GET) && (request.getMethod() != POST)) {
			sendError(ctx, METHOD_NOT_ALLOWED);
			return;
		}
		System.out.println("请求的URL = " + request.getUri());

		// 是否请求的UploadFile
		if (request.getUri().equalsIgnoreCase("/Uploadfile")) {
			if (request.getMethod() != POST) { // 不是请求的方法，返回 method_not_allowed
				sendError(ctx, METHOD_NOT_ALLOWED);
				return;
			}

			// 取得报文头部的长度字段。
			int len = (int) request.getContentLength();

			System.out.println("报文头部字段 Content-Length: = " + len);

			// 读出文件内容
			if (len > 0) { // 长度大于0
				// e 本身就已经包含了内容，直接转换为 ChannelBuffer
				Object msg = e.getMessage();
				if (msg instanceof HttpMessage) {// 是否 上个pipeline的返回结果。

					ChannelBuffer buffer = ((HttpMessage) msg).getContent();// ChannelBuffers.dynamicBuffer();
																			// //长度可变的buffer

					if (buffer.readable()) { // 含有内容
						int size = buffer.readableBytes(); // 可读取的字节数目
						byte[] dst = new byte[size];
						buffer.getBytes(0, dst);

						System.out.println(new String(dst)); // 输出后面的内容。
					}
				}
				else{
					System.out.println("不能处理的消息内容!");
				}
			}
		}

		// 如果请求的URL是 /, 则返回index.htm的内容
		if (request.getUri().equalsIgnoreCase("/")) {
			// ...
		}
		String strOut = "Hello,world! my netty server!";
		HttpResponse response = new DefaultHttpResponse(HTTP_1_1, OK);
		response.setHeader(CONTENT_TYPE, "text/html; charset=UTF-8"); // 设置响应的
																		// 编码：
																		// UTF-8
		setContentLength(response, strOut.getBytes().length);

		// 将内容写入缓存中
		ChannelBuffer buff = ChannelBuffers.buffer(strOut.getBytes().length);
		buff.writeBytes(strOut.getBytes());

		// 取得和客户端连接的 Channel
		Channel ch = e.getChannel();

		// Write the initial line and the header.
		ch.write(response); // Channel写入报文头部
		ChannelFuture writeFuture = ch.write(buff); // Channel中写入报文内容

		// Decide whether to close the connection or not.
		if (!isKeepAlive(request)) { // 如果http不需要保持连接的话，则关闭连接。
			// Close the connection when the whole content is written out.
			writeFuture.addListener(ChannelFutureListener.CLOSE);
		}
	}

	@Override
	public void exceptionCaught(ChannelHandlerContext ctx, ExceptionEvent e)
			throws Exception {
		Channel ch = e.getChannel();
		Throwable cause = e.getCause();
		if (cause instanceof TooLongFrameException) {
			sendError(ctx, BAD_REQUEST);
			return;
		}

		cause.printStackTrace();
		if (ch.isConnected()) {
			sendError(ctx, INTERNAL_SERVER_ERROR);
		}
	}

	private String sanitizeUri(String uri) {
		// Decode the path.
		try {
			uri = URLDecoder.decode(uri, "UTF-8");
		} catch (UnsupportedEncodingException e) {
			try {
				uri = URLDecoder.decode(uri, "ISO-8859-1");
			} catch (UnsupportedEncodingException e1) {
				throw new Error();
			}
		}

		// Convert file separators.
		uri = uri.replace('/', File.separatorChar);

		// Simplistic dumb security check.
		// You will have to do something serious in the production environment.
		if (uri.contains(File.separator + ".")
				|| uri.contains("." + File.separator) || uri.startsWith(".")
				|| uri.endsWith(".")) {
			return null;
		}

		// Convert to absolute path.
		return System.getProperty("user.dir") + File.separator + uri;
	}

	private void sendError(ChannelHandlerContext ctx, HttpResponseStatus status) {
		HttpResponse response = new DefaultHttpResponse(HTTP_1_1, status);
		response.setHeader(CONTENT_TYPE, "text/plain; charset=UTF-8");
		response.setContent(ChannelBuffers.copiedBuffer(
				"Failure: " + status.toString() + "\r\n", CharsetUtil.UTF_8));

		// Close the connection as soon as the error message is sent.
		ctx.getChannel().write(response)
				.addListener(ChannelFutureListener.CLOSE);
	}
	
	private void printHeaders(HttpRequest request){
		if( null == request ) return;
		Set<String> headSet = request.getHeaderNames();
		Iterator<String> it = headSet.iterator();
		while( it.hasNext() ) {
			String name = it.next();
			System.out.println(name + ": " + request.getHeader(name));
		}
	}
}
