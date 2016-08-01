package chatToATG;

import java.awt.Font;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import javax.swing.JFrame;
import javax.swing.JOptionPane;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.text.BadLocationException;
import javax.swing.text.Utilities;

public class ChatClient {
	BufferedReader in;
	PrintWriter out;
	JFrame frame = new JFrame("Chatt");
	JTextField textField = new JTextField(40);
	JTextArea messageArea = new JTextArea(8, 40);
	JTextArea activeUsers = new JTextArea(5, 40);

	/**
	 * Constructs the client by laying out the GUI and registering a listener with the textfield so that pressing Return
	 * in the listener sends the textfield contents to the server. Note however that the textfield is initially NOT
	 * editable, and only becomes editable AFTER the client receives the NAMEACCEPTED message from the server.
	 */
	public ChatClient() {

		// Layout GUI
		textField.setEditable(false);
		messageArea.setEditable(false);
		activeUsers.setEditable(false);
		frame.getContentPane().add(textField, "North");
		frame.getContentPane().add(new JScrollPane(messageArea), "Center");
		frame.getContentPane().add(new JScrollPane(activeUsers), "East");
		frame.pack();

		// Add lister for mouseClick
		// Used later...
		activeUsers.addMouseListener(new MouseListener() {

			@Override
			public void mouseReleased(MouseEvent arg0) {
				// TODO Auto-generated method stub

			}

			@Override
			public void mousePressed(MouseEvent arg0) {
				// TODO Auto-generated method stub

			}

			@Override
			public void mouseExited(MouseEvent arg0) {
				// TODO Auto-generated method stub

			}

			@Override
			public void mouseEntered(MouseEvent arg0) {
				// TODO Auto-generated method stub

			}

			@Override
			public void mouseClicked(MouseEvent arg0) {

				if (arg0.getButton() != MouseEvent.BUTTON1) {
					return;
				}
				// Only handle double clicks
				if (arg0.getClickCount() != 2) {
					return;
				}

				int offset = activeUsers.viewToModel(arg0.getPoint());

				try {
					int rowStart = Utilities.getRowStart(activeUsers, offset);
					int rowEnd = Utilities.getRowEnd(activeUsers, offset);
					String selectedLine = activeUsers.getText().substring(rowStart, rowEnd);
					System.out.println("Kickat på " + selectedLine);

				} catch (BadLocationException e1) {
					e1.printStackTrace();
				}
			}
		});

		// Add Listeners
		textField.addActionListener(new ActionListener() {
			/**
			 * < * Responds to pressing the enter key in the textfield by sending the contents of the text field to the
			 * server. Then clear the text area in preparation for the next message.
			 */
			public void actionPerformed(ActionEvent e) {
				out.println(textField.getText());
				textField.setText("");
			}
		});
	}

	/**
	 * Prompt for and return the address of the server.
	 */
	private String getServerAddress() {
		return JOptionPane.showInputDialog(frame, "Enter IP Address of the Server: (localhost)",
				"Welcome to the Chatter", JOptionPane.QUESTION_MESSAGE);
	}

	/**
	 * Prompt for and return the desired screen name.
	 */
	private String getName() {
		String name = "null";
		while (name.equals("null") || name == null) {
			name = null;
			name = JOptionPane.showInputDialog(frame, "Login User and set a screen name:", "Screen name selection",
					JOptionPane.PLAIN_MESSAGE);
		}
		if (!frame.getTitle().equals(name)) {
			frame.setTitle(name.toUpperCase());
		}

		return name;
	}

	/**
	 * Initialize contacts and status to users window and return a readable contactlist
	 * 
	 * @param contacts
	 * @return edited and readable contactlist
	 */
	private List<String> setContactList(List<String> contacts) {
		List<String> contactList = new ArrayList<>();
		activeUsers.setFont(new Font(null, Font.BOLD, 20));
		activeUsers.append("Contacts and Status" + "\n");
		activeUsers.setFont(null);
		int index = 0;
		for (String contactUser : contacts) {
			activeUsers.append(contactUser.substring(contactUser.indexOf("<") + 1, contactUser.indexOf("v<") + 2)
					+ "\n");
			contactList.add(index, contactUser.substring(contactUser.indexOf("<") + 1, contactUser.indexOf("v<") + 2)
					+ "\n");
			index = index + 1;
		}
		return contactList;
	}

	/**
	 * Connects to the server then enters the processing loop.
	 */
	private void run() throws IOException {

		// Make connection and initialize streams
		String serverAddress = getServerAddress();
		Socket socket = new Socket(serverAddress, 9001);
		in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
		out = new PrintWriter(socket.getOutputStream(), true);
		List<String> contacts = new ArrayList<>();
		String name = null;
		// Process all messages from server, according to the protocol.
		while (true) {
			String line = in.readLine();

			if (line.startsWith("SUBMITNAME")) {
				name = getName();
				out.println(name);
			} else if (line.startsWith("NAMEACCEPTED")) {
				textField.setEditable(true);
			} else if (line.startsWith("MESSAGE")) {
				// Show own message in own feed
				if ((line.substring(8, line.indexOf(":")).equalsIgnoreCase(frame.getTitle()))) {
					messageArea.append(line.substring(8) + "\n");
				} else {
					for (String nameOnContactList : contacts) {
						// Check if user is on contactlist, then show message
						if (nameOnContactList.substring(0, nameOnContactList.indexOf(":")).equalsIgnoreCase(
								line.substring(8, line.indexOf(":")))) {
							messageArea.append(line.substring(8) + "\n");
						}
					}
				}
			} else if (line.startsWith("[CONTACTLIST")) {
				List<String> contactsFromServer = Arrays.asList(line
						.substring(line.indexOf("[") + 1, line.indexOf("]")).split("\\s*,\\s*"));
				contacts = setContactList(contactsFromServer);
			} else if (line.startsWith("USERSTATUS")) {
				// Dont update own window with own status
				if (!line.substring(11, line.indexOf(":")).equalsIgnoreCase(frame.getTitle())) {
					activeUsers.setText(null);
					activeUsers.setFont(new Font(Font.SERIF, Font.BOLD, 12));
					activeUsers.append("Contacts and Status" + "\n");

					int index = 0;

					for (String nameOnContactList2 : contacts) {
						// Check for new status for a user on contactlist, if not dont update status.
						if (line.substring(11, line.indexOf(":")).equalsIgnoreCase(
								nameOnContactList2.substring(0, nameOnContactList2.indexOf(":")))) {
							activeUsers.append(line.substring(11, line.indexOf("<")) + "\n");
							contacts.set(index, "");
							contacts.set(index, (nameOnContactList2.substring(0, nameOnContactList2.indexOf(":")) + ":"
									+ line.substring((line.indexOf(":") + 1), line.indexOf("<")) + "<"));
							System.out.println("Aktiva användare: " + contacts.get(index));
							index = index + 1;

						} else {
							activeUsers.append(nameOnContactList2.substring(0, nameOnContactList2.indexOf("<")) + "\n");
							contacts.set(index, "");
							contacts.set(index, (nameOnContactList2));
							System.out.println("Uppdaterad användare inaktiv: "
									+ contacts.get(index).substring(0, contacts.get(index).indexOf("<")));
							index = index + 1;
						}
					}
				}
			}
		}
	}

	/**
	 * Runs the client as an application with a closeable frame.
	 */
	public static void main(String[] args) throws Exception {
		ChatClient client = new ChatClient();
		client.frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		client.frame.setVisible(true);
		client.run();
	}
}