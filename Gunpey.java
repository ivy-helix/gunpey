import java.awt.*;
import javax.swing.*;
import java.awt.event.*;
import java.io.File;

class Gunpey extends JFrame {

    private GameMenu    gMenu;
    private DisplayPanel dPanel;

    Gunpey() {
        setTitle("GUNPEY");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setResizable(false);

        // アイコン読み込み（存在しなくてもクラッシュしない）
        try {
            ImageIcon icon = new ImageIcon("imgs" + File.separator + "icon.png");
            if (icon.getImageLoadStatus() == MediaTracker.COMPLETE)
                setIconImage(icon.getImage());
        } catch (Exception ignored) {}

        // メニュー
        gMenu = new GameMenu();
        gMenu.iStart.addActionListener(e -> dPanel.gameStart());
        gMenu.iExit.addActionListener(e -> System.exit(0));
        gMenu.iHelp.addActionListener(e -> showHelp());
        setJMenuBar(gMenu);

        // メインパネル
        dPanel = new DisplayPanel();
        getContentPane().setLayout(new BorderLayout());
        getContentPane().add(dPanel, BorderLayout.CENTER);

        pack();
        setLocationRelativeTo(null);
        setVisible(true);
    }

    private void showHelp() {
        JDialog dialog = new JDialog(this, "ヘルプ", false);
        dialog.setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);
        JLabel text = new JLabel(
            "<html><body style='padding:10px'>" +
            "このソフトについては<br>" +
            "<a href='https://github.com/kimushun1101/gunpey'>" +
            "https://github.com/kimushun1101/gunpey</a><br>" +
            "をご参照ください。" +
            "</body></html>"
        );
        text.setBorder(BorderFactory.createEmptyBorder(10, 16, 10, 16));
        dialog.getContentPane().add(text);
        dialog.pack();
        dialog.setLocationRelativeTo(this);
        dialog.setVisible(true);
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(Gunpey::new);
    }
}
