import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;

import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;

//Jonathan Hsu

public class ChatForTwo implements ActionListener 
{
	public static void main(String[] args) {
		System.out.println("Jonathan Hsu ChatForTwo");
		if(args.length < 2)
		{
			System.out.println("Please enter your network address and your port number");
			System.out.println("If you are the server enter server instead of a network address");
			return;
		}
		try {
			new ChatForTwo(args[0], args[1]);
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	JFrame window = new JFrame("ChatForTwo");
	JTextArea chatArea = new JTextArea();
	JTextArea messageArea = new JTextArea();
	JButton sendButton = new JButton("Send");
	JPanel chatPanel = new JPanel();
	JScrollPane messageScrollPane = new JScrollPane(messageArea);
	
	Socket s;
	ServerSocket ss;
	DataInputStream dis;
	DataOutputStream dos;
	
	String newLine = System.lineSeparator();
	
	public ChatForTwo(String address, String port) throws Exception 
	{
		try
		{
			int portNumber = Integer.parseInt(port);
			if(address.equals("server"))
			{
				ss = new ServerSocket(portNumber);
				System.out.println("Waiting for other user to connect");
				s = ss.accept();
			}
			else
			{
				s = new Socket(address, portNumber);
			}
		}
		catch(NumberFormatException nfe)
		{
			throw new Exception("Port must be numeric");
		}
		System.out.println("Connected at " + address + " on port " + port);
		
		dis = new DataInputStream(s.getInputStream());
		dos = new DataOutputStream(s.getOutputStream());
		
		chatPanel.setLayout(new GridLayout(1, 2));
		chatPanel.add(chatArea);
		chatPanel.add(sendButton);
		window.getContentPane().add(messageScrollPane, "Center");
		window.getContentPane().add(chatPanel, "South");
		messageArea.setEditable(false);
		window.setSize(400, 400);
		window.setVisible(true);
		window.setLocation(400,0);
		window.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		
		sendButton.addActionListener(this);
		
		while(true)
		{
			try
			{
				String chat = dis.readUTF().trim();
				messageArea.append("Other user: " + chat + newLine);
				messageArea.setCaretPosition(messageArea.getDocument().getLength());
			}
			catch(Exception e)
			{
				messageArea.append("User has disconnected");
				return;
			}
		}
	}
	
	private void send() throws Exception
	{
		String message = chatArea.getText().trim();
		dos.writeUTF(message);
		messageArea.append("You said: " + message + newLine);
		messageArea.setCaretPosition(messageArea.getDocument().getLength());
		chatArea.setText("");
	}

	@Override
	public void actionPerformed(ActionEvent ae) 
	{
		if(ae.getSource() == sendButton)
		{
			try {
				send();
			} catch (Exception e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
	}

}
