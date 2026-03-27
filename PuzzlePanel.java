import java.awt.*;
import java.awt.event.*;
import java.awt.geom.*;
import javax.swing.*;
import javax.sound.sampled.*;
import java.util.Random;
import java.io.File;
import java.net.URL;

class PuzzlePanel extends JPanel implements KeyListener {

    // ── グリッドサイズ ──────────────────────────────
    private static final int GSW = 40;
    private static final int GSH = 30;
    private static final int COL = 5;
    private static final int ROW = 10;

    // ── カラーパレット（ダーク×ネオン） ──────────────
    private static final Color C_BG     = new Color(10,  14,  26);
    private static final Color C_CELL   = new Color(13,  18,  34);
    private static final Color C_GRID   = new Color(24,  36,  60);
    private static final Color C_LINE   = new Color(0,  229, 255);   // ネオンシアン
    private static final Color C_VANISH = new Color(255, 120,  40);  // オレンジ
    private static final Color C_CURSOR = new Color(255, 220,   0);  // ゴールド
    private static final Color C_SCORE  = new Color(255, 230,  80);  // スコアポップアップ

    // ── 盤面配列 ─────────────────────────────────
    private int[][] normalPanel   = new int[COL][ROW];
    private int[][] vanishPanel   = new int[COL][ROW];
    private int[][] floatingPanel = new int[COL][ROW];
    private int[][] panelFlag     = new int[COL][ROW];
    private int[][] gridFlag      = new int[COL + 1][ROW + 1];
    private int connectFlag;

    // ── パネル種類定数 ────────────────────────────
    private static final int bs = 1;  // バックスラッシュ (\)
    private static final int sl = 2;  // スラッシュ       (/)
    private static final int vi = 3;  // キャレット        (∧)
    private static final int ha = 4;  // V字              (∨)

    // ── スコア ────────────────────────────────────
    public  int score        = 0;
    private int scoreTemp    = 0;
    private int scoreTempPre = 0;

    // ── タイマー ──────────────────────────────────
    private TimeFlag swapTimer;
    private TimeFlag vanishTimer;
    private TimeFlag panelupTimer;
    public  boolean  isPlaying = false;

    // ── カーソル・スワップ位置 ────────────────────
    private int curX = 2, curY = 4;
    private int s1X  = 0, s1Y  = 0;
    private int s2X  = 0, s2Y  = 0;

    // ── オーディオ（javax.sound.sampled） ─────────
    private Clip acSwap;
    private Clip acConnect;
    private Clip acVanish;
    private Clip acPanelUp;

    // ═════════════════════════════════════════════
    //  コンストラクタ
    // ═════════════════════════════════════════════
    public PuzzlePanel() {
        setPreferredSize(new Dimension(COL * GSW + 2, ROW * GSH + 2));
        setFocusable(true);
        addKeyListener(this);

        swapTimer    = new TimeFlag(false, 0, 80);
        vanishTimer  = new TimeFlag(false, 0, 6000);
        panelupTimer = new TimeFlag(false, 0, 5000);

        acSwap    = loadClip("sounds/swap.wav");
        acConnect = loadClip("sounds/connect.wav");
        acVanish  = loadClip("sounds/vanish.wav");
        acPanelUp = loadClip("sounds/panelup.wav");
    }

    /** サウンドファイルを読み込む。存在しない場合は null を返す */
    private Clip loadClip(String resourcePath) {
        try {
            URL url = getClass().getResource(resourcePath);
            if (url == null) return null;
            AudioInputStream ais = AudioSystem.getAudioInputStream(url);
            Clip clip = AudioSystem.getClip();
            clip.open(ais);
            return clip;
        } catch (Exception e) {
            return null;
        }
    }

    /** サウンドを再生する。null でも例外でも安全に無視する */
    private void playClip(Clip clip) {
        if (clip == null) return;
        try {
            clip.setFramePosition(0);
            clip.start();
        } catch (Exception e) { /* ignore */ }
    }

    // ═════════════════════════════════════════════
    //  ゲーム制御
    // ═════════════════════════════════════════════
    public void start() {
        for (int y = 0; y < ROW; y++)
            for (int x = 0; x < COL; x++)
                normalPanel[x][y] = 0;
        curX = 2; curY = 4;
        score = 0;
        panelupTimer.setFlag(true);
        isPlaying = true;
    }

    public void end() {
        score += scoreTemp;
        scoreTemp = 0;
        RankingData.setScore(score);
        swapTimer.setFlag(false);
        vanishTimer.setFlag(false);
        isPlaying = false;
        repaint();
    }

