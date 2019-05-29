package com.tecsoundclass.socket;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.net.InetSocketAddress;
import java.net.URLDecoder;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;

import org.java_websocket.WebSocket;
import org.java_websocket.handshake.ClientHandshake;
import org.java_websocket.server.WebSocketServer;

public class TSCserver extends WebSocketServer {

	private static ConcurrentHashMap<String, WebSocket> ClientSet = new ConcurrentHashMap<String, WebSocket>();
	private static Map<String , List<String>> ClientGroup=new HashMap<>();
	private String ConnectString =null;
	
	public TSCserver(int port) throws UnknownHostException {
		super(new InetSocketAddress(port));
	}

	public TSCserver(InetSocketAddress address) {
		super(address);
	}
	
	@Override
	public void onOpen(WebSocket conn, ClientHandshake handshake) {
		
		try {
			ConnectString=URLDecoder.decode(handshake.getFieldValue("id"),"UTF-8");
		} catch (UnsupportedEncodingException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		if(ClientSet.containsKey(ConnectString)){
			ClientSet.get(ConnectString).close();
			ClientSet.remove(ConnectString);
		}
		ClientSet.put(ConnectString, conn);
		System.out.println("["+conn.getRemoteSocketAddress().getAddress()
				.getHostAddress()+"]"+ConnectString
				+ "  连接至服务器");
		System.out.println(ClientSet.toString());
	}
	@Override
	public void onClose(WebSocket conn, int code, String reason, boolean remote) {
		String key="";
		for (Entry<String, WebSocket> entry:ClientSet.entrySet()) {
			if (conn==entry.getValue()) {
				key=entry.getKey();
				break;
			}
		}
		System.out.println("["+conn.getRemoteSocketAddress().getAddress()
				.getHostAddress()+"]"+key
				+ "  与服务器断开连接,返回码:"+"["+code+"]");
		ClientSet.values().remove(conn);
//		System.out.println(ClientSet.toString());
	}
	@Override
	public void onMessage(WebSocket conn, String message) {
		//解析message中是否有拼接信息,没有则群体发送
		if (message.contains("|")) {
			//带有[condition]的字段解析,主要为课程功能交互
			if (message.contains("[")) {
				String condition=message.substring(message.indexOf("["), message.indexOf("]"));
				//对condition进行判定
				if(condition.equals("startsign")) {
					String str[]=message.split("\\|");
					
				}
				
			}//无[condition],则为普通消息通信
			else {
				
			}
			
		}else {
			send2All("["
					+ conn.getRemoteSocketAddress().getAddress().getHostAddress()
					+ "]" +"向服务器发送"+message);
	
			System.out.println("["
					+ conn.getRemoteSocketAddress().getAddress().getHostAddress()
					+ "]" +"向服务器发送"+message);
		}
	}
	@Override
	public void onError(WebSocket conn, Exception e) {
		String key="";
		for (Entry<String, WebSocket> entry:ClientSet.entrySet()) {
			if (conn==entry.getValue()) {
				key=entry.getKey();
				break;
			}
		}
		System.out.println("["+conn.getRemoteSocketAddress().getAddress().getHostAddress()+"]"+key+"  与服务器连接异常");
		e.printStackTrace();
		if (conn != null) {
			conn.close();
			ClientSet.values().remove(conn);
		}
		
	}

	//发送给该端口所有对象
	private void send2All(String text) {
		Collection<WebSocket> conns = connections();
		synchronized (conns) {
			for (WebSocket client : conns) {
				client.send(text);
			}
		}
	}
	
	
	//发送给单个指定链接对象
	private void send2Single(String id,String text) {
		Collection<WebSocket> conns = connections();
		synchronized (conns) {
			for (WebSocket client : conns) {
				if (client==ClientSet.get(id)) {
					client.send(text);
				}
			}
		}
	}
	
	//发送给指定群组客户端
	private void send2Group(String GroupId,String text) {
		Collection<WebSocket> conns = connections();
		List<String> list =ClientGroup.get(GroupId);
		synchronized (conns) {
			for (WebSocket client : conns) {
				for(String user:list) {
					if(client==ClientSet.get(user)) {
						client.send(text);
					}
				}
			}
		}
	}

	
	public static void main(String[] args) throws InterruptedException,
	IOException {

	int port = 8886;
	
	TSCserver server = new TSCserver(port);
	server.start();
	
	System.out.println("房间已开启，等待客户端接入，端口号: " + server.getPort());
	
	BufferedReader webSocketIn = new BufferedReader(new InputStreamReader(
			System.in));
	
	while (true) {
		String stringIn = webSocketIn.readLine();
		server.send2All(stringIn);
		}
	}
}
