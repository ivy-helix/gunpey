import java.awt.*;
import java.awt.geom.*;
import javax.swing.*;
import java.util.concurrent.TimeUnit;

class DisplayPanel extends JPanel implements Runnable {

    // ── レイアウト定数 ──────────────────────────────
    private static final int WIDTH  = 500;
    private static final int HEIGHT = 310;

    // ── カラーパレット ──────────────────────────────
    private static final Color C_BG     = new Color(10,  14,  26);
    private static final Color C_CARD   = new Color(16,  22,  40);
    private static final Color C_BORDER = new Color(28,  42,  72);
    private static final Color C_ACCENT = new Color(0,  229, 255);
    private static final Color C_LABEL  = new Color(90, 120, 160);
    private static final Color C_VALUE  = new Color(220, 230, 255);
    private static final Color C_DOT    = new Color(255, 255, 255,   8);
    private static final Color C_WARN   = new Color(255, 165,   0);
    private static final Color C_DANGER = new Color(255,  60,  60);
    private static final Color C_BEST   = new Color(0,  229, 255);

    // ── コンポーネント ──────────────────────────────
    private PuzzlePanel pPanel;
    private JLabel dispScore     = new JLabel("0");
    private JLabel dispHighScore = new JLabel("0");
    private JLabel dispTimer     = new JLabel("01:30");

    // ── タイマー ────────────────────────────────────
    private Thread   thread;
    private TimeFlag tGame;
    private static final int TIME_LIMIT = 1000 * 90;  // 90秒
    private boolean isOnce = false;

    // ═════════════════════════════════════════════
    //  コンストラクタ
    // ═════════════════════════════════════════════
    public DisplayPanel() {
        setPreferredSize(new Dimension(WIDTH, HEIGHT));
        setLayout(null);

        // ── タイマー表示 ─────────────────────────────
        dispTimer.setHorizontalAlignment(JLabel.CENTER);
        dispTimer.setForeground(C_VALUE);
        dispTimer.setFont(new Font("SansSerif", Font.BOLD, 28));
        dispTimer.setOpaque(false);
        dispTimer.setBounds(5, 27, 139, 42);

        // ── スコア表示 ───────────────────────────────
        dispScore.setHorizontalAlignment(JLabel.RIGHT);
        dispScore.setForeground(C_VALUE);
        dispScore.setFont(new Font("SansSerif", Font.BOLD, 26));
        dispScore.setOpaque(false);
        dispScore.setBounds(358, 27, 133, 42);

        // ── ハイスコア表示 ───────────────────────────
        dispHighScore.setHorizontalAlignment(JLabel.RIGHT);
        dispHighScore.setForeground(C_BEST);
        dispHighScore.setFont(new Font("SansSerif", Font.BOLD, 24));
        dispHighScore.setOpaque(false);
        dispHighScore.setBounds(358, 107, 133, 42);
        dispHighScore.setText(String.valueOf(safeGetHighScore()));

        add(dispTimer);
        add(dispScore);
        add(dispHighScore);

        // ── パズルパネル ─────────────────────────────
        pPanel = new PuzzlePanel();
        pPanel.setBounds(149, 4, 202, 302);
        add(pPanel);

        tGame  = new TimeFlag(false, 0, TIME_LIMIT);
        thread = new Thread(this);
        thread.setDaemon(true);
        thread.start();
    }

    private int safeGetHighScore() {
        try { return RankingData.getHighScore(); }
        catch (Exception e) { return 0; }
    }

    // ═════════════════════════════════════════════
    //  ゲーム開始
    // ═════════════════════════════════════════════
    public void gameStart() {
        tGame.setFlag(true);
        isOnce = true;
        pPanel.start();
        pPanel.requestFocusInWindow();
    }

    // ═════════════════════════════════════════════
    //  描画（背景・カード）
    // ═════════════════════════════════════════════
    @Override
    public void paintComponent(Graphics g) {
        Graphics2D g2 = (Graphics2D) g;
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setRenderingHint(RenderingHints.KEY_RENDERING,    RenderingHints.VALUE_RENDER_QUALITY);

        // 背景
        g2.setColor(C_BG);
        g2.fillRect(0, 0, WIDTH, HEIGHT);

        // 等間隔ドットグリッド（装飾）
        g2.setColor(C_DOT);
        for (int y = 10; y < HEIGHT; y += 20)
            for (int x = 10; x < WIDTH; x += 20)
                g2.fillOval(x - 1, y - 1, 2, 2);

        // 左カード：タイマー
        drawCard(g2,  5,  5, 139, 72, "TIME");
        // 左カード：操作説明
        drawCard(g2,  5, 84, 139, 221, "HOW TO PLAY");
        // 右カード：スコア
        drawCard(g2, 356,  5, 139, 72, "SCORE");
        // 右カード：ハイスコア
        drawCard(g2, 356, 84, 139, 72, "BEST");

        // 操作説明テキスト
        g2.setFont(new Font("SansSerif", Font.PLAIN, 12));
        String[][] instructions = {
            {"←→↑↓",  "移動"},
            {"SPACE",  "入れ替え"},
            {"B",      "新しい行"},
        };
        int iy = 112;
        for (String[] pair : instructions) {
            g2.setColor(C_ACCENT);
            g2.drawString(pair[0], 14, iy);
            g2.setColor(C_LABEL);
            g2.drawString(pair[1], 70, iy);
            iy += 18;
        }

        // 区切り線
        g2.setColor(C_BORDER);
        g2.drawLine(14, iy + 2, 135, iy + 2);
        iy += 12;

        g2.setFont(new Font("SansSerif", Font.PLAIN, 11));
        g2.setColor(C_LABEL);
        g2.drawString("ゲーム開始:", 14, iy);
        iy += 16;
        g2.setColor(C_VALUE);
        g2.drawString("Alt+G → Enter", 14, iy);
    }

    /** モダンなカードを描く */
    private void drawCard(Graphics2D g2, int x, int y, int w, int h, String label) {
        // 背景
        g2.setColor(C_CARD);
        g2.fill(new RoundRectangle2D.Float(x, y, w, h, 8, 8));
        // 外枠
        g2.setColor(C_BORDER);
        g2.setStroke(new BasicStroke(1f));
        g2.draw(new RoundRectangle2D.Float(x, y, w, h, 8, 8));
        // ラベル
        g2.setFont(new Font("SansSerif", Font.PLAIN, 10));
        g2.setColor(C_LABEL);
        g2.drawString(label, x + 8, y + 14);
        // アクセントライン
        g2.setColor(C_ACCENT);
        g2.setStroke(new BasicStroke(1.5f));
        g2.drawLine(x + 7, y + 18, x + w - 7, y + 18);
    }

    // ═════════════════════════════════════════════
    //  ゲームループ（スレッド）
    // ═════════════════════════════════════════════
    @Override
    public void run() {
        while (true) {
            try { Thread.sleep(5); }
            catch (InterruptedException e) { Thread.currentThread().interrupt(); return; }

            if (tGame.getFlag()) {
                tGame.tickTime();
                pPanel.tick();

                long remaining = (long)(TIME_LIMIT - tGame.getTime());
                long sec = TimeUnit.MILLISECONDS.toSeconds(remaining);
                long ms  = remaining - TimeUnit.SECONDS.toMillis(sec);

                final String timerText  = String.format("%02d:%02d", sec, ms / 10);
                final Color  timerColor = (sec >= 30) ? C_VALUE : (sec >= 10) ? C_WARN : C_DANGER;
                final String scoreText  = String.valueOf(pPanel.score);

                SwingUtilities.invokeLater(() -> {
                    dispTimer.setText(timerText);
                    dispTimer.setForeground(timerColor);
                    dispScore.setText(scoreText);
                });

                if (!pPanel.isPlaying) tGame.setFlag(false);

            } else if (isOnce) {
                pPanel.end();
                final String hs = String.valueOf(safeGetHighScore());
                SwingUtilities.invokeLater(() -> dispHighScore.setText(hs));
                isOnce = false;
            }

            final String scoreText = String.valueOf(pPanel.score);
            SwingUtilities.invokeLater(() -> dispScore.setText(scoreText));
        }
    }
}
