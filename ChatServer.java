package chatToATG;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.HashSet;

public class ChatServer {
	/**
	 * The port that the server listens on.
	 */
	private static final int PORT = 9001;

	/**
	 * The set of all names of clients in the chat room. Maintained so that we can check that new clients are not
	 * registering name already in use.
	 */
	private static HashSet<String> names = new HashSet<String>();

	/**
	 * The set of all the print writers for all the clients. This set is kept so we can easily broadcast messages to the
	 * clients.
	 */
	private static HashSet<PrintWriter> writers = new HashSet<PrintWriter>();

	/**
	 * The appplication main method, which just listens on a port and spawns handler threads.
	 */
	public static void main(String[] args) throws Exception {
		System.out.println("The chat server is running on port: " + PORT);
		ServerSocket listener = new ServerSocket(PORT);
		try {
			while (true) {
				new Handler(listener.accept()).start();
			}
		} finally {
			listener.close();
		}
	}

	/**
	 * A handler thread class. Handlers are spawned from the listening loop and are responsible for a dealing with a
	 * single client and broadcasting its messages.
	 */
	private static class Handler extends Thread {
		private String name;
		private Socket socket;
		private BufferedReader in;
		private PrintWriter out;

		/**
		 * Constructs a handler thread, squirreling away the socket. All the interesting work is done in the run method.
		 */
		public Handler(Socket socket) {
			this.socket = socket;
		}

		/**
		 * Services this thread's client by repeatedly requesting a screen name until a unique one has been submitted,
		 * then acknowledges the name and registers the output stream for the client in a global set, then repeatedly
		 * gets inputs and broadcasts them.
		 */
		public void run() {
			try {

				// Create character streams for the socket.
				in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
				out = new PrintWriter(socket.getOutputStream(), true);
				// Contactlist sent to client
				ArrayList<String> contactlist = new ArrayList<>();

				// Request a name from this client. Keep requesting until
				// a name is submitted that is not already used. Note that
				// checking for the existence of a name and adding the name
				// must be done while locking the set of names.
				while (true) {
					out.println("SUBMITNAME");
					name = in.readLine();
					if (name == null) {
						return;
					}
					synchronized (names) {
						if (!names.contains(name)) {
							names.add(name);
							break;
						}
					}

				}

				// Now that a successful name has been chosen, add the
				// socket's print writer to the set of all writers so
				// this client can receive broadcast messages.

				out.println("NAMEACCEPTED");
				writers.add(out);

				/**
				 * Find contacts for user and send to client. This could be replaced by a DB call.
				 */
				contactlist = getContacts(name);
				out.println(contactlist);

				/**
				 * For all active users update status to "Active" names contains all active users
				 */
				for (String names2 : names) {
					updateNewStatus(names2, "Aktiv<");
				}

				// Accept messages from this client and broadcast them.
				// Ignore other clients that cannot be broadcasted to.
				while (true) {
					String input = in.readLine();
					if (input == null) {
						return;
					}
					for (PrintWriter writer : writers) {
						writer.println("MESSAGE " + name + ": " + input);
					}
				}
			} catch (IOException e) {
				System.out.println(e);
			} finally {
				// This client is going down! Remove its name and its print
				// writer from the sets, and close its socket.
				// Update status to inactive
				if (name != null) {

					names.remove(name);
					updateNewStatus(name, "Inaktiv<");

				}
				if (out != null) {
					writers.remove(out);
				}
				try {
					socket.close();
				} catch (IOException e) {
				}
			}
		}
	}

	/**
	 * Update new status for each user
	 * 
	 * @param name
	 * @param status
	 */
	private static void updateNewStatus(String name, String status) {
		for (PrintWriter writer : writers) {
			writer.println("USERSTATUS " + name + ": " + status);
			// Add db update here future functionality.
		}
	}

	/**
	 * Get contactlist for each user. Empty list if no contactlist foudn
	 * 
	 * @param name
	 *            Name of User
	 * @return List of contacts
	 */
	private static ArrayList<String> getContacts(String name) {
		String fileName = "c://Micke/" + name + ".txt";
		ArrayList<String> localContactsList = new ArrayList<>();
		try (BufferedReader br = new BufferedReader(new FileReader(fileName))) {

			String line;
			int index = 0;
			while ((line = br.readLine()) != null) {
				localContactsList.add(index, "CONTACTLIST: for>" + name + "<" + line + ":Inaktiv<");
				index = index + 1;
			}
		} catch (FileNotFoundException fe) {
			System.out.println("No contacts found for: " + name + "Return empty contactlist");
			return localContactsList;
		} catch (IOException e) {
			e.printStackTrace();
			return localContactsList;
		}
		return localContactsList;
	}
}
