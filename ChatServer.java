import java.io.*;
import java.net.*;
import java.util.*;


// This is Server code
public class ChatServer {
    private static final int PORT = 12345;
    private static Set<String> usernames = new HashSet<>();
    private static List<ClientHandler> clients = new ArrayList<>();

    public static void main(String[] args) {
        try (ServerSocket serverSocket = new ServerSocket(PORT)) {
            System.out.println("Server started...");
            while (true) {
                Socket socket = serverSocket.accept();
                System.out.println("New client connected: " + socket);

                ClientHandler client = new ClientHandler(socket);
                clients.add(client);
                client.start();
            }
        } catch (IOException ex) {
            System.out.println("Server exception: " + ex.getMessage());
            ex.printStackTrace();
        }
    }

    static class ClientHandler extends Thread {
        private Socket socket;
        private PrintWriter out;
        private BufferedReader in;
        private String username;

        public ClientHandler(Socket socket) {
            this.socket = socket;
        }

        @Override
        public void run() {
            try {
                out = new PrintWriter(socket.getOutputStream(), true);
                in = new BufferedReader(new InputStreamReader(socket.getInputStream()));

                out.println("Enter your username:");
                username = in.readLine();
                synchronized (usernames) {
                    while (usernames.contains(username)) {
                        out.println("Username already taken. Enter a different username:");
                        username = in.readLine();
                    }
                    usernames.add(username);
                }

                broadcast(username + " joined the chat.");

                String input;
                while ((input = in.readLine()) != null) {
                    if (input.equalsIgnoreCase("/exit")) {
                        break;
                    } else if (input.startsWith("/private")) {
                        String[] parts = input.split(" ", 3);
                        if (parts.length >= 3) {
                            String recipient = parts[1];
                            String message = parts[2];
                            sendPrivateMessage(username, recipient, message);
                        }
                    } else {
                        broadcast(username + ": " + input);
                    }
                }
            } catch (IOException ex) {
                System.out.println("Client exception: " + ex.getMessage());
            } finally {
                try {
                    socket.close();
                    clients.remove(this);
                    usernames.remove(username);
                    broadcast(username + " left the chat.");
                } catch (IOException ex) {
                    ex.printStackTrace();
                }
            }
        }

        private void broadcast(String message) {
            synchronized (clients) {
                for (ClientHandler client : clients) {
                    client.out.println(message);
                }
            }
        }

        private void sendPrivateMessage(String sender, String recipient, String message) {
            synchronized (clients) {
                for (ClientHandler client : clients) {
                    if (client.username.equals(recipient)) {
                        client.out.println("(Private) " + sender + ": " + message);
                        break;
                    }
                }
            }
        }
    }
}
