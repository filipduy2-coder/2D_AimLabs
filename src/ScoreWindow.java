import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.io.FileWriter;
import java.util.ArrayList;
import java.util.Scanner;

public class ScoreWindow extends JFrame {

    JTable table;
    ArrayList<String[]> data;

    public ScoreWindow() {
        setTitle("Scores");
        setSize(400, 300);
        setLocationRelativeTo(null);
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);

        loadData();
        buildUI();

        setVisible(true);
    }

    private void loadData() {
        data = new ArrayList<>();

        try (Scanner sc = new Scanner(new File("scores.txt"))) {
            while (sc.hasNextLine()) {
                String line = sc.nextLine().trim();
                if (line.isEmpty()) continue;

                String[] parts = line.split(";");
                if (parts.length < 2) continue;

                data.add(parts);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void buildUI() {
        String[] columns = {"Score", "Date"};
        String[][] tableData = data.toArray(new String[0][]);

        table = new JTable(tableData, columns);
        table.setFont(new Font("Arial", Font.PLAIN, 16));
        table.setRowHeight(25);
        table.setEnabled(false);

        add(new JScrollPane(table), BorderLayout.CENTER);

        JButton clearBtn = new JButton("Clear Scores");
        clearBtn.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                ScoreWindow.this.clearScores();
            }
        });

        JPanel bottom = new JPanel();
        bottom.add(clearBtn);

        add(bottom, BorderLayout.SOUTH);
    }

    private void clearScores() {
        try {
            FileWriter fw = new FileWriter("scores.txt", false);
            fw.write("");
            fw.close();
        } catch (Exception e) {
            e.printStackTrace();
        }

        data.clear();
        table.setModel(new javax.swing.table.DefaultTableModel(
                new Object[][]{},
                new String[]{"Score", "Date"}
        ));
    }
}
