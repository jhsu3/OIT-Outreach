import java.awt.Color;
import java.awt.Font;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.Arrays;
import java.util.List;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTextArea;


public class PictureChatClient implements ActionListener, Runnable
{
public static void main(String[] args)
  {
  if (args.length != 3)
      {
	  System.out.println("Provide ChatServer network address and your chat name and password as 3 command line parameters."); 
	  return; 
      }
  
  try {
	  new PrivateChatClient(args[0],args[1],args[2]);
      }
  catch (Exception e)        // catch ANY kind of Exception! 
      {
      System.out.println(e); // print the Exception object	  
      }                      // as the error message!
  }

Socket             s;
ObjectInputStream  ois;
ObjectOutputStream oos;
String             chatName;
String             newLine = System.lineSeparator();

// Who's-in window GUI objects
JFrame  whosInWindow         = new JFrame("Who's IN (Hold Ctrl to select multiple)");
JList<String> whosInList     = new JList<String>();
JScrollPane whosInScrollPane = new JScrollPane(whosInList);
JButton clearWhosInButton    = new JButton("CLEAR");
JButton sendPrivateButton    = new JButton("Send Private");

//Who's-not-in window GUI objects
JFrame  whosNotInWindow      = new JFrame("Who's NOT in (Hold Ctrl to select multiple)");
JList<String> whosNotInList  = new JList<String>();
JScrollPane whosNotInScrollPane = new JScrollPane(whosNotInList);
JButton clearWhosNotInButton = new JButton("CLEAR");
JButton saveMessageButton    = new JButton("Save Message");

// Chat window GUI Objects
JFrame      chatWindow       = new JFrame();
JLabel      chatLabel        = new JLabel("Move bar to allocate more space to input or output chat area.");
JButton     sendToAllButton  = new JButton("Send To All");     
JTextArea   inChatArea       = new JTextArea();
JTextArea   outChatArea      = new JTextArea();
JScrollPane inChatScrollPane = new JScrollPane(inChatArea);
JScrollPane outChatScrollPane= new JScrollPane(outChatArea);
JSplitPane  chatPane         = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT,
                               inChatScrollPane, outChatScrollPane);
JMenuBar    menuBar          = new JMenuBar();
JMenu       chatMenu         = new JMenu("ScreenOrientation");
JMenuItem   horizontalSplit  = new JMenuItem("horizontal");
JMenuItem   verticalSplit    = new JMenuItem("vertical");
	
public PictureChatClient(String serverAddress,String chatName,String password)
       throws UnknownHostException, IOException, IllegalArgumentException,ClassNotFoundException // constructor
  {
  // save chat name
  this.chatName = chatName;
  
  // Edit input parms	
  if ((serverAddress == null)
   || (serverAddress.trim().length() == 0)
   ||  serverAddress.trim().contains(" "))
     throw new IllegalArgumentException("serverAddress is null, zero length, or contains blank(s)");
   else
	 serverAddress = serverAddress.trim();
  
  if ((chatName == null)
   || (chatName.trim().length() == 0)
   ||  chatName.trim().contains(" "))
	 throw new IllegalArgumentException("chat name is null, zero length, or contains blank(s)");
   else
	 chatName = chatName.trim(); 

  if ((password == null)
   || (password.trim().length() == 0)
   ||  password.trim().contains(" "))
		     throw new IllegalArgumentException("serverAddress is null, zero length, or contains blank(s)");
   else
	   password = password.trim(); 
  
  // CONNECT
  System.out.println("Connecting to PictureChatServer at "
                   + serverAddress + " on port 6666");
  s = new Socket(serverAddress, 6666);
  System.out.println("Connected to ChatServer!");
  oos = new ObjectOutputStream(s.getOutputStream());
  oos.writeObject(chatName + "/" + password);
  ois = new ObjectInputStream(s.getInputStream());
  String serverReply = (String) ois.readObject();
  if (serverReply.startsWith("Welcome"))
     {
	 System.out.println(serverReply);
	 new Thread(this).start();//make application thread
     }
   else
     throw new IllegalArgumentException(serverReply);//RETURN!
  
  // Build GUIs
  whosInWindow.getContentPane().add(clearWhosInButton,"North");
  whosInWindow.getContentPane().add(whosInScrollPane,"Center");
  whosInWindow.getContentPane().add(sendPrivateButton,"South");
  whosInWindow.setSize(200,200);
  whosInWindow.setLocation(200,0);
  whosInWindow.setVisible(true);
  whosInWindow.setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);

  whosNotInWindow.getContentPane().add(clearWhosNotInButton,"North");
  whosNotInWindow.getContentPane().add(whosNotInScrollPane,"Center");
  whosNotInWindow.getContentPane().add(saveMessageButton,"South");
  whosNotInWindow.setSize(200,200);
  whosNotInWindow.setLocation(400,0);
  whosNotInWindow.setVisible(true);
  whosNotInWindow.setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);

  chatWindow.getContentPane().add(chatLabel,"North");
  chatWindow.getContentPane().add(chatPane,"Center");
  chatWindow.getContentPane().add(sendToAllButton,"South");
  chatWindow.setSize(500,300);
  chatWindow.setLocation(100,200);
  chatWindow.setVisible(true);
  chatWindow.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

  // Build and show menu
  chatMenu.add(horizontalSplit);
  chatMenu.add(verticalSplit);
  menuBar.add(chatMenu);
  chatWindow.setJMenuBar(menuBar);

  // Set GUI attributes
  outChatArea.setEditable(false); // keep cursor out
  chatWindow.setTitle(chatName + "'s Chat Room! Alt-ENTER will also send-to-all. Close the window to leave the Chat Room.");
  inChatArea.setFont (new Font("default",Font.BOLD,20));
  outChatArea.setFont(new Font("default",Font.BOLD,20));
  inChatArea.setLineWrap(true);
  outChatArea.setLineWrap(true);
  inChatArea.setWrapStyleWord(true);
  outChatArea.setWrapStyleWord(true);
  sendToAllButton.setBackground(Color.green);
  sendToAllButton.setForeground(Color.white);
  sendPrivateButton.setBackground(Color.red);
  sendPrivateButton.setForeground(Color.white);
  saveMessageButton.setBackground(Color.blue);
  saveMessageButton.setForeground(Color.white);

  chatWindow.setFont(new Font("default", Font.BOLD, 20));

  // Activate GUI objects for events
  sendToAllButton.addActionListener(this);//give it our address
  sendToAllButton.setMnemonic(KeyEvent.VK_ENTER);//same as pushing button
  horizontalSplit.addActionListener(this);
  verticalSplit.addActionListener(this);
  clearWhosInButton.addActionListener(this);
  sendPrivateButton.addActionListener(this);
  clearWhosNotInButton.addActionListener(this);
  saveMessageButton.addActionListener(this);    

  }


