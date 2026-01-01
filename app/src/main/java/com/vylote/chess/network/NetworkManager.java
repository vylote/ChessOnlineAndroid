package com.vylote.chess.network;

import android.os.Handler;
import android.os.Looper;
import android.widget.Toast;

import com.vylote.chess.controller.GameController;
import model.GameConfigPacket;
import model.MovePacket;
import java.io.*;
import java.net.*;

public class NetworkManager {
    private GameController controller;
    private ObjectOutputStream out;
    private ObjectInputStream in;
    private Socket socket;
    private ServerSocket serverSocket;
    private final Handler uiHandler = new Handler(Looper.getMainLooper());

    public NetworkManager(GameController controller) { this.controller = controller; }

    public void hostGame(int port) {
        new Thread(() -> {
            try {
                serverSocket = new ServerSocket(port);
                socket = serverSocket.accept(); // Đợi đối thủ
                setupStreams();
                // Báo cho đối thủ biết mình là ai
                sendConfig(new GameConfigPacket(controller.playerColor, controller.getMyName()));
                listenForData();
            } catch (IOException e) { e.printStackTrace(); }
        }).start();
    }

    public void joinGame(String ip, int port) {
        new Thread(() -> {
            try {
                socket = new Socket(ip, port);
                setupStreams();
                listenForData();
            } catch (IOException e) { e.printStackTrace(); }
        }).start();
    }

    private void setupStreams() throws IOException {
        out = new ObjectOutputStream(socket.getOutputStream());
        out.flush();
        in = new ObjectInputStream(socket.getInputStream());
        uiHandler.post(() -> controller.onOpponentConnected());
    }

    public void sendMove(MovePacket packet) {
        new Thread(() -> {
            try {
                if (out != null) { out.writeObject(packet); out.flush(); out.reset(); }
            } catch (IOException e) { e.printStackTrace(); }
        }).start();
    }

    public void sendConfig(GameConfigPacket packet) {
        new Thread(() -> {
            try {
                if (out != null) { out.writeObject(packet); out.flush(); out.reset(); }
            } catch (IOException e) { e.printStackTrace(); }
        }).start();
    }

    private void listenForData() {
        try {
            while (socket != null && !socket.isClosed()) {
                Object data = in.readObject();
                uiHandler.post(() -> {
                    if (data instanceof MovePacket) controller.receiveNetworkMove((MovePacket) data);
                    else if (data instanceof GameConfigPacket) controller.onConfigReceived((GameConfigPacket) data);
                });
            }
        } catch (Exception e) {
            // KHI ĐỐI THỦ THOÁT: Luồng đọc sẽ ném ngoại lệ
            uiHandler.post(() -> {
                Toast.makeText(controller.getContext(), "Đối thủ đã ngắt kết nối!", Toast.LENGTH_SHORT).show();
                controller.exitToMenu(); // Tự động thoát ra Menu chính
            });
        } finally {
            closeConnection();
        }
    }

    public void closeConnection() {
        try {
            if (in != null) in.close(); if (out != null) out.close();
            if (socket != null) socket.close(); if (serverSocket != null) serverSocket.close();
        } catch (IOException e) { e.printStackTrace(); }
    }
}