package ru.kolts.java.chat.server;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;

public class ClientHandler {
    private Server server;
    private Socket socket;
    private DataInputStream in;
    private DataOutputStream out;

    private String username;
    private static int userCount = 0;

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public ClientHandler(Server server, Socket socket) throws IOException {
        this.server = server;
        this.socket = socket;
        this.in = new DataInputStream(socket.getInputStream());
        this.out = new DataOutputStream(socket.getOutputStream());
        do {
            userCount++;
        } while (nameExists("user" + userCount));
        username = "user" + userCount;
        new Thread(() -> {
            try {
                System.out.println("Клиент подключился ");
                sendMessage("Type \"/help\" to get list of commands");
                while (true) {
                    String message = in.readUTF();
                    if (message.startsWith("/")) {
                        if (message.equals("/exit")) {
                            sendMessage("/exitok");
                            break;
                        }
                        if (message.startsWith("/w")) {
                            sendPrivateMessage(message, "/w");
                        }
                        if (message.startsWith("/changename")) {
                            changeName(message, "/changename");
                        }
                        if (message.equals("/name")) {
                            sendMessage(getUsername());
                        }
                        if (message.equals("/help")) {
                            listCommands();
                        }
                    } else {
                        server.broadcastMessage(username + " : " + message);
                    }
                }
            } catch (IOException e) {
                e.printStackTrace();
            } finally {
                disconnect();
            }
        }).start();
    }

    public void sendMessage(String message) {
        try {
            out.writeUTF(message);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void disconnect() {
        server.unsubscribe(this);
        try {
            in.close();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        try {
            out.close();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        try {
            socket.close();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public void listCommands() {
        sendMessage("\n\"/w [name] [message]\" - send message to another client" +
                "\n\"/changename [name]\" - change name" +
                "\n\"/name\" - check name" +
                "\n\"/exit\" - exit client");
    }

    public void sendPrivateMessage(String message, String command) {
            String[] parts = message.trim().split(" ", 3);
            if (!parts[0].equals(command) || parts.length != 3 || !nameExists(parts[1].trim())) {
                sendMessage("Fail. Use: " + command + " [username] [message]");
                return;
            }
            String name = parts[1].trim();
            String privateMessage = parts[2].trim();
            server.msgClientToClient(name, username + " : " + privateMessage);
    }

    public void changeName(String message, String command) {
        String[] parts = message.replaceAll("\\s{2,}", " ").split(" ");
        if (!parts[0].equals(command) || parts.length != 2) {
            sendMessage("Fail. Use: " + command + " [name]");
            return;
        }
        String newName = parts[1].trim();
        if (newName.isEmpty() || nameExists(newName)) {
            sendMessage("Fail. Use: " + command + " [name]");
            return;
        }
        setUsername(newName);
        sendMessage("Success");
    }

    public boolean nameExists(String username) {
        for (ClientHandler client : server.getClients()) {
            if (client.getUsername().equals(username)) {
                return true;
            }
        }
        return false;
    }
}
