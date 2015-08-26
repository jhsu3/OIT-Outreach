import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Arrays;
import java.util.Date;
import java.util.Set;
import java.util.Vector;
import java.util.concurrent.ConcurrentHashMap;


public class PictureChatServer implements Runnable
{
public static void main(String[] args)
  {
  if (args.length != 0)
	  System.out.println("ChatServer does not accept any command line parameters.");

  try {
	  new PrivateChatServer(); // load program object
      }
  catch (Exception e)
      {
	  System.out.println(e);
      } 	
  }
//------------------------------------------------------------

ServerSocket ss;
int          threadNumber = 1;
ConcurrentHashMap<String,ObjectOutputStream>
   whosIn =	new ConcurrentHashMap<String,ObjectOutputStream>();
ConcurrentHashMap<String,String>
   passwords =	new ConcurrentHashMap<String,String>();
Vector<String> whosNotIn; 
ConcurrentHashMap<String,Vector<String>>
   savedMessages =	new ConcurrentHashMap<String,Vector<String>>();
  

//===========================================================
public PictureChatServer() throws Exception // constructor
  {
  ss = new ServerSocket(6666);

  // Restore passwords collection from disk
  try {
      ObjectInputStream ois = new ObjectInputStream(
                              new FileInputStream("Passwords.ser"));
      passwords = (ConcurrentHashMap<String,String>) ois.readObject();
      ois.close();
      }
  catch (FileNotFoundException fnfe)
      {
	  System.out.println("Passwords.ser is not found on disk, so we will be starting with an empty passwords collection.");
      }
  Set notInSet = passwords.keySet();
  whosNotIn = new Vector<String>(notInSet);

  // Restore savedMessages collection from disk
  try {
      ObjectInputStream ois = new ObjectInputStream(
                              new FileInputStream("SavedMessages.ser"));
      savedMessages = (ConcurrentHashMap<String,Vector<String>>) ois.readObject();
      ois.close();
      }
  catch (FileNotFoundException fnfe)
      {
	  System.out.println("SavedMessages.ser is not found on disk, so we will be starting with an empty messages collection.");
	  Set<String> keys = passwords.keySet();
	  for (String clientName : keys)
          {
          savedMessages.put(clientName, new Vector<String>());
          }
	  saveMessages();
      }
 
  
  System.out.println("PrivateChatServer is up at "
          + InetAddress.getLocalHost().getHostAddress()
          + " on port " + ss.getLocalPort());
  
  new Thread(this).start(); // make ourself a thread!   
  }


//============================================================
public void run() // app threads enter here
  {
  System.out.println("Application thread #"
		           + threadNumber++
                   + " entering run() to wait for a client to connect.");
  Socket             s        = null;
  ObjectInputStream  ois      = null;
  ObjectOutputStream oos      = null;
  String             chatName = null;
  String             password = null;
  try {
	  s = ss.accept(); // wait for a client to connect.
      new Thread(this).start();// make another application thread
	  System.out.println("Client has connected from "
	          + s.getInetAddress());
	  ois = new ObjectInputStream(s.getInputStream());
	  String firstMessage = (String) ois.readObject();
      oos = new ObjectOutputStream(s.getOutputStream());
      if (!firstMessage.contains("/"))
         {
    	 oos.writeObject("Sorry - wrong number!");
    	 oos.close(); // "flush" out the msg, then hang up.
    	 return; // and kill this client thread - we're done with them!
    	 }
      // JOIN PROCESSING
      int slashOffset = firstMessage.indexOf("/");
      chatName = firstMessage.substring(0,slashOffset).trim().toUpperCase();
      password = firstMessage.substring(slashOffset+1).trim();
      System.out.println(chatName + " is attempting to join with password " + password);
      // See if Bubba is already in the chat room
      if (whosIn.containsKey(chatName)) // already joined
         {
    	 System.out.println(chatName + " join rejected due to duplicate chat name."); 
         oos.writeObject("Duplicate chat name. Please rejoin with a different name.");
         oos.close(); // "flush" out the msg, then hang up.
         return; // and kill this client thread - we're done with them!
         }
      // does a password exist for this chat name?
      if (passwords.containsKey(chatName)) // previously joined
         {
    	 String storedPassword = passwords.get(chatName);
    	 if (!password.equals(storedPassword))
    	    {
        	System.out.println(chatName + " join rejected due to invalid password."); 
            oos.writeObject("Entered password does not match stored password.");
            oos.close(); // "flush" out the msg, then hang up.
            return; // and kill this client thread - we're done with them!
    	    }
         }
       else//new never-joined client wants to join
         {
    	 passwords.put(chatName, password);
    	 savePasswords();
    	 savedMessages.put(chatName, new Vector<String>());
    	 saveMessages();
         }
      //At this point either a new client is added,
      //or a previous client has passed the password test
      //So send them the welcome message!
      oos.writeObject("Welcome " + chatName);
      whosIn.put(chatName, oos);
      whosNotIn.remove(chatName);
      System.out.println(chatName + " has joined!");
      String[] whosInArray = whosIn.keySet().toArray(new String[0]);
      Arrays.sort(whosInArray);
      sendToAll(whosInArray); // sorted
      String[] whosNotInArray = whosNotIn.toArray(new String[0]);
      Arrays.sort(whosNotInArray);
      sendToAll(whosNotInArray); // sorted
      sendToAll("Welcome to " + chatName + " who has just joined the chat room.");
      // send any messages saved for this client
      Vector<String> savedMessageList = savedMessages.get(chatName);
      if (savedMessageList == null)
         {
         System.out.println("SYSTEM ERROR: No message Vector found for " + chatName + " at join.");
         oos.close(); // hang up
         return; // kill client thread
         }
      boolean msgListWasModified = false;
      while(!savedMessageList.isEmpty())//send until empty
           {
           String savedMessage = savedMessageList.remove(0);// get #1
           msgListWasModified = true;
           try {
               oos.writeObject(savedMessage); 
               }
           catch(IOException ioe) // joiner probably changed
               {                  // their mind and suddenly left.
               savedMessageList.add(savedMessage);// put it back
               break;//don't expect any following sends to work.
               }
           }
      if (msgListWasModified) saveMessages();
      }// end of join processing
  catch (ClassNotFoundException cnfe) {}
  catch (IOException e)
      {
	  System.out.println("Unsuccessful connection attempt from a potential 'client'.");
	  return; // KILL this client thread!
      }
  
  // RECEIVE Processing
  try {
      while (true) // capture client thread
        {
    	Object somethingFromMyClient = ois.readObject();
    	if (somethingFromMyClient instanceof String)
    	   {
           String message = (String) somethingFromMyClient;
           sendToAll(chatName + " says: " + message);
           System.out.println("Received " + message + " from " + chatName);
           continue; // receive next thing
    	   }
    	if (somethingFromMyClient instanceof String[])
 	       {
           String[] messageWithRecipients = (String[]) somethingFromMyClient;
 	       if (whosIn.containsKey(messageWithRecipients[1])) // recipient is in
 	    	   sendPrivateMessage(messageWithRecipients,chatName);
 	        else // recipient is not currently in the chat room
  	    	   saveAmessage(messageWithRecipients,chatName);
 	       continue;
 	       }
    	System.out.println("Unrecognized object received: " + somethingFromMyClient);
    	sendToAll(somethingFromMyClient);
        }
      }
  catch(ClassNotFoundException cnfe) {}
  catch(IOException ioe) // LEAVE Processing 
      {
	  whosIn.remove(chatName);
	  whosNotIn.add(chatName);
	  sendToAll("Goodbye to " + chatName + " who has left the chat room.");
	  System.out.println(chatName + " has left.");
      String[] whosInArray = whosIn.keySet().toArray(new String[0]);
	  Arrays.sort(whosInArray);
      sendToAll(whosInArray); // sorted
      String[] whosNotInArray = whosNotIn.toArray(new String[0]);
       Arrays.sort(whosNotInArray);
      sendToAll(whosNotInArray); // sorted
      }
  // On exit, client thread returns to it's Thread object and is terminated.
  }//end of run()

//==========================================================
private synchronized void sendToAll(Object objectToSend)
  {
  ObjectOutputStream[] oosList = whosIn.values().toArray(new ObjectOutputStream[0]);
  for (ObjectOutputStream clientOOS : oosList)
      {
	  try{ clientOOS.writeObject(objectToSend);}
	  catch(IOException ioe){} // go on to next client
      }                        // (this one just left)  
  }


//==========================================================
private synchronized void sendPrivateMessage(String[] messageWithRecipients, String senderChatName)
 {
 System.out.println("Private message received from " + senderChatName);
 String message = messageWithRecipients[0];
 String intendedRecipients = "";	 
 String actualRecipients = "";//add to as they are sent

 for (int i = 1; i < messageWithRecipients.length; i++)
      intendedRecipients += messageWithRecipients[i] + ",";
 intendedRecipients = intendedRecipients.substring(0,intendedRecipients.length()-1);//drop last comma
 
 for (int i = 1; i < messageWithRecipients.length; i++)
     {
	 String recipient = messageWithRecipients[i];
	 try {
		 ObjectOutputStream recipientOOS = whosIn.get(recipient);
		 if (recipientOOS == null) continue;//must have left the chat room!
		 recipientOOS.writeObject("PRIVATE from " + senderChatName + " to " + intendedRecipients + " : " + message); 
		 actualRecipients += recipient + ",";
	     }
	 catch(IOException ioe)
	     {
		 continue; // they must have left the chat room
	     }
     }
 actualRecipients = actualRecipients.substring(0,actualRecipients.length()-1);//drop last comma
 System.out.println("Private message from " + senderChatName + " sent to " + actualRecipients);
 ObjectOutputStream senderOOS = whosIn.get(senderChatName);
 if (senderOOS == null) return; // they left the chat room!
 try {
	 senderOOS.writeObject("Your PRIVATE: " + message + " was sent to " + actualRecipients);
     }
 catch(IOException ioe) {} // Oh well, we tried!
 }
 
 
//=========================================================== 
private synchronized void saveAmessage(String[] messageWithRecipients, String senderChatName)
 {
 System.out.println("Save message received from " + senderChatName);
 String message = messageWithRecipients[0];
 String recipients = "";//add to as they are saved
 for (int i = 1; i < messageWithRecipients.length; i++)
     {
	 String recipient = messageWithRecipients[i];
    	 Vector<String> savedMessageList = savedMessages.get(recipient);
	     if (savedMessageList == null) 
	        {
            System.out.println("SYSTEM ERROR: No saved message Vector found for " + recipient);
	    	continue;//to save this message for others
            }  
	     savedMessageList.add("Message from " + senderChatName + " was saved for you on " + new Date() + " : " + message); 
	     recipients += recipient + ",";
     }
 saveMessages(); // save all updated Vectors
 recipients = recipients.substring(0,recipients.length()-1);//drop last comma
 System.out.println("message from " + senderChatName + " saved for to " + recipients);
 ObjectOutputStream senderOOS = whosIn.get(senderChatName);
 if (senderOOS == null) return; // they left the chat room!
 try {
     senderOOS.writeObject("Your message " + message + " was saved for " + recipients);
     }
 catch(IOException ioe) {} // Oh well, we tried!
 }
	   
	   
//==========================================================
private synchronized void savePasswords() throws IOException
  {
  ObjectOutputStream oos = new ObjectOutputStream(
                           new FileOutputStream("Passwords.ser"));
  oos.writeObject(passwords);
  oos.close();
  }


//==========================================================
private synchronized void saveMessages() 
  {
  try {	
      ObjectOutputStream oos = new ObjectOutputStream(
                               new FileOutputStream("SavedMessages.ser"));
      oos.writeObject(savedMessages);
      oos.close();
      }
  catch(IOException ioe)
      {
	  System.out.println("SYSTEM ERROR: Problem saving message file! " + ioe);  
      }
  }
}
