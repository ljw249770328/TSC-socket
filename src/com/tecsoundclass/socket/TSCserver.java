package com.tecsoundclass.socket;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.net.InetSocketAddress;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.net.UnknownHostException;
import java.nio.channels.NotYetConnectedException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Random;
import java.util.concurrent.ConcurrentHashMap;

import org.java_websocket.WebSocket;
import org.java_websocket.handshake.ClientHandshake;
import org.java_websocket.server.WebSocketServer;

import com.google.gson.Gson;

import net.sf.json.JSONObject;

public class TSCserver extends WebSocketServer {

	private static ConcurrentHashMap<String, WebSocket> ClientSet = new ConcurrentHashMap<String, WebSocket>();
	private static Map<String , List<String>> ClientGroup=new HashMap<>();
	private static Map<String, String> CLS2TeaMap=new HashMap<String, String>();
	private static boolean accessable=false;
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
		new Thread(new Runnable() {
			@Override
			public void run() {
				// TODO Auto-generated method stub
				if(ClientSet.containsKey(ConnectString)){
					Map<String, String> resparam=new HashMap<String, String>();
					resparam.put("intent", "FORCE_OFFLINE");
					Gson gson=new Gson();
					try {
						ClientSet.get(ConnectString).send(URLEncoder.encode(gson.toJson(resparam),"UTF-8"));
					} catch (NotYetConnectedException | UnsupportedEncodingException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
					ClientSet.get(ConnectString).close();
					ClientSet.remove(ConnectString);
				}
				ClientSet.put(ConnectString, conn);
				System.out.println("["+conn.getRemoteSocketAddress().getAddress()
						.getHostAddress()+"]"+ConnectString
						+ "  连接至服务器");
				System.out.println(ClientSet.toString());
			}
		}).start();
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
		ClientSet.remove(key);
//		ClientSet.values().remove(conn);
		System.out.println(ClientSet.toString());
	}
	@Override
	public void onMessage(WebSocket conn, String message) {
		
		//解析message
		Map<String, String> param=new HashMap<String, String>();
		Map<String, String> resparam=new HashMap<String, String>();
		Gson gson=new Gson();
		try {
			String msg=URLDecoder.decode(message,"UTF-8");
			param=gson.fromJson(msg, param.getClass());
		} catch (UnsupportedEncodingException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		if (param!=null&&param.containsKey("condition")) {
			System.out.println(param.get("condition"));
			switch (param.get("condition")) {
			case "SignStart":
				List<String> stuStrings =new ArrayList<String>();
				//添加映射
				ClientGroup.put(param.get("SignClass"), stuStrings);
				CLS2TeaMap.put(param.get("SignClass"),param.get("ClsTea"));
				resparam.put("intent", "SIGN_STARTED");
				System.out.println(param.get("SignClass")+"开放签到通道");
				System.out.println(CLS2TeaMap.toString());
				try {
					conn.send(URLEncoder.encode(gson.toJson(resparam),"UTF-8"));
				} catch (NotYetConnectedException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				} catch (UnsupportedEncodingException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
				break;
			case "SignStop":
				ClientGroup.put("$"+param.get("StopClass"), ClientGroup.get(param.get("StopClass")));
				resparam.put("SignList", gson.toJson(ClientGroup.get(param.get("StopClass"))));
				ClientGroup.remove(param.get("StopClass"));
				resparam.put("intent", "SIGN_STOPPED");
				System.out.println(param.get("StopClass")+"关闭签到通道");
				try {
					conn.send(URLEncoder.encode(gson.toJson(resparam),"UTF-8"));
				} catch (NotYetConnectedException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				} catch (UnsupportedEncodingException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
				break;
			case "SignSuccess":
				ClientGroup.get(param.get("Cid")).add(param.get("Sid"));
				System.out.println(ClientGroup.get(param.get("Cid")).toString());
				resparam.put("intent", "SUC_SIGN");
				try {
					conn.send(URLEncoder.encode(gson.toJson(resparam),"UTF-8"));
				} catch (NotYetConnectedException | UnsupportedEncodingException e1) {
					// TODO Auto-generated catch block
					e1.printStackTrace();
				}
				break;
			case "CheckSign":
				String Sid=param.get("SignStu");
				if (ClientGroup.containsKey(param.get("SignClass"))) {
					if (ClientGroup.get(param.get("SignClass")).contains(Sid)) {
						resparam.put("intent", "SIGN_ED");
						try {
							conn.send(URLEncoder.encode(gson.toJson(resparam),"UTF-8"));
						} catch (NotYetConnectedException | UnsupportedEncodingException e) {
							// TODO Auto-generated catch block
							e.printStackTrace();
						}
					}else {
						resparam.put("intent","SIGN_ACCESSED");
						System.out.println(Sid+resparam.get("intent"));
						try {
							conn.send(URLEncoder.encode(gson.toJson(resparam),"UTF-8"));
						} catch (NotYetConnectedException | UnsupportedEncodingException e) {
							// TODO Auto-generated catch block
							e.printStackTrace();
						}
					}
				}else {
					resparam.put("intent", "SIGN_DENYED");
					System.out.println(Sid+resparam.get("intent"));
					try {
						conn.send(URLEncoder.encode(gson.toJson(resparam),"UTF-8"));
					} catch (NotYetConnectedException | UnsupportedEncodingException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
				}
				break;
			case "ActQuestion":
				resparam.put("intent", "COME_QUESTION");
				resparam.put("question", param.get("Question"));
				resparam.put("CourseId", param.get("Course"));
				System.out.println(param.get("Course")+"  QUES: "+param.get("Question"));
				try {
					send2Group("$"+param.get("Course"),URLEncoder.encode(gson.toJson(resparam),"UTF-8"));
					send2Single(CLS2TeaMap.get(param.get("Course")), URLEncoder.encode(gson.toJson(resparam),"UTF-8"));
				} catch (UnsupportedEncodingException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
				break;
			case "Answered":
				String cidString =param.get("Cid");
				resparam.put("intent", "GRADE_DIALOG");
				resparam.put("question", param.get("question"));
				resparam.put("answer", param.get("Answer"));
				resparam.put("VoiceURL", param.get("VoiceURL"));
				resparam.put("Cid",param.get("Cid"));
				resparam.put("Sid",  getKey(ClientSet, conn));
				try {
					send2Single(CLS2TeaMap.get(cidString), URLEncoder.encode(gson.toJson(resparam),"UTF-8"));
				} catch (UnsupportedEncodingException e1) {
					// TODO Auto-generated catch block
					e1.printStackTrace();
				}
				break;
			case "Graded":
				resparam.put("intent", "GRADE_ED");
				resparam.put("Grade", param.get("Grade"));
				Map<String, String> resparamA=new HashMap<String, String>();
				resparamA.put("intent", "INTERACT_REFLESH");
				try {
					send2Single(param.get("Sid"), URLEncoder.encode(gson.toJson(resparam),"UTF-8"));
					send2Group("$"+param.get("Cid"),URLEncoder.encode(gson.toJson(resparamA),"UTF-8"));
					conn.send(URLEncoder.encode(gson.toJson(resparamA),"UTF-8"));
				} catch (UnsupportedEncodingException e1) {
					// TODO Auto-generated catch block
					e1.printStackTrace();
				}
				break;
			case "Caughted":
				resparam.put("intent", "DIALOG_CANCLE");
				resparam.put("CaughtUid",getKey(ClientSet, conn));
				try {
					send2GroupExc("$"+param.get("Cid"), URLEncoder.encode(gson.toJson(resparam),"UTF-8"),conn);
					send2Single(CLS2TeaMap.get(param.get("Cid")), URLEncoder.encode(gson.toJson(resparam),"UTF-8"));
				} catch (UnsupportedEncodingException e1) {
					// TODO Auto-generated catch block
					e1.printStackTrace();
				}
				break;
			case "NoReply":
				String Cid =param.get("Cid");
				List<String> clsLst=ClientGroup.get("$"+Cid);
				String idString=clsLst.get(new Random().nextInt(clsLst.size()));
				System.out.println(clsLst.toString()+clsLst.size()+"||"+clsLst.get(new Random().nextInt(clsLst.size())));
				resparam.put("CaughtUid", idString);
				resparam.put("intent", "DRAW_ED");
				resparam.put("question", param.get("question"));
				resparam.put("CourseId", Cid);
				try {
					send2Single(idString, URLEncoder.encode(gson.toJson(resparam),"UTF-8"));
					conn.send(URLEncoder.encode(gson.toJson(resparam),"UTF-8"));
				} catch (UnsupportedEncodingException e1) {
					// TODO Auto-generated catch block
					e1.printStackTrace();
				}
				break;
			case "ChatWith":
				resparam.put("intent","COME_CHAT");
				resparam.put("message", param.get("message"));
				resparam.put("Sender", getKey(ClientSet, conn));
				System.out.println(getKey(ClientSet, conn)+" 向 "+param.get("SendUser")+" 发送 "+param.get("message"));
				try {
					send2Single(param.get("SendUser"),URLEncoder.encode(gson.toJson(resparam),"UTF-8"));
				} catch (UnsupportedEncodingException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
				break;
			case "InteractCancel":
				resparam.put("intent", "DIALOG_CANCLE");
				resparam.put("CaughtUid","");

				try {
					send2Group("$"+param.get("Cid"),URLEncoder.encode(gson.toJson(resparam),"UTF-8"));
				} catch (UnsupportedEncodingException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
				
				break;
			case "GetStuList":
				resparam.put("intent", "ONLINE_LIST");
				resparam.put("OnLineList", gson.toJson(ClientGroup.get("$"+param.get("Cid"))));
				try {
					conn.send(URLEncoder.encode(gson.toJson(resparam),"UTF-8"));
				} catch (NotYetConnectedException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				} catch (UnsupportedEncodingException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}

				break;
			case "TeaSelect":
				String sid=param.get("Sid");
				String cid=param.get("Cid");
				String question=param.get("question"+"  "+CLS2TeaMap.get(cid));
				
				System.out.println(sid+"  "+cid+"  "+question);
					//取消教师和学生的对话框	
					try {
						Map<String, String> resparam2=new HashMap<String, String>();
						resparam2.put("intent", "DIALOG_CANCLE");
						resparam2.put("CaughtUid",sid);
						send2Single(CLS2TeaMap.get(cid), URLEncoder.encode(gson.toJson(resparam2),"UTF-8"));
						send2Group("$"+cid,URLEncoder.encode(gson.toJson(resparam2),"UTF-8"));
						resparam.put("CaughtUid", sid);
						resparam.put("intent", "DRAW_ED");
						resparam.put("question",question );
						resparam.put("CourseId", cid);
						//发送给教师选中的学生
						send2Single(sid, URLEncoder.encode(gson.toJson(resparam),"UTF-8"));							
					} catch (UnsupportedEncodingException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}				
				break;
			
			default:
				break;
			}
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
		System.out.println(ClientSet.toString());
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
	//发送给除自己外的用户群
	private void send2GroupExc(String GroupId,String text,WebSocket conn) {
		Collection<WebSocket> conns = connections();
		List<String> list =ClientGroup.get(GroupId);
		System.out.println(list.toString());
		synchronized (conns) {
			for (WebSocket client : conns) {
				for(String user:list) {
					if(client==ClientSet.get(user) && client!=conn) {
						client.send(text);
					}
				}
			}
		}
	}

	public static String getKey(Map map, Object value){
	    String targetString="";
	    for(Object key: map.keySet()){
	        if(map.get(key).equals(value)){
	            targetString=(String) key;
	            break;
	        }
	    }
	    return targetString;
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
