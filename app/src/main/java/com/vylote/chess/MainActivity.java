package com.vylote.chess;

import android.app.AlertDialog;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
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
    private Button btnLobbyAction;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getSupportActionBar() != null) getSupportActionBar().hide();
        applyImmersiveMode();
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

    // ================= MENU CHÍNH =================
    public void showMenu() {
        setContentView(R.layout.activity_menu);
        controller = new GameController(this, null, this);
        findViewById(R.id.btnNewGame).setOnClickListener(v -> { launchChessBoard(); controller.startNewGame(); });
        findViewById(R.id.btnMultiplayer).setOnClickListener(v -> showMultiplayerChoice());
        findViewById(R.id.btnExit).setOnClickListener(v -> finish());
    }

    public void showMultiplayerChoice() {
        String[] options = {"Host Game (Tạo phòng)", "Join Game (Tìm phòng)", "Hủy"};
        new AlertDialog.Builder(this).setTitle("CHẾ ĐỘ CHƠI MẠNG").setItems(options, (dialog, which) -> {
            if (which == 0) showHostSetupDialog();
            else if (which == 1) showJoinLobby();
        }).show();
    }

    private void showHostSetupDialog() {
        View v = getLayoutInflater().inflate(R.layout.dialog_host_setup, null);
        final EditText edtName = v.findViewById(R.id.edtHostName);
        final RadioGroup rg = v.findViewById(R.id.rgColor);
        new AlertDialog.Builder(this).setTitle("CẤU HÌNH PHÒNG").setView(v).setPositiveButton("TẠO PHÒNG", (dialog, which) -> {
            String name = edtName.getText().toString().trim();
            if(name.isEmpty()) name = "Android Player";
            int color = (rg.getCheckedRadioButtonId() == R.id.rbWhite) ? 0 : 1;
            controller.setMyProfile(name, color);
            controller.setupMultiplayer(true, color, null);
            discoveryService.startBroadcasting(name, color);
            showLobbyUI(name, color, null);
        }).setNegativeButton("Hủy", null).show();
    }



    private void showManualIPDialog() {
        final EditText input = new EditText(this);
        input.setHint("192.168.1.5");
        new AlertDialog.Builder(this).setTitle("NHẬP IP PC").setView(input).setPositiveButton("KẾT NỐI", (dialog, which) -> {
            String ip = input.getText().toString().trim();
            if (!ip.isEmpty()) connectToHost(ip, 0);
        }).show();
    }

    private void connectToHost(String ip, int hostColor) {
        discoveryService.stop();
        int myColor = (hostColor == 0) ? 1 : 0;
        controller.setMyProfile("Android Joiner", myColor);
        controller.setupMultiplayer(false, myColor, ip);
        showLobbyUI("Android Joiner", myColor, null);
    }

    private void showLobbyUI(String myName, int myColor, PlayerProfile opp) {
        setContentView(R.layout.activity_lobby);
        ((TextView)findViewById(R.id.txtMyName)).setText(myName);
        findViewById(R.id.viewMyColor).setBackgroundResource(myColor == 0 ? R.drawable.circle_white : R.drawable.circle_black);

        btnLobbyAction = findViewById(R.id.btnLobbyAction);
        btnLobbyAction.setText("CANCEL");
        btnLobbyAction.setBackgroundColor(Color.parseColor("#C83232")); // Màu đỏ
        btnLobbyAction.setOnClickListener(v -> controller.exitToMenu());

        if (opp != null) {
            updateOpponentInLobby(opp);
        }
    }

    private void updateOpponentInLobby(PlayerProfile opp) {
        TextView txtOppName = findViewById(R.id.txtOppName);
        View viewOppColor = findViewById(R.id.viewOppColor);
        if (txtOppName != null) {
            txtOppName.setText(opp.name);
            txtOppName.setTextColor(Color.WHITE);
        }
        if (viewOppColor != null) {
            viewOppColor.setBackgroundResource(opp.color == 0 ? R.drawable.circle_white : R.drawable.circle_black);
        }
    }

    public void launchChessBoard() {
        setContentView(R.layout.activity_main);
        ChessView chessView = findViewById(R.id.chessView);
        txtTimer = findViewById(R.id.txtTimer);
        txtStatus = findViewById(R.id.txtStatus);
        layoutYou = findViewById(R.id.layoutYou);
        layoutOpponent = findViewById(R.id.layoutOpponent);

        // HIỂN THỊ ĐÚNG MÀU QUÂN VÀ TÊN TRONG TRẬN
        TextView txtMyNameInGame = findViewById(R.id.txtMyNameInGame);
        TextView txtOppNameInGame = findViewById(R.id.txtOppNameInGame);
        View viewMyColorInGame = findViewById(R.id.viewMyColorInGame);
        View viewOppColorInGame = findViewById(R.id.viewOppColorInGame);

        if (txtMyNameInGame != null) txtMyNameInGame.setText(controller.getMyName());
        if (viewMyColorInGame != null) {
            viewMyColorInGame.setBackgroundResource(controller.playerColor == 0
                    ? R.drawable.circle_white : R.drawable.circle_black);
        }

        PlayerProfile opp = controller.getOpponentProfile();
        if (opp != null) {
            if (txtOppNameInGame != null) txtOppNameInGame.setText(opp.name);
            if (viewOppColorInGame != null) {
                viewOppColorInGame.setBackgroundResource(opp.color == 0
                        ? R.drawable.circle_white : R.drawable.circle_black);
            }
        }

        controller.setChessView(chessView);
        chessView.setController(controller);
        findViewById(R.id.btnPause).setOnClickListener(v -> showPauseMenu());
    }

    private void showPauseMenu() {
        controller.isTimeRunning = false;
        String[] options = {"Tiếp tục", "Thoát ra Menu"};
        new AlertDialog.Builder(this).setItems(options, (dialog, which) -> {
            if (which == 0) controller.isTimeRunning = true;
            else controller.exitToMenu();
        }).setCancelable(false).show();
    }

    @Override
    public void onTimerUpdate(int seconds) { runOnUiThread(() -> { if(txtTimer != null) txtTimer.setText(String.format("%02d", seconds)); }); }

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
    // =========================================================
// SỬA LOGIC JOIN: CHO PHÉP NHẬP TÊN
// =========================================================
    private void showJoinLobby() {
        discoveredHosts.clear();
        final ArrayList<String> hostNames = new ArrayList<>();
        final ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, hostNames);

        AlertDialog lobbyDialog = new AlertDialog.Builder(this).setTitle("TÌM PHÒNG...")
                .setAdapter(adapter, (dialog, which) -> {
                    PlayerProfile selected = discoveredHosts.get(which);
                    // SAU KHI CHỌN PHÒNG -> HIỆN DIALOG NHẬP TÊN
                    showJoinerProfileDialog(selected);
                }).setNegativeButton("HỦY", (d, w) -> discoveryService.stop()).create();

        discoveryService.startListening(host -> {
            String entry = host.name + " (" + host.ip + ")";
            if (!hostNames.contains(entry)) {
                discoveredHosts.add(host);
                runOnUiThread(() -> { hostNames.add(entry); adapter.notifyDataSetChanged(); });
            }
        });
        lobbyDialog.show();
    }

    private void showJoinerProfileDialog(PlayerProfile host) {
        EditText input = new EditText(this);
        input.setHint("Nhập tên của bạn");
        new AlertDialog.Builder(this).setTitle("THÔNG TIN NGƯỜI CHƠI").setView(input)
                .setPositiveButton("VÀO PHÒNG", (d, w) -> {
                    String name = input.getText().toString().trim();
                    if(name.isEmpty()) name = "Android Joiner";
                    connectToHost(host.ip, host.color, name); // Gửi kèm tên mới
                }).show();
    }

    private void connectToHost(String ip, int hostColor, String myName) {
        discoveryService.stop();
        int myColor = (hostColor == 0) ? 1 : 0;
        controller.setMyProfile(myName, myColor);
        controller.setupMultiplayer(false, myColor, ip);
        showLobbyUI(myName, myColor, null);
    }

    // Trong MainActivity.java
    public void startCountdownSync() {
        runOnUiThread(() -> {
            // 1. Cập nhật UI ngay lập tức: Đổi màu xanh và chữ STARTING
            btnLobbyAction.setEnabled(false);
            btnLobbyAction.setText("STARTING...");
            // Dùng màu xanh lá (#2ECC71)
            btnLobbyAction.setBackgroundTintList(android.content.res.ColorStateList.valueOf(Color.parseColor("#2ECC71")));

            // 2. Tạo một khoảng trễ cực ngắn (200ms) để mắt người kịp thấy hiệu ứng đổi nút
            new Handler(Looper.getMainLooper()).postDelayed(() -> {
                if (!isFinishing()) {
                    // Nạp bàn cờ và bắt đầu game
                    launchChessBoard();
                    controller.startNewGame();
                }
            }, 200); // 200ms là con số hoàn hảo trên Android
        });
    }

    @Override
    public void onGameStarted() {
        runOnUiThread(() -> {
            PlayerProfile opp = controller.getOpponentProfile();
            if (opp != null) updateOpponentInLobby(opp);

            btnLobbyAction = findViewById(R.id.btnLobbyAction);

            if (controller.isServer) {
                // NẾU ANDROID LÀ HOST
                btnLobbyAction.setText("START GAME");
                btnLobbyAction.setBackgroundTintList(android.content.res.ColorStateList.valueOf(Color.parseColor("#2ECC71")));

                btnLobbyAction.setOnClickListener(v -> {
                    // THỐNG NHẤT: Gửi mã -2 để PC hoặc Android khác đều hiểu là START
                    controller.netManager.sendMove(new model.MovePacket(-2, -2, -2, -2, -1));
                    // 2. Chạy hiệu ứng trên chính máy Android Host này
                    startCountdownSync();
                });
            } else {
                // NẾU ANDROID LÀ JOINER
                btnLobbyAction.setText("LEAVE ROOM");
                btnLobbyAction.setBackgroundTintList(android.content.res.ColorStateList.valueOf(Color.parseColor("#C83232")));
                btnLobbyAction.setOnClickListener(v -> controller.exitToMenu());

                // Thông báo trạng thái
                Toast.makeText(this, "Đã kết nối! Đợi Host bắt đầu...", Toast.LENGTH_SHORT).show();
            }
        });
    }
}