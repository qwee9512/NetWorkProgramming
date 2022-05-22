package exam_13;

import java.io.*;
import java.net.*;
import java.util.*;
import java.awt.*;
import java.awt.event.*;

/*
 * Ŭ���̾�Ʈ ������ �ϴ� ���α׷�
 * �������� �α���, �α׾ƿ� ���¸� ó���ϱ� ���� �ΰ��� Thread�� ������
 * �α���, �α׾ƿ� �޼����� �����ϰ� Ȯ�� ������ �޴� DatagramSocket "socket"
 * ��Ƽĳ��Ʈ �ּҷ� �����͸� ��/�����ϱ� ���� MulticastSocket "multi"
 * 
 * Ŭ���̾�Ʈ�� ���̵� �Է��ϰ� �α��� ��ư�� ������ ������ �ּҷ� �ּҿ�û �޼����� ����
 * ������ ���̵��� �ߺ��� Ȯ���ϰ� ��Ƽĳ��Ʈ ä�� �׷� �ּҸ� �ѱ�
 * Ŭ���̾�Ʈ�� ���ۿ� ��Ŷ�� �׷� �ּҷ� �ʱ�ȭ�ϰ� �ؽ�Ʈ�ʵ��� ���� �о� �����
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
		super("Ŭ���̾�Ʈ");

		mlbl = new Label("ä�� ���¸� �����ݴϴ�.");
		add(mlbl, BorderLayout.NORTH);

		display = new TextArea("", 0, 0, TextArea.SCROLLBARS_VERTICAL_ONLY);
		display.setEditable(false);
		add(display, BorderLayout.CENTER);

		ptotal = new Panel(new BorderLayout());

		pword = new Panel(new BorderLayout());
		wlbl = new Label("��ȭ��");
		wtext = new TextField(20); // ������ �����͸� �Է��ϴ� �ʵ�
		wtext.addKeyListener(this); // �Էµ� �����͸� �۽��ϱ� ���� �̺�Ʈ ����
		pword.add(wlbl, BorderLayout.WEST);
		pword.add(wtext, BorderLayout.EAST);
		ptotal.add(pword, BorderLayout.NORTH);

		plabel = new Panel(new BorderLayout());
		loglbl = new Label("�α׿�");
		ltext = new TextField(20); // ������ �����͸� �Է��ϴ� �ʵ�
		ltext.addActionListener(this); // �Էµ� �����͸� �۽��ϱ� ���� �̺�Ʈ ����
		logoutBtn = new Button("�α׾ƿ�");
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
			// ������ �ּ� ��û �޽����� ������ ���� ���� ����
			socket = new DatagramSocket();
			serverIP = InetAddress.getLocalHost();
			receivePacket = new DatagramPacket(data, data.length);
			multi = new MulticastSocket(6002);
		}
		catch (IOException e)
		{
			// TODO �ڵ� ������ catch ���
			e.printStackTrace();
		}
	}

	public void runClient()
	{
		mlbl.setText("��Ƽĳ��Ʈ ä�� ������ ���� ��û�մϴ�.");
		clientdata = new StringBuffer(2048);
		while (true)
		{
			if (ID != null)
			{
				try
				{
					// DatagramSocket���� ��Ƽĳ��Ʈ �ּ� �ޱ�
					socket.receive(receivePacket);
					serverdata = new String(receivePacket.getData(), receivePacket.getOffset(),
							receivePacket.getLength());

					// �α��� ��û�� ���� ������ OK�̸� ���� ��Ƽĳ��Ʈ IP�ּҿ� �����Ѵ�.
					// �α��� ��û�� ���� ������ DENY�̸� �ߺ� ���̵����� ǥ���Ѵ�.
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
						{ // ���鹮�� ������ ���� ��ȭ���߰�
							message = message + " " + st.nextToken();
						}
						display.append(message + "\r\n");
						mlbl.setText(ID + "(��)�� �α��� �Ͽ����ϴ�.");
						ltext.setText("");

						// �α׾ƿ� ��ư�� ���
						ptotal.remove(plabel);
						ptotal.add(logoutBtn, BorderLayout.SOUTH);
						setVisible(true);
						rThread r = new rThread();
						r.start();
						break;
					}
					case REQ_LOGON_DENY:
					{
						mlbl.setText("�̹� �����ϴ� ID�Դϴ�!!!");
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
				// �α��ο�û|���̵�| ���·� ������ �޼����� ����
				clientdata.setLength(0);
				clientdata.append(REQ_LOGON);
				clientdata.append(SEPARATOR);
				clientdata.append(ID);
				clientdata.append(SEPARATOR);
				sendPacket = new DatagramPacket(clientdata.toString().getBytes(),
						clientdata.toString().getBytes().length, serverIP, serverPORT);
				socket.send(sendPacket);
				// �α����� �Ϸ�Ǹ� ��ȭ�� �ؽ�Ʈ�ʵ�� ��Ŀ���� �ű�
				wtext.requestFocus();
			}
			catch (IOException e1)
			{
				// TODO �ڵ� ������ catch ���
				e1.printStackTrace();
			}
		}
	}

	public void logout()
	{
		try
		{
			// �α׾ƿ���û|���̵� ���·� ������ �޼����� ����
			clientdata.setLength(0);
			clientdata.append(REQ_LOGOUT);
			clientdata.append(SEPARATOR);
			clientdata.append(ID);
			clientdata.append(SEPARATOR);
			data = clientdata.toString().getBytes();
			sendPacket = new DatagramPacket(data, data.length, serverIP, serverPORT);
			socket.send(sendPacket);
			// login �޼��尡 ����Ǿ� ���� groupIP���� ����
			multi.leaveGroup(groupIP);

			mlbl.setText(ID + "(��)�� �α׾ƿ� �Ͽ����ϴ�.");
			// �α׾ƿ� �� Ŭ���̾�Ʈ ȭ���� �ʱ�ȭ������ ���ư��� ��
			display.setText("");
			ID = null;

			ptotal.add(plabel, BorderLayout.CENTER);
			ptotal.remove(logoutBtn);
			setVisible(true);
		}
		catch (IOException e)
		{
			// TODO �ڵ� ������ catch ���
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

	// ��ȭ�� ������ ���� �Լ�
	public void keyPressed(KeyEvent ke)
	{
		if (ke.getKeyChar() == KeyEvent.VK_ENTER)
		{
			String message = wtext.getText();
			StringTokenizer st = new StringTokenizer(message, " ");
			if (ID == null)
			{
				mlbl.setText("�α��� �� �̿��ϼ���!!!");
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
	
	// ��Ƽĳ��Ʈ �޼����� ó���ϱ� ���� ������ Ŭ����
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
						display.append("����� " + memberID + "(��)�� �α��� �Ͽ����ϴ�" + "\r\n");
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
							// �� �޼����� ���������� ������ ó���ϱ� ���� �ۼ�
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
					// TODO �ڵ� ������ catch ���
					e.printStackTrace();
				}
			}
		}
	}
}