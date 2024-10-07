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
        userCount++;
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

                        processCommands(message);

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

    public void processCommands(String message) {
        if (message.startsWith("/w ")) {
            String[] parts = message.split(" ", 3);
            server.msgClientToClient(parts[1], username + " : " + parts[2].trim());
        }

        if (message.startsWith("/changename ")) {
            String newName = (message.replaceAll("\\s{2,}", " ").split(" "))[1];
            for (ClientHandler client : server.getClients()) {
                if (client.getUsername().equals(newName)) {
                    sendMessage("UserName already taken");
                    return;
                }
            }
            setUsername(newName);
        }

        if (message.equals("/name")) {
            sendMessage(getUsername());
        }

        if (message.equals("/help")) {
            sendMessage("\n\"/w `name` `message`\" - send message to another client" +
                    "\n\"/changename `name`\" - change name" +
                    "\n\"/name\" - check name" +
                    "\n\"/exit\" - exit client");
        }
    }
}
