package exam_13;

import java.io.*;
import java.net.*;
import java.util.*;
import java.awt.*;
import java.awt.event.*;

/*
 * 클라이언트 역할을 하는 프로그램
 * 서버와의 로그인, 로그아웃 상태를 처리하기 위해 두개의 Thread를 생성함
 * 로그인, 로그아웃 메세지를 전송하고 확인 응답을 받는 DatagramSocket "socket"
 * 멀티캐스트 주소로 데이터를 송/수신하기 위한 MulticastSocket "multi"
 * 
 * 클라이언트가 아이디를 입력하고 로그인 버튼을 누르면 서버의 주소로 주소요청 메세지를 전송
 * 서버는 아이디의 중복을 확인하고 멀티캐스트 채팅 그룹 주소를 넘김
 * 클라이언트는 전송용 패킷을 그룹 주소로 초기화하고 텍스트필드의 값을 읽어 통신함
 */

public class MultiChatC extends Frame implements ActionListener, KeyListener
{

	TextArea display;
	TextField wtext, ltext;
	Label mlbl, wlbl, loglbl;
	Button logoutBtn;
	Panel ptotal;
	Panel pword;
	Panel plabel;

	StringBuffer clientdata;
	String serverdata = "";
	String ID;

	DatagramSocket socket;
	MulticastSocket multi;
	DatagramPacket sendPacket;
	DatagramPacket receivePacket;
	byte[] data = new byte[10000];

	private static final String SEPARATOR = "|";
	private static final int REQ_LOGON = 1001;
	private static final int REQ_LOGOUT = 1002;
	private static final int REQ_LOGON_OK = 1003;
	private static final int REQ_LOGON_DENY = 1004;
	private static final int REQ_SENDWORDS = 1005;
	private static final int serverPORT = 6000;
	int port = 6002;
	private static InetAddress serverIP;
	private static InetAddress groupIP;

	public MultiChatC()
	{
		super("클라이언트");

		mlbl = new Label("채팅 상태를 보여줍니다.");
		add(mlbl, BorderLayout.NORTH);

		display = new TextArea("", 0, 0, TextArea.SCROLLBARS_VERTICAL_ONLY);
		display.setEditable(false);
		add(display, BorderLayout.CENTER);

		ptotal = new Panel(new BorderLayout());

		pword = new Panel(new BorderLayout());
		wlbl = new Label("대화말");
		wtext = new TextField(20); // 전송할 데이터를 입력하는 필드
		wtext.addKeyListener(this); // 입력된 데이터를 송신하기 위한 이벤트 연결
		pword.add(wlbl, BorderLayout.WEST);
		pword.add(wtext, BorderLayout.EAST);
		ptotal.add(pword, BorderLayout.NORTH);

		plabel = new Panel(new BorderLayout());
		loglbl = new Label("로그온");
		ltext = new TextField(20); // 전송할 데이터를 입력하는 필드
		ltext.addActionListener(this); // 입력된 데이터를 송신하기 위한 이벤트 연결
		logoutBtn = new Button("로그아웃");
		logoutBtn.addActionListener(this);
		plabel.add(loglbl, BorderLayout.WEST);
		plabel.add(ltext, BorderLayout.EAST);
		ptotal.add(plabel, BorderLayout.CENTER);

		add(ptotal, BorderLayout.SOUTH);

		addWindowListener(new WinListener());
		setSize(300, 250);
		setVisible(true);

		try
		{
			// 서버에 주소 요청 메시지를 보내기 위한 소켓 생성
			socket = new DatagramSocket();
			serverIP = InetAddress.getLocalHost();
			receivePacket = new DatagramPacket(data, data.length);
			multi = new MulticastSocket(6002);
		}
		catch (IOException e)
		{
			// TODO 자동 생성된 catch 블록
			e.printStackTrace();
		}
	}

	public void runClient()
	{
		mlbl.setText("멀티캐스트 채팅 서버에 가입 요청합니다.");
		clientdata = new StringBuffer(2048);
		while (true)
		{
			if (ID != null)
			{
				try
				{
					// DatagramSocket으로 멀티캐스트 주소 받기
					socket.receive(receivePacket);
					serverdata = new String(receivePacket.getData(), receivePacket.getOffset(),
							receivePacket.getLength());

					// 로그인 요청에 대한 응답이 OK이면 받은 멀티캐스트 IP주소에 참여한다.
					// 로그인 요청에 대한 응답이 DENY이면 중복 아이디임을 표시한다.
					StringTokenizer st = new StringTokenizer(serverdata, SEPARATOR);
					int command = Integer.parseInt(st.nextToken());
					switch (command)
					{
					case REQ_LOGON_OK:
					{
						groupIP = InetAddress.getByName(st.nextToken());
						multi.joinGroup(groupIP);
						String message = st.nextToken();
						while (st.hasMoreTokens())
						{ // 공백문자 다음에 오는 대화말추가
							message = message + " " + st.nextToken();
						}
						display.append(message + "\r\n");
						mlbl.setText(ID + "(으)로 로그인 하였습니다.");
						ltext.setText("");

						// 로그아웃 버튼을 띄움
						ptotal.remove(plabel);
						ptotal.add(logoutBtn, BorderLayout.SOUTH);
						setVisible(true);
						rThread r = new rThread();
						r.start();
						break;
					}
					case REQ_LOGON_DENY:
					{
						mlbl.setText("이미 존재하는 ID입니다!!!");
						ltext.setText("");
						ID = null;
						break;
					}
					}
				}
				catch (IOException e)
				{
					System.err.println(e);
				}
			}
			else
				System.out.print("");
		}
	}

