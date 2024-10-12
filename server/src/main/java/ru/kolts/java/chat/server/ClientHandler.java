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
    private Role role;

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
        new Thread(() -> {
            try {
                System.out.println("Клиент подключился ");
                //цикл аутентификации
                sendMessage("Перед работой необходимо пройти аутентификацию командой " +
                        "/auth login password или регистрацию командой /reg login password username");
                while (true) {
                    String message = in.readUTF();
                    if (message.startsWith("/")) {
                        if (message.startsWith("/exit")) {
                            sendMessage("/exitok");
                            break;
                        }
                        // /auth login password
                        if (message.startsWith("/auth ")) {
                            String[] elements = message.split(" ");
                            if (elements.length != 3) {
                                sendMessage("Неверный формат команды /auth ");
                                continue;
                            }
                            if (server.getAuthenticatedProvider()
                                    .authenticate(this, elements[1], elements[2])) {
                                role = server.getAuthenticatedProvider().getPermission(elements[1], elements[2]);
                                break;
                            }
                            continue;
                        }
                        // /reg login password username
                        if (message.startsWith("/reg ")) {
                            String[] elements = message.split(" ");
                            if (elements.length != 4) {
                                sendMessage("Неверный формат команды /reg ");
                                continue;
                            }
                            if (server.getAuthenticatedProvider()
                                    .registration(this, elements[1], elements[2], elements[3])) {
                                break;
                            }
                            continue;
                        }
                    }
                    sendMessage("Перед работой необходимо пройти аутентификацию командой " +
                            "/auth login password или регистрацию командой /reg login password username");
                }
                System.out.println("Клиент " + username + " успешно прошел аутентификацию");

                //Цилк работы
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
                        if (message.equals("/name")) {
                            sendMessage(getUsername());
                        }
                        if (message.equals("/help")) {
                            listCommands();
                        }
                        if (message.startsWith("/kick") ) {
                            String[] elements = message.split(" ");
                            if (elements.length != 2) {
                                sendMessage("Fail. Use: /kick [username]");
                                continue;
                            }
                            if (role == Role.ADMIN) {
                                if(!server.kickUser(elements[1])){
                                    sendMessage("User not founds");
                                }
                            }else{
                                sendMessage("Not enough permissions");
                            }
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
                "\n\"/exit\" - exit client");
    }

    public void sendPrivateMessage(String message, String command) {
        String[] parts = message.trim().split(" ", 3);
        if (!parts[0].equals(command) || parts.length != 3) {
            sendMessage("Fail. Use: " + command + " [username] [message]");
            return;
        }
        String name = parts[1].trim();
        String privateMessage = parts[2].trim();
        server.msgClientToClient(name, username + " : " + privateMessage);
    }
}
