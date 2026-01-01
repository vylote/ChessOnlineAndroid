package com.vylote.chess;

import android.app.AlertDialog;
import android.os.Bundle;
import android.view.View;
import android.widget.*;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.core.view.WindowInsetsControllerCompat;

import com.vylote.chess.controller.DiscoveryService;
import com.vylote.chess.controller.GameController;
import model.PlayerProfile;
import com.vylote.chess.ui.ChessView;
import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity implements GameUIListener {
    private GameController controller;
    private DiscoveryService discoveryService;
    private final List<PlayerProfile> discoveredHosts = new ArrayList<>();

    private TextView txtTimer, txtStatus;
    private LinearLayout layoutYou, layoutOpponent;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getSupportActionBar() != null) getSupportActionBar().hide();

        applyImmersiveMode(); // Ẩn thanh trạng thái hệ thống
        discoveryService = new DiscoveryService(this);
        showMenu();
    }

    private void applyImmersiveMode() {
        WindowCompat.setDecorFitsSystemWindows(getWindow(), false);
        WindowInsetsControllerCompat controllerCompat = new WindowInsetsControllerCompat(getWindow(), getWindow().getDecorView());
        controllerCompat.hide(WindowInsetsCompat.Type.systemBars());
        controllerCompat.setSystemBarsBehavior(WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE);
    }

    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);
        if (hasFocus) applyImmersiveMode();
    }

    // =========================================================
    // MENU CHÍNH
    // =========================================================
    public void showMenu() {
        setContentView(R.layout.activity_menu);
        controller = new GameController(this, null, this);

        findViewById(R.id.btnNewGame).setOnClickListener(v -> { launchChessBoard(); controller.startNewGame(); });
        findViewById(R.id.btnMultiplayer).setOnClickListener(v -> showMultiplayerChoice());
        findViewById(R.id.btnSaveLoad).setOnClickListener(v -> Toast.makeText(this, "Tính năng đang phát triển", Toast.LENGTH_SHORT).show());
        findViewById(R.id.btnExit).setOnClickListener(v -> finish());
    }

    public void showMultiplayerChoice() {
        String[] options = {"Host Game (Tạo phòng)", "Join Game (Tìm phòng)", "Hủy"};
        new AlertDialog.Builder(this)
                .setTitle("CHẾ ĐỘ CHƠI MẠNG")
                .setItems(options, (dialog, which) -> {
                    if (which == 0) showHostSetupDialog();
                    else if (which == 1) showJoinLobby();
                }).show();
    }

    // =========================================================
    // LOGIC MULTIPLAYER (LAN / VPN)
    // =========================================================
    private void showHostSetupDialog() {
        View v = getLayoutInflater().inflate(R.layout.dialog_host_setup, null);
        final EditText edtName = v.findViewById(R.id.edtHostName);
        final RadioGroup rg = v.findViewById(R.id.rgColor);

        new AlertDialog.Builder(this)
                .setTitle("CẤU HÌNH PHÒNG")
                .setView(v)
                .setPositiveButton("TẠO PHÒNG", (dialog, which) -> {
                    String name = edtName.getText().toString().trim();
                    if(name.isEmpty()) name = "Android Player";
                    int color = (rg.getCheckedRadioButtonId() == R.id.rbWhite) ? 0 : 1;

                    controller.setMyProfile(name, color);
                    controller.setupMultiplayer(true, color, null); // Mở Server cổng 5555
                    discoveryService.startBroadcasting(name, color); // Phát tín hiệu cổng 8888

                    showLobbyUI(name, color, null);
                }).setNegativeButton("Hủy", null).show();
    }

    private void showJoinLobby() {
        discoveredHosts.clear();
        final ArrayList<String> hostNames = new ArrayList<>();
        final ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, hostNames);

        // Hiển thị một Dialog duy nhất chứa cả danh sách tự động và nút thủ công
        AlertDialog lobbyDialog = new AlertDialog.Builder(this)
                .setTitle("ĐANG TÌM PHÒNG TRONG MẠNG...")
                .setAdapter(adapter, (dialog, which) -> {
                    PlayerProfile selected = discoveredHosts.get(which);
                    connectToHost(selected.ip, selected.color);
                })
                .setNeutralButton("NHẬP IP THỦ CÔNG", (d, w) -> {
                    discoveryService.stop();
                    showManualIPDialog(); // Giải pháp cho VPN/Radmin
                })
                .setNegativeButton("HỦY", (d, w) -> discoveryService.stop())
                .create();

        // Lắng nghe tín hiệu UDP 8888 từ PC
        discoveryService.startListening(host -> {
            String entry = host.name + " (" + host.ip + ")";
            if (!hostNames.contains(entry)) {
                discoveredHosts.add(host);
                runOnUiThread(() -> {
                    hostNames.add(entry);
                    adapter.notifyDataSetChanged();
                });
            }
        });

        lobbyDialog.show();
    }

    private void showManualIPDialog() {
        final EditText input = new EditText(this);
        input.setHint("Ví dụ: 192.168.1.5 hoặc 10.147.x.x");
        input.setPadding(50, 40, 50, 40);

        new AlertDialog.Builder(this)
                .setTitle("NHẬP IP MÁY CHỦ (PC)")
                .setMessage("Nhập địa chỉ IP của PC để kết nối trực tiếp")
                .setView(input)
                .setPositiveButton("KẾT NỐI", (dialog, which) -> {
                    String ip = input.getText().toString().trim();
                    if (!ip.isEmpty()) connectToHost(ip, 0); // Mặc định coi Host là Trắng
                })
                .setNegativeButton("QUAY LẠI", (d, w) -> showJoinLobby())
                .show();
    }

    private void connectToHost(String ip, int hostColor) {
        discoveryService.stop();
        int myColor = (hostColor == 0) ? 1 : 0;
        controller.setMyProfile("Android Joiner", myColor);
        controller.setupMultiplayer(false, myColor, ip); // Kết nối TCP 5555
        showLobbyUI("Android Joiner", myColor, null);
    }

    private void showLobbyUI(String myName, int myColor, PlayerProfile opp) {
        setContentView(R.layout.activity_lobby);
        ((TextView)findViewById(R.id.txtMyName)).setText(myName);
        findViewById(R.id.viewMyColor).setBackgroundResource(myColor == 0 ? R.drawable.circle_white : R.drawable.circle_black);
        if (opp != null) {
            ((TextView)findViewById(R.id.txtOppName)).setText(opp.name);
            findViewById(R.id.viewOppColor).setBackgroundResource(opp.color == 0 ? R.drawable.circle_white : R.drawable.circle_black);
        }
    }

    // =========================================================
    // GAMEPLAY SYNC
    // =========================================================
    @Override
    public void onGameStarted() {
        runOnUiThread(() -> {
            if (discoveryService != null) discoveryService.stop();
            launchChessBoard();
        });
    }

    private void launchChessBoard() {
        setContentView(R.layout.activity_main);
        ChessView chessView = findViewById(R.id.chessView);
        txtTimer = findViewById(R.id.txtTimer);
        txtStatus = findViewById(R.id.txtStatus);
        layoutYou = findViewById(R.id.layoutYou);
        layoutOpponent = findViewById(R.id.layoutOpponent);

        controller.setChessView(chessView);
        chessView.setController(controller);
        findViewById(R.id.btnPause).setOnClickListener(v -> showPauseMenu());
    }

    private void showPauseMenu() {
        controller.isTimeRunning = false;
        String[] options = {"Tiếp tục", "Thoát ra Menu"};
        new AlertDialog.Builder(this).setItems(options, (dialog, which) -> {
            if (which == 0) controller.isTimeRunning = true;
            else controller.exitToMenu(); // Đồng bộ thoát cả 2 máy
        }).setCancelable(false).show();
    }

    @Override
    public void onTimerUpdate(int seconds) {
        runOnUiThread(() -> { if(txtTimer != null) txtTimer.setText(String.format("%02d", seconds)); });
    }

    @Override
    public void onTurnUpdate(String text, int color, boolean isMyTurn) {
        runOnUiThread(() -> {
            if(txtStatus != null) { txtStatus.setText(text); txtStatus.setTextColor(color); }
            if(layoutYou != null && layoutOpponent != null) {
                layoutYou.setBackgroundResource(isMyTurn ? R.drawable.bg_player_box_active : R.drawable.bg_player_box);
                layoutOpponent.setBackgroundResource(isMyTurn ? R.drawable.bg_player_box : R.drawable.bg_player_box_active);
            }
        });
    }
}