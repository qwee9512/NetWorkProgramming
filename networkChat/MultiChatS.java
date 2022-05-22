package exam_13;

import java.io.*;
import java.net.*;
import java.util.*;
import java.awt.*;
import java.awt.event.*;

/*
 * 서버 역할을 하는 프로그램
 * 클라이언트의 로그인과 로그아웃 메세지 처리를 담당하는 ServerThread4 클래스
 * 
 * 멀티캐스트 소켓으로 메시지를 전달하면 리스트를 이용한 전체 메시지 전달을 하지 않아도 됨
 * DatagramSocket으로 멀티캐스트 주소를 전송한 뒤 클라이언트 아이디 서버정보를 해쉬맵에 추가
 * 멀티캐스트 메시지는 주소 239.255.10.20로 전송함
 * 서버의 데이터그램 소켓의 포트번호는 6000이다. 
 */

public class MultiChatS extends Frame
{
	TextArea display;
	Label info;
	HashMap<String, ServerThread4> hash;
	public ServerThread4 SThread;
	InetAddress multicastIP;

	public MultiChatS()
	{
		super("서버");
		info = new Label();
		add(info, BorderLayout.CENTER);
		display = new TextArea("", 0, 0, TextArea.SCROLLBARS_VERTICAL_ONLY);
		display.setEditable(false);
		add(display, BorderLayout.SOUTH);
		addWindowListener(new WinListener());
		setSize(300, 250);
		setVisible(true);
	}

	public void runServer()
	{
		DatagramSocket sock;
		ServerThread4 SThread;
		hash = new HashMap<String, ServerThread4>();
		try
		{
			multicastIP = InetAddress.getByName("239.255.10.20");
			sock = new DatagramSocket(6000);
			while (true)
			{
				// 한 클라이언트와 소켓을 통해 통신한다. (스레드)
				SThread = new ServerThread4(this, sock, multicastIP, display, info);
				SThread.start();
				info.setText("멀티캐스트 채팅 그룹 주소 : " + multicastIP.getHostName());
			}
		}
		catch (IOException ioe)
		{
			ioe.printStackTrace();
		}

	}

	public static void main(String args[])
	{
		MultiChatS s = new MultiChatS();
		s.runServer();
	}

	class WinListener extends WindowAdapter
	{
		public void windowClosing(WindowEvent e)
		{
			System.exit(0);
		}
	}
}

class ServerThread4 extends Thread
{
	MulticastSocket server;
	DatagramSocket sock;
	DatagramPacket receivePacket;
	DatagramPacket sendPacket;
	InetAddress groupIP;

	TextArea display;
	Label info;
	TextField text;
	String clientdata;
	String serverdata = "";
	MultiChatS cs;

	byte[] data = new byte[10000];

	private static final String SEPARATOR = "|";
	private static final int REQ_LOGON = 1001;
	private static final int REQ_LOGOUT = 1002;
	private static final int REQ_LOGON_OK = 1003;
	private static final int REQ_LOGON_DENY = 1004;

	public ServerThread4(MultiChatS c, DatagramSocket s, InetAddress m, TextArea ta, Label l)
	{
		sock = s;
		groupIP = m;
		display = ta;
		info = l;
		cs = c;
		try
		{
			server = new MulticastSocket(6001);
			server.joinGroup(m);
		}
		catch (IOException e)
		{
			// TODO 자동 생성된 catch 블록
			e.printStackTrace();
		}
		receivePacket = new DatagramPacket(data, data.length);
	}

	public void run()
	{
		try
		{
			sock.receive(receivePacket);
			clientdata = new String(receivePacket.getData(), receivePacket.getOffset(), receivePacket.getLength());
			InetAddress clientIP = receivePacket.getAddress();
			int clientPORT = receivePacket.getPort();
			while (clientdata != null)
			{

				StringTokenizer st = new StringTokenizer(clientdata, SEPARATOR);
				int command = Integer.parseInt(st.nextToken());
				switch (command)
				{
				case REQ_LOGON:
				{
					String ID = st.nextToken();
					if (cs.hash.containsKey(ID))
					{
						serverdata = REQ_LOGON_DENY + SEPARATOR;
						data = serverdata.getBytes();
						sendPacket = new DatagramPacket(data, data.length, clientIP, clientPORT);
						sock.send(sendPacket);
					}
					else
					{
						display.append("클라이언트가 " + ID + "(으)로 로그인 하였습니다.\r\n");
						cs.hash.put(ID, this); // 해쉬 테이블에 아이디와 스레드를 저장한다
						serverdata = REQ_LOGON_OK + SEPARATOR + groupIP.getHostAddress() + SEPARATOR;

						// 기로그인 사용자들의 key값을 담기 위한 StringBuffer 변수 생성
						StringBuffer logonMember = new StringBuffer("로그인한 사용자 목록\r\n");
						for (String key : cs.hash.keySet())
						{
							if (!key.equals(ID))
								logonMember.append("아이디 : " + key + "\r\n");
						}
						data = (serverdata + logonMember.toString() + SEPARATOR).getBytes();
						sendPacket = new DatagramPacket(data, data.length, clientIP, clientPORT);
						sock.send(sendPacket);
						
						// 사용자 로그인 시 아이디를 멀티캐스트 메시지로 보냄
						serverdata = REQ_LOGON + SEPARATOR + ID + SEPARATOR;
						data = serverdata.getBytes();
						sendPacket = new DatagramPacket(data, data.length, groupIP, 6002);
						server.send(sendPacket);
					}
					break;
				}

				case REQ_LOGOUT:
				{ // “1002|아이디”를 수신한 경우
					String ID = st.nextToken();
					cs.hash.remove(ID);
					display.append("클라이언트 " + ID + "(이)가 로그아웃 하였습니다.\r\n");
					serverdata = REQ_LOGOUT + SEPARATOR + "사용자 " + ID + "(이)가 로그아웃 하였습니다" + SEPARATOR;
					data = serverdata.getBytes();
					sendPacket = new DatagramPacket(data, data.length, groupIP, 6002);
					server.send(sendPacket);
					break;
				}
				}
				sock.receive(receivePacket);
				clientdata = new String(receivePacket.getData(), receivePacket.getOffset(), receivePacket.getLength());
				if (clientdata == null)
					break;
			}
		}
		catch (IOException e)
		{
			e.printStackTrace();
		}
		sock.close();
	}
}