package ru.kolts.java.chat.server;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;

public class Server {
    private int port;
    private List<ClientHandler> clients;
    private AuthenticatedProvider authenticatedProvider;

    public Server(int port) {
        this.port = port;
        clients = new ArrayList<>();
        authenticatedProvider = new InMemoryAuthenticationProvider(this);
        authenticatedProvider.initialize();
    }

    public AuthenticatedProvider getAuthenticatedProvider() {
        return authenticatedProvider;
    }

    public void start() {
        try (ServerSocket serverSocket = new ServerSocket(port)) {
            System.out.println("Сервер запущен на порту: " + port);
            while (true) {
                Socket socket = serverSocket.accept();
                subscribe(new ClientHandler(this, socket));
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public synchronized void subscribe(ClientHandler clientHandler) {
        clients.add(clientHandler);
    }

    public synchronized void unsubscribe(ClientHandler clientHandler) {
        clients.remove(clientHandler);
    }

    public boolean isUsernameBusy(String username) {
        for (ClientHandler client : clients) {
            if (client.getUsername() != null && client.getUsername().equals(username)) {
                return true;
            }
        }
        return false;
    }

    public synchronized void broadcastMessage(String message) {
        for (ClientHandler client : clients) {
            if (client.getUsername() != null) {
                client.sendMessage(message);
            }
        }
    }

    public synchronized void msgClientToClient(String name, String message) {
        for (ClientHandler client : clients) {
            if (client.getUsername().equals(name)) {
                client.sendMessage(message);
                break;
            }
        }
    }

    public boolean kickUser(String username){
        for (ClientHandler client : clients) {
            if (client.getUsername() != null && client.getUsername().equals(username)){
                client.sendMessage("You are removed from the chat");
                client.sendMessage("/exitonkick");
                return true;
            }
        }
        return false;
    }
}
