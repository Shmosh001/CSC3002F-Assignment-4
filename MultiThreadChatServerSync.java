import java.io.DataInputStream;
import java.io.PrintStream;
import java.io.IOException;
import java.net.Socket;
import java.net.ServerSocket;

/*
 * A ch a t s e r v e r t h a t d e l i v e r s p u bli c and p r i v a t e me s sage s .
 */
public class MultiThreadChatServerSync
{
	// The s e r v e r s o c k e t .
	private static ServerSocket serverSocket = null;
	// The c l i e n t s o c k e t .
	private static Socket clientSocket = null;
	// This ch a t s e r v e r can a c c e p t up t o maxClientsCount c l i e n t s  c o n n e c ti o n s .
	private static final int maxClientsCount = 10;
	private static final clientThread[] threads = new clientThread[maxClientsCount];

	public static void main(String args[])
	{
		// The d e f a u l t p o r t number.
		int portNumber = 2222;
		if (args.length < 1)
		{
			System.out.println("Usage : java MultiThreadChatServerSync <portNumber>\n" + "Now using port number= "
					+ portNumber);
		}
		else
		{
			portNumber = Integer.valueOf(args[0]).intValue();
		}
		/*
		 * Open a s e r v e r s o c k e t on the portNumber ( d e f a u l t 2 2
		 * 2 2 ). Note t h a t we can not ch o o se a p o r t l e s s than 1023
		 * i f we a r e not p r i v i l e g e d u s e r s ( r o o t ).
		 */
		try
		{
			serverSocket = new ServerSocket(portNumber);
		}
		catch (IOException e)
		{
			System.out.println(e);
		}
		/*
		 * C re a te a c l i e n t s o c k e t f o r each c o n n e c ti o n and
		 * p a s s i t t o a new c l i e n t th re ad .
		 */
		while (true)
		{
			try
			{
				clientSocket = serverSocket.accept();
				int i = 0;
				for (i = 0; i < maxClientsCount; i++)
				{
					if (threads[i] == null)
					{
						(threads[i] = new clientThread(clientSocket, threads)).start();
						break;
					}
				}
				if (i == maxClientsCount)
				{
					PrintStream os = new PrintStream(clientSocket.getOutputStream());
					os.println("Server too busy. Try later.");
					os.close();
					clientSocket.close();
				}
			}
			catch (IOException e)
			{
				System.out.println(e);
			}
		}
	}
}

/*
 * The ch a t c l i e n t th re ad . This c l i e n t th re ad opens the inpu t
 * and the output s t re am s f o r a p a r t i c u l a r c l i e n t , ask the
 * c l i e n t  s name , i n f o rm s a l l the c l i e n t s c onnec ted t o
 * the s e r v e r about the f a c t t h a t a new c l i e n t has j oi n e d
 * the ch a t room , and a s l o n g a s i t r e c e i v e data , ech o s t h a
 * t data back t o a l l o t h e r c l i e n t s . The th re ad b r o a d c a s
 * t the incoming me s sage s t o a l l c l i e n t s and r o u t e s the p r i
 * v a t e message t o the p a r t i c u l a r c l i e n t . When a c l i e n t
 * l e a v e s the ch a t room t h i s th re ad i n f o rm s a l s o a l l the c
 * l i e n t s about t h a t and t e rmi n a t e s .
 */
class clientThread extends Thread
{
	private String clientName = null;
	private DataInputStream is = null;
	private PrintStream os = null;
	private Socket clientSocket = null;
	private final clientThread[] threads;
	private int maxClientsCount;

	public clientThread(Socket clientSocket, clientThread[] threads)
	{
		this.clientSocket = clientSocket;
		this.threads = threads;
		maxClientsCount = threads.length;
	}

	public void run()
	{
		int maxClientsCount = this.maxClientsCount;
		clientThread[] threads = this.threads;
		try
		{
			/*
			 * C re a te inpu t and output s t re am s f o r t h i s c l i e n t
			 * .
			 */
			is = new DataInputStream(clientSocket.getInputStream());
			os = new PrintStream(clientSocket.getOutputStream());
			String name;
			while (true)
			{
				os.println("Enter your name.");
				name = is.readLine().trim();
				if (name.indexOf('@') == -1)
				{
					break;
				}
				else
				{
					os.println("The name should not contain '@' character.");
				}
			}
			/* Welcome the new the c l i e n t . */
			os.println("Welcome " + name + " to our chat room.\n To leave enter /quit in a new line.");
			synchronized (this)
			{
				for (int i = 0; i < maxClientsCount; i++)
				{
					if (threads[i] != null && threads[i] == this)
					{
						clientName = "@" + name;
						break;
					}
				}
				for (int i = 0; i < maxClientsCount; i++)
				{
					if (threads[i] != null && threads[i] != this)
					{
						threads[i].os.println("* * * A new user " + name + " entered the chat room!!! * * *");
					}
				}
			}
			/* S t a r t the c o n v e r s a ti o n . */
			while (true)
			{
				String line = is.readLine();
				if (line.startsWith("/quit"))
				{
					break;
				}
				/*
				 * I f the message i s p r i v a t e s e n t i t t o the gi v e
				 * n c l i e n t .
				 */
				if (line.startsWith("@"))
				{
					String[] words = line.split("\\s", 2);
					if (words.length > 1 && words[1] != null)
					{
						words[1] = words[1].trim();
						if (!words[1].isEmpty())
						{
							synchronized (this)
							{
								for (int i = 0; i < maxClientsCount; i++)
								{
									if (threads[i] != null && threads[i] != this && threads[i].clientName != null
											&& threads[i].clientName.equals(words[0]))
									{
										threads[i].os.println("< " + name + "> " + words[1]);
										/*
										 * Echo t h i s message t o l e t the c
										 * l i e n t know the p r i v a t e
										 * message was s e n t .
										 */
										this.os.println("> " + name + "> " + words[1]);
										break;
									}
								}
							}
						}
					}
				}
				else
				{
					/*
					 * The message i s p u bli c , b r o a d c a s t i t t o a l
					 * l o t h e r c l i e n t s .
					 */
					synchronized (this)
					{
						for (int i = 0; i < maxClientsCount; i++)
						{
							if (threads[i] != null && threads[i].clientName != null)
							{
								threads[i].os.println("< " + name + "> " + line);
							}
						}
					}
				}
			}
			synchronized (this)
			{
				for (int i = 0; i < maxClientsCount; i++)
				{
					if (threads[i] != null && threads[i] != this && threads[i].clientName != null)
					{
						threads[i].os.println("* * * The user " + name + " is leaving the chat room!!! * * *");
					}
				}
			}
			os.println("* * * Bye " + name + " * * * ");
			/*
			 * Clean up . Se t the c u r r e n t th re ad v a r i a b l e t o n
			 * u l l s o t h a t a new c l i e n t c ould be a c c e p t e d by
			 * the s e r v e r .
			 */
			synchronized (this)
			{
				for (int i = 0; i < maxClientsCount; i++)
				{
					if (threads[i] == this)
					{
						threads[i] = null;
					}
				}
			}
			/*
			 * Cl o se the output stream , c l o s e the inp u t stream , c l o
			 * s e the s o c k e t .
			 */
			is.close();
			os.close();
			clientSocket.close();
		}
		catch (IOException e)
		{
		}
	}
}