	public void login()
	{
		if (ID == null && !ltext.getText().equals(""))
		{
			ID = ltext.getText();
			try
			{
				// 로그인요청|아이디| 형태로 서버에 메세지를 전송
				clientdata.setLength(0);
				clientdata.append(REQ_LOGON);
				clientdata.append(SEPARATOR);
				clientdata.append(ID);
				clientdata.append(SEPARATOR);
				sendPacket = new DatagramPacket(clientdata.toString().getBytes(),
						clientdata.toString().getBytes().length, serverIP, serverPORT);
				socket.send(sendPacket);
				// 로그인이 완료되면 대화말 텍스트필드로 포커스를 옮김
				wtext.requestFocus();
			}
			catch (IOException e1)
			{
				// TODO 자동 생성된 catch 블록
				e1.printStackTrace();
			}
		}
	}

	public void logout()
	{
		try
		{
			// 로그아웃요청|아이디 형태로 서버에 메세지를 전송
			clientdata.setLength(0);
			clientdata.append(REQ_LOGOUT);
			clientdata.append(SEPARATOR);
			clientdata.append(ID);
			clientdata.append(SEPARATOR);
			data = clientdata.toString().getBytes();
			sendPacket = new DatagramPacket(data, data.length, serverIP, serverPORT);
			socket.send(sendPacket);
			// login 메서드가 실행되어 얻은 groupIP에서 나감
			multi.leaveGroup(groupIP);

			mlbl.setText(ID + "(이)가 로그아웃 하였습니다.");
			// 로그아웃 시 클라이언트 화면을 초기화면으로 돌아가게 함
			display.setText("");
			ID = null;

			ptotal.add(plabel, BorderLayout.CENTER);
			ptotal.remove(logoutBtn);
			setVisible(true);
		}
		catch (IOException e)
		{
			// TODO 자동 생성된 catch 블록
			e.printStackTrace();
		}
	}

	public void actionPerformed(ActionEvent ae)
	{

		if (ae.getSource() == ltext)
			login();
		else if (ae.getSource() == logoutBtn)
			logout();
	}

	public static void main(String args[])
	{
		MultiChatC c = new MultiChatC();
		c.runClient();
	}

	class WinListener extends WindowAdapter
	{
		public void windowClosing(WindowEvent e)
		{
			if (ID != null)
				logout();
			System.exit(0);
		}
	}

	// 대화말 전송을 위한 함수
	public void keyPressed(KeyEvent ke)
	{
		if (ke.getKeyChar() == KeyEvent.VK_ENTER)
		{
			String message = wtext.getText();
			StringTokenizer st = new StringTokenizer(message, " ");
			if (ID == null)
			{
				mlbl.setText("로그인 후 이용하세요!!!");
				wtext.setText("");
			}
			else
			{
				try
				{
					clientdata.setLength(0);
					clientdata.append(REQ_SENDWORDS);
					clientdata.append(SEPARATOR);
					clientdata.append(ID);
					clientdata.append(SEPARATOR);
					clientdata.append(message);
					clientdata.append(SEPARATOR);

					data = clientdata.toString().getBytes();
					sendPacket = new DatagramPacket(data, data.length, groupIP, port);
					multi.send(sendPacket);

					wtext.setText("");
				}

				catch (IOException e)
				{
					e.printStackTrace();
				}
			}
		}
	}

	public void keyReleased(KeyEvent ke)
	{
	}

	public void keyTyped(KeyEvent ke)
	{
	}
	
	// 멀티캐스트 메세지를 처리하기 위해 생성한 클래스
	class rThread extends Thread
	{
		public void run()
		{
			while (true)
			{
				try
				{
					multi.receive(receivePacket);
					serverdata = new String(receivePacket.getData(), receivePacket.getOffset(),
							receivePacket.getLength());
					StringTokenizer st = new StringTokenizer(serverdata, SEPARATOR);
					int command = Integer.parseInt(st.nextToken());
					switch (command)
					{
					case REQ_LOGON:
					{
						String memberID = st.nextToken();
						display.append("사용자 " + memberID + "(이)가 로그인 하였습니다" + "\r\n");
						break;
					}

					case REQ_SENDWORDS:
					{
						String memberID = "";
						String memberMessage;
						try
						{
							memberID = st.nextToken();
							memberMessage = st.nextToken();
							display.append(memberID + " : " + memberMessage + "\r\n");
						}
						catch (NoSuchElementException e)
						{
							// 빈 메세지가 도착했을때 오류를 처리하기 위해 작성
							memberMessage = "";
							display.append(memberID + " : " + memberMessage + "\r\n");
						}
						break;
					}
					case REQ_LOGOUT:
					{
						String message;
						message = st.nextToken();
						display.append(message + "\r\n");
						break;
					}
					}
				}
				catch (IOException e)
				{
					// TODO 자동 생성된 catch 블록
					e.printStackTrace();
				}
			}
		}
	}
}