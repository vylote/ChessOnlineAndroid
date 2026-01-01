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

        applyImmersiveMode(); // Ẩn pin, sóng, wifi
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
    // NHÓM 1: MENU CHÍNH (4 LỰA CHỌN)
    // =========================================================
    public void showMenu() {
        setContentView(R.layout.activity_menu);
        controller = new GameController(this, null, this);

        findViewById(R.id.btnNewGame).setOnClickListener(v -> { launchChessBoard(); controller.startNewGame(); });
        findViewById(R.id.btnMultiplayer).setOnClickListener(v -> showMultiplayerChoice());
        findViewById(R.id.btnSaveLoad).setOnClickListener(v -> Toast.makeText(this, "Save/Load Offline Only", Toast.LENGTH_SHORT).show());
        findViewById(R.id.btnExit).setOnClickListener(v -> finish());
    }

    public void showMultiplayerChoice() {
        String[] options = {"Host Game", "Join Game", "Cancel"};
        new AlertDialog.Builder(this)
                .setTitle("MULTIPLAYER")
                .setItems(options, (dialog, which) -> {
                    if (which == 0) showHostSetupDialog();
                    else if (which == 1) showJoinLobby(); // Tự tìm phòng trong mạng LAN/Radmin
                }).show();
    }

    // =========================================================
    // NHÓM 2: LOGIC HOST & JOIN (LAN / RADMIN)
    // =========================================================
    private void showHostSetupDialog() {
        View v = getLayoutInflater().inflate(R.layout.dialog_host_setup, null);
        final EditText edtName = v.findViewById(R.id.edtHostName);
        final RadioGroup rg = v.findViewById(R.id.rgColor);

        new AlertDialog.Builder(this)
                .setView(v)
                .setPositiveButton("CREATE", (dialog, which) -> {
                    String name = edtName.getText().toString().trim();
                    if(name.isEmpty()) name = "Host Player";
                    int color = (rg.getCheckedRadioButtonId() == R.id.rbWhite) ? 0 : 1;

                    controller.setMyProfile(name, color);
                    controller.setupMultiplayer(true, color, null);
                    discoveryService.startBroadcasting(name, color);

                    showLobbyUI(name, color, null); // Chuyển sang màn hình VS
                }).setNegativeButton("Cancel", null).show();
    }

    private void showJoinLobby() {
        discoveredHosts.clear();
        ArrayList<String> hostNames = new ArrayList<>();
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, hostNames);

        new AlertDialog.Builder(this)
                .setTitle("Searching LAN / Radmin...")
                .setAdapter(adapter, (dialog, which) -> {
                    PlayerProfile selected = discoveredHosts.get(which);
                    discoveryService.stop();
                    controller.setMyProfile("Joiner", selected.color == 0 ? 1 : 0);
                    controller.setupMultiplayer(false, selected.color == 0 ? 1 : 0, selected.ip);
                    showLobbyUI("Joiner", selected.color == 0 ? 1 : 0, selected);
                })
                .setNegativeButton("Hủy", (d, w) -> discoveryService.stop()).show();

        discoveryService.startListening(host -> {
            if (!hostNames.contains(host.name + " (" + host.ip + ")")) {
                discoveredHosts.add(host);
                runOnUiThread(() -> { hostNames.add(host.name + " (" + host.ip + ")"); adapter.notifyDataSetChanged(); });
            }
        });
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
    // NHÓM 3: ĐỒNG BỘ GAMEPLAY
    // =========================================================
    @Override
    public void onGameStarted() {
        runOnUiThread(() -> {
            if (discoveryService != null) discoveryService.stop();
            launchChessBoard(); // Tự động vào bàn cờ
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
        String[] options = {"Continue", "Exit to Menu"};
        new AlertDialog.Builder(this).setItems(options, (dialog, which) -> {
            if (which == 0) controller.isTimeRunning = true;
            else controller.exitToMenu();
        }).setCancelable(false).show();
    }

    @Override public void onTimerUpdate(int seconds) { runOnUiThread(() -> { if(txtTimer != null) txtTimer.setText(String.format("%02d", seconds)); }); }
    @Override public void onTurnUpdate(String text, int color, boolean isMyTurn) {
        runOnUiThread(() -> {
            if(txtStatus != null) { txtStatus.setText(text); txtStatus.setTextColor(color); }
            if(layoutYou != null) {
                layoutYou.setBackgroundResource(isMyTurn ? R.drawable.bg_player_box_active : R.drawable.bg_player_box);
                layoutOpponent.setBackgroundResource(isMyTurn ? R.drawable.bg_player_box : R.drawable.bg_player_box_active);
            }
        });
    }
}