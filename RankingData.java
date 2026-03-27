import java.io.*;
import java.util.*;

class RankingData {

    public static int getHighScore() {
        ArrayList<Integer> al = loadScores();
        if (al.isEmpty()) return 0;
        return al.get(0);
    }

    public static int setScore(int score) {
        ArrayList<Integer> al = loadScores();
        if (score != 0) al.add(score);
        Collections.sort(al);
        Collections.reverse(al);
        saveScores(al);
        if (al.isEmpty()) return 0;
        return al.get(0);
    }

    private static ArrayList<Integer> loadScores() {
        ArrayList<Integer> al = new ArrayList<>();
        File file = new File("ranking" + File.separator + "score.txt");
        if (!file.exists() || !file.isFile() || !file.canRead()) return al;
        try (BufferedReader br = new BufferedReader(new FileReader(file))) {
            String str;
            while ((str = br.readLine()) != null) {
                try { al.add(Integer.valueOf(str.trim())); }
                catch (NumberFormatException ignored) {}
            }
        } catch (IOException e) {
            System.out.println("Score load error: " + e.getMessage());
        }
        Collections.sort(al);
        Collections.reverse(al);
        return al;
    }

    private static void saveScores(ArrayList<Integer> al) {
        File file = new File("ranking" + File.separator + "score.txt");
        if (!file.exists() || !file.isFile() || !file.canWrite()) {
            System.out.println("Cannot write score file.");
            return;
        }
        try (PrintWriter pw = new PrintWriter(new BufferedWriter(new FileWriter(file)))) {
            for (int s : al) pw.println(s);
        } catch (IOException e) {
            System.out.println("Score save error: " + e.getMessage());
        }
    }
}