public void run() // our app thread enters here
  {
  System.out.println("Application thread enters run() to receive chat.");	
  while(true) // CAPTURE application thread!
    {
    try {
        Object somethingFromTheServer = ois.readObject(); // wait for server to send     
        if (somethingFromTheServer instanceof String[]) 
           {
           String[] clientList = (String[]) somethingFromTheServer;
           System.out.println(Arrays.toString(clientList));
           if (Arrays.binarySearch(clientList, chatName.toUpperCase()) >= 0 )
               whosInList.setListData(clientList);
            else // no find our chat name
               whosNotInList.setListData(clientList);
           }
  else if (somethingFromTheServer instanceof String) 
           {
	       String chatMessage = (String) somethingFromTheServer;
           outChatArea.append(newLine + chatMessage);
           // scroll the outChatArea to the bottom
           outChatArea.setCaretPosition(outChatArea.getDocument().getLength()); 
           }
         else // we don't recognize this type of object!
           System.out.println("Unrecognized object received from server: " + somethingFromTheServer);
        }
    catch (ClassNotFoundException cnfe) {}
    catch (IOException ioe)
        {
        System.out.println("Failure of receive connection!");
        return; // kill receive thread
        }
    }
 }


public void actionPerformed(ActionEvent ae) // GUI thread calls here
  {
  chatLabel.setText(""); // clear
  
  if (ae.getSource() == sendToAllButton)
    {
    String chatToSend = inChatArea.getText().trim();
    if (chatToSend.length() == 0)
       {
	   chatLabel.setForeground(Color.red); 
	   chatLabel.setText("NO MESSAGE TO SEND!"); 
	   return; 
	   }
    inChatArea.setText(""); // clear
    System.out.println("Sending " + chatToSend);
    try {
	    oos.writeObject(chatToSend);
        }
    catch (IOException e) {} // catch in send
    return;
    } 

  
  if (ae.getSource() == horizontalSplit)
     {
	 System.out.println("horizontalSplit selected"); 
	 chatPane.setOrientation(JSplitPane.HORIZONTAL_SPLIT);
	 return;
     }

  
  if (ae.getSource() == verticalSplit)
     {
	 System.out.println("horizontalSplit selected"); 
	 chatPane.setOrientation(JSplitPane.VERTICAL_SPLIT);
	 return;
     }
  
  
  if (ae.getSource() == clearWhosInButton)
     {
	 whosInList.clearSelection(); 
	 return;  
     }

  
  if (ae.getSource() == clearWhosNotInButton)
     {
	 whosNotInList.clearSelection(); 
	 return;  
     }

  
  if (ae.getSource() == sendPrivateButton)
     {
	 String chatMessage = inChatArea.getText().trim();
	 if (chatMessage.length() == 0)
	    {
		chatLabel.setForeground(Color.red); 
		chatLabel.setText("NO MESSAGE TO SEND!"); 
		return; 
	    }
     List<String> recipients = whosInList.getSelectedValuesList();
     if (recipients.isEmpty())
        {
 		chatLabel.setForeground(Color.red); 
 		chatLabel.setText("NO RECIPIENTS ARE SELECTED"); 
 		return; 
 	    }
     inChatArea.setText(""); // clear
     System.out.println("Sending " + chatMessage + " to private message recipients: " + recipients);
	 String[] privateArray = new String[recipients.size()+1];
     privateArray[0] = chatMessage;
	 for (int i = 1; i < privateArray.length; i++)
		 privateArray[i] = recipients.get(i-1);
	 try {oos.writeObject(privateArray);}
	 catch(IOException ioe) {} // let run() handle 
     return; 
     }
  
  
  if (ae.getSource() == saveMessageButton)    
     {
	 String chatMessage = inChatArea.getText().trim();
	 if (chatMessage.length() == 0)
	    {
		chatLabel.setForeground(Color.red); 
		chatLabel.setText("NO MESSAGE TO SAVE!"); 
		return; 
	    }
     List<String> recipients = whosNotInList.getSelectedValuesList();
     if (recipients.isEmpty())
        {
		chatLabel.setForeground(Color.red); 
		chatLabel.setText("NO RECIPIENTS ARE SELECTED"); 
		return; 
	    }
     inChatArea.setText(""); // clear
     System.out.println("Saving message: " + chatMessage + " for recipients: " + recipients);
	 String[] saveArray = new String[recipients.size()+1];
	 saveArray[0] = chatMessage;
	 for (int i = 1; i < saveArray.length; i++)
		 saveArray[i] = recipients.get(i-1);
	 try {oos.writeObject(saveArray);}
	 catch(IOException ioe) {} // let run() handle 
	 return; 
     }
  }
}