    // ═════════════════════════════════════════════
    //  描画
    // ═════════════════════════════════════════════
    @Override
    public void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2 = (Graphics2D) g;
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,   RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setRenderingHint(RenderingHints.KEY_RENDERING,      RenderingHints.VALUE_RENDER_QUALITY);
        g2.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_PURE);

        // ── 背景 ──────────────────────────────────
        g2.setColor(C_BG);
        g2.fillRect(0, 0, getWidth(), getHeight());

        // ── グリッドセル ──────────────────────────
        for (int y = 0; y < ROW; y++) {
            for (int x = 0; x < COL; x++) {
                g2.setColor(C_CELL);
                g2.fillRect(x * GSW + 1, y * GSH + 1, GSW - 1, GSH - 1);
                g2.setColor(C_GRID);
                g2.setStroke(new BasicStroke(1f));
                g2.drawRect(x * GSW, y * GSH, GSW, GSH);
            }
        }

        // ── パネル描画用の共通設定 ─────────────────
        Composite cSave = g2.getComposite();
        Composite cGlow = AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.28f);
        Stroke    sGlow = new BasicStroke(8f,   BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND);
        Stroke    sLine = new BasicStroke(2.5f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND);

        for (int x = 0; x < COL; x++) {
            for (int y = 0; y < ROW; y++) {
                int px = x * GSW, py = y * GSH;

                if (normalPanel[x][y] != 0)
                    drawGlowPanel(g2, normalPanel[x][y],   px,     py,     C_LINE,   cSave, cGlow, sGlow, sLine);
                if (vanishPanel[x][y] != 0)
                    drawGlowPanel(g2, vanishPanel[x][y],   px,     py,     C_VANISH, cSave, cGlow, sGlow, sLine);
                if (floatingPanel[x][y] != 0) {
                    drawGlowPanel(g2, floatingPanel[x][y], px - 4, py - 4, C_LINE,   cSave, cGlow, sGlow, sLine);
                    g2.setColor(C_GRID);
                    g2.setStroke(new BasicStroke(1f));
                    g2.drawRect(px - 4, py - 4, GSW, GSH);
                }
            }
        }

        // ── スワップアニメーション ──────────────────
        if (swapTimer.getFlag()) {
            s1X = curX * GSW;  s1Y = curY * GSH;
            s2X = curX * GSW;  s2Y = (curY + 1) * GSH;

            g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.18f));
            g2.setColor(C_CURSOR);
            g2.fillRect(s1X, s1Y, GSW, GSH);
            g2.fillRect(s2X, s2Y, GSW, GSH);
            g2.setComposite(cSave);

            int offset = (int)(GSH * swapTimer.getRate());
            g2.setColor(C_LINE);
            g2.setStroke(sLine);
            drawPanelShape(g2, normalPanel[curX][curY + 1], s1X, s1Y + offset);
            drawPanelShape(g2, normalPanel[curX][curY],     s2X, s2Y - offset);
        }

        // ── カーソル ──────────────────────────────
        if (isPlaying) {
            g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.12f));
            g2.setColor(C_CURSOR);
            g2.fillRect(curX * GSW + 1, curY * GSH + 1, GSW - 1, 2 * GSH - 1);
            g2.setComposite(cSave);
            g2.setColor(C_CURSOR);
            g2.setStroke(new BasicStroke(2f));
            g2.drawRect(curX * GSW, curY * GSH, GSW, 2 * GSH);
        }

        // ── スコアポップアップ ─────────────────────
        if (scoreTemp != 0) {
            int dX = 0, dY = 0;
            for (int y = 0; y < ROW; y++)
                for (int x = 0; x < COL; x++)
                    if (panelFlag[x][y] == 1) { dX = x; dY = y; }
            if (dX > 3) dX = 3;
            if (dY > 5) dY -= 1;

            String txt = "+" + scoreTemp;
            g2.setFont(new Font("SansSerif", Font.BOLD, 16));
            FontMetrics fm = g2.getFontMetrics();
            g2.setColor(new Color(0, 0, 0, 160));
            g2.drawString(txt, dX * GSW + 2, dY * GSH + 1);
            g2.setColor(C_SCORE);
            g2.drawString(txt, dX * GSW + 1, dY * GSH);
        }

        // ── スタンバイオーバーレイ ─────────────────
        if (!isPlaying) {
            g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.55f));
            g2.setColor(C_BG);
            g2.fillRect(0, 0, getWidth(), getHeight());
            g2.setComposite(cSave);
            g2.setFont(new Font("SansSerif", Font.BOLD, 13));
            g2.setColor(C_LINE);
            FontMetrics fm = g2.getFontMetrics();
            String msg = "PRESS START";
            g2.drawString(msg, (getWidth() - fm.stringWidth(msg)) / 2, getHeight() / 2);
        }
    }

    /** グロー効果付きでパネルを描画する */
    private void drawGlowPanel(Graphics2D g2, int p, int x, int y, Color color,
            Composite cSave, Composite cGlow, Stroke sGlow, Stroke sLine) {
        g2.setComposite(cGlow);
        g2.setColor(color);
        g2.setStroke(sGlow);
        drawPanelShape(g2, p, x, y);

        g2.setComposite(cSave);
        g2.setColor(color);
        g2.setStroke(sLine);
        drawPanelShape(g2, p, x, y);
    }

    /** パネルの形状（線）を描画する */
    private void drawPanelShape(Graphics2D g2, int p, int x, int y) {
        int x2 = x + GSW, y2 = y + GSH, xm = x + GSW / 2, ym = y + GSH / 2;
        switch (p) {
            case bs: g2.draw(new Line2D.Double(x,  y,  x2, y2)); break;  // \
            case sl: g2.draw(new Line2D.Double(x,  y2, x2, y )); break;  // /
            case vi:  // ∧
                g2.draw(new Line2D.Double(x,  y,  xm, ym));
                g2.draw(new Line2D.Double(xm, ym, x2, y ));
                break;
            case ha:  // ∨
                g2.draw(new Line2D.Double(x,  y2, xm, ym));
                g2.draw(new Line2D.Double(xm, ym, x2, y2));
                break;
        }
    }

    // ═════════════════════════════════════════════
    //  接続チェック（ロジックは原作から変更なし）
    // ═════════════════════════════════════════════
    public void checkFlags() {
        for (int x = 0; x < COL; x++)
            for (int y = 0; y < ROW; y++)
                if (vanishPanel[x][y] != 0)
                    normalPanel[x][y] = vanishPanel[x][y];

        for (int x = 0; x < COL + 1; x++) {
            for (int y = 0; y < ROW + 1; y++) {
                if (x < COL && y < ROW) panelFlag[x][y] = 0;
                gridFlag[x][y] = (x < COL) ? 0 : 1;
            }
        }

        for (int y = 0; y < ROW + 1; y++) {
            connectFlag = 0;
            connectionCheck(0, y, 0, y);
        }
        for (int y = 0; y < ROW + 1; y++) gridFlag[COL][y] = 0;
        for (int y = 0; y < ROW + 1; y++) {
            connectFlag = 0;
            connectionCheck(COL, y, COL, y);
        }

        scoreTemp = 0;
        for (int x = 0; x < COL; x++) {
            for (int y = 0; y < ROW; y++) {
                if (panelFlag[x][y] == 1) {
                    vanishPanel[x][y] = normalPanel[x][y];
                    normalPanel[x][y] = 0;
                    if (!vanishTimer.getFlag()) vanishTimer.setFlag(true);
                    scoreTemp++;
                }
            }
        }
        scoreTemp = 100 * scoreTemp * (scoreTemp - 4);
        if (scoreTemp > scoreTempPre) playClip(acConnect);
        scoreTempPre = scoreTemp;
    }

    public int connectionCheck(int x, int y, int px, int py) {
        int final_gF = 0;
        switch (gridFlag[x][y]) {
            case 0: gridFlag[x][y] = 2; break;
            case 1: return 1;
            case 2: return connectFlag;
        }
        if (x != COL) {
            if (y != 0) {
                if (normalPanel[x][y-1] == sl & (px != x+1 | py != y-1))
                    final_gF += panelFlag[x][y-1] = connectionCheck(x+1, y-1, x, y);
                if (normalPanel[x][y-1] == ha & (px != x+1 | py != y))
                    final_gF += panelFlag[x][y-1] = connectionCheck(x+1, y,   x, y);
            }
            if (y != ROW) {
                if (normalPanel[x][y] == vi & (px != x+1 | py != y))
                    final_gF += panelFlag[x][y]   = connectionCheck(x+1, y,   x, y);
                if (normalPanel[x][y] == bs & (px != x+1 | py != y+1))
                    final_gF += panelFlag[x][y]   = connectionCheck(x+1, y+1, x, y);
            }
        }
        if (x != 0) {
            if (y != 0) {
                if (normalPanel[x-1][y-1] == bs & (px != x-1 | py != y-1))
                    final_gF += panelFlag[x-1][y-1] = connectionCheck(x-1, y-1, x, y);
                if (normalPanel[x-1][y-1] == ha & (px != x-1 | py != y))
                    final_gF += panelFlag[x-1][y-1] = connectionCheck(x-1, y,   x, y);
            }
            if (y != ROW) {
                if (normalPanel[x-1][y] == vi & (px != x-1 | py != y))
                    final_gF += panelFlag[x-1][y]   = connectionCheck(x-1, y,   x, y);
                if (normalPanel[x-1][y] == sl & (px != x-1 | py != y+1))
                    final_gF += panelFlag[x-1][y]   = connectionCheck(x-1, y+1, x, y);
            }
        }
        if (final_gF == 0) {
            gridFlag[x][y] = 0;
        } else {
            gridFlag[x][y] = 1;
            connectFlag = 1;
        }
        return gridFlag[x][y];
    }

    // ═════════════════════════════════════════════
    //  キー入力
    // ═════════════════════════════════════════════
    @Override
    public void keyPressed(KeyEvent e) {
        if (!swapTimer.getFlag() && isPlaying) {
            switch (e.getKeyCode()) {
                case KeyEvent.VK_LEFT:  if (curX > 0)       curX--; break;
                case KeyEvent.VK_UP:    if (curY > 0)       curY--; break;
                case KeyEvent.VK_RIGHT: if (curX < COL - 1) curX++; break;
                case KeyEvent.VK_DOWN:  if (curY < ROW - 2) curY++; break;
                case KeyEvent.VK_SPACE:
                    swapTimer.setFlag(true);
                    playClip(acSwap);
                    int buf1, buf2;
                    buf1 = (vanishPanel[curX][curY]   == 0) ? normalPanel[curX][curY]   : floatingPanel[curX][curY];
                    buf2 = (vanishPanel[curX][curY+1] == 0) ? normalPanel[curX][curY+1] : floatingPanel[curX][curY+1];
                    if (vanishPanel[curX][curY]   == 0) normalPanel[curX][curY]   = buf2; else floatingPanel[curX][curY]   = buf2;
                    if (vanishPanel[curX][curY+1] == 0) normalPanel[curX][curY+1] = buf1; else floatingPanel[curX][curY+1] = buf1;
                    checkFlags();
                    break;
                case KeyEvent.VK_B:
                    panelUp();
                    break;
            }
        }
    }

    @Override public void keyReleased(KeyEvent e) {}
    @Override public void keyTyped(KeyEvent e)    {}

    // ═════════════════════════════════════════════
    //  パネルアップ
    // ═════════════════════════════════════════════
    public void panelUp() {
        if (!vanishTimer.getFlag()) {
            playClip(acPanelUp);
            for (int x = 0; x < COL; x++)
                if (normalPanel[x][0] != 0) isPlaying = false;

            for (int y = 0; y < ROW - 1; y++)
                for (int x = 0; x < COL; x++)
                    normalPanel[x][y] = normalPanel[x][y + 1];

            Random rnd = new Random();
            int num = rnd.nextInt(100);
            if      (num < 45) num = 2;
            else if (num < 90) num = 3;
            else               num = 4;

            boolean[] inputP = {false, false, false, false, false};
            while (num > 0) {
                int px = rnd.nextInt(5);
                if (!inputP[px]) { inputP[px] = true; num--; }
            }
            for (int x = 0; x < COL; x++)
                normalPanel[x][ROW - 1] = inputP[x] ? rnd.nextInt(4) + 1 : 0;

            if (curY > 0) curY--;
            checkFlags();
        }
        panelupTimer.setFlag(true);
    }

    // ═════════════════════════════════════════════
    //  毎フレーム処理
    // ═════════════════════════════════════════════
    public void tick() {
        swapTimer.tickTime();
        vanishTimer.tickTime();
        panelupTimer.tickTime();

        if (!vanishTimer.getFlag()) {
            for (int x = 0; x < COL; x++) {
                for (int y = 0; y < ROW; y++) {
                    vanishPanel[x][y] = 0;
                    if (floatingPanel[x][y] != 0) {
                        normalPanel[x][y]   = floatingPanel[x][y];
                        floatingPanel[x][y] = 0;
                    }
                }
            }
            if (scoreTemp != 0) {
                score += scoreTemp;
                scoreTemp = 0;
            }
            if (vanishTimer.getTime() > 5) playClip(acVanish);
        }
        if (!panelupTimer.getFlag()) panelUp();
        repaint();
    }
}
