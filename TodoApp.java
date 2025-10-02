import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.nio.file.*;
import java.util.List;

public class TodoApp extends JFrame {
    private final DefaultListModel<Task> model = new DefaultListModel<>();
    private final JList<Task> list = new JList<>(model);
    private final JTextField input = new JTextField();
    private final JButton addButton = new JButton("Add");
    private final JButton deleteButton = new JButton("Delete");
    private final JButton toggleButton = new JButton("Done / Undo");
    private final Path saveFile = Paths.get(System.getProperty("user.home"), ".todoapp.txt");

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            TodoApp app = new TodoApp();
            app.setVisible(true);
        });
    }

    public TodoApp() {
        super("To-Do App");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(520, 420);
        setLocationRelativeTo(null);
        initComponents();
        loadTasks();
    }

    private void initComponents() {
        JPanel top = new JPanel(new BorderLayout(6, 6));
        top.setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));
        input.setPreferredSize(new Dimension(200, 28));
        top.add(input, BorderLayout.CENTER);
        top.add(addButton, BorderLayout.EAST);
        add(top, BorderLayout.NORTH);

        list.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        list.setCellRenderer(new TaskCellRenderer());
        add(new JScrollPane(list), BorderLayout.CENTER);

        JPanel right = new JPanel();
        right.setLayout(new BoxLayout(right, BoxLayout.Y_AXIS));
        right.setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));
        toggleButton.setAlignmentX(Component.CENTER_ALIGNMENT);
        deleteButton.setAlignmentX(Component.CENTER_ALIGNMENT);
        right.add(toggleButton);
        right.add(Box.createVerticalStrut(8));
        right.add(deleteButton);
        add(right, BorderLayout.EAST);

        addButton.addActionListener(e -> addTask());
        input.addActionListener(e -> addTask());
        deleteButton.addActionListener(e -> deleteTask());
        toggleButton.addActionListener(e -> toggleTask());

        list.addMouseListener(new MouseAdapter() {
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2) {
                    int idx = list.locationToIndex(e.getPoint());
                    if (idx >= 0) {
                        list.setSelectedIndex(idx);
                        toggleTask();
                    }
                }
            }
        });

        list.getInputMap(JComponent.WHEN_FOCUSED).put(KeyStroke.getKeyStroke(KeyEvent.VK_DELETE, 0), "delete");
        list.getActionMap().put("delete", new AbstractAction() {
            public void actionPerformed(ActionEvent e) { deleteTask(); }
        });

        addWindowListener(new WindowAdapter() {
            public void windowClosing(WindowEvent e) { saveTasks(); }
        });
    }

    private void addTask() {
        String text = input.getText().trim();
        if (text.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Cannot add an empty task", "Warning", JOptionPane.WARNING_MESSAGE);
            return;
        }
        model.addElement(new Task(text, false));
        input.setText("");
    }

    private void deleteTask() {
        int i = list.getSelectedIndex();
        if (i == -1) {
            JOptionPane.showMessageDialog(this, "Select a task to delete", "Info", JOptionPane.INFORMATION_MESSAGE);
            return;
        }
        int ans = JOptionPane.showConfirmDialog(this, "Delete selected task?", "Confirm Delete", JOptionPane.YES_NO_OPTION);
        if (ans == JOptionPane.YES_OPTION) {
            model.remove(i);
        }
    }

    private void toggleTask() {
        int i = list.getSelectedIndex();
        if (i == -1) {
            JOptionPane.showMessageDialog(this, "Select a task first", "Info", JOptionPane.INFORMATION_MESSAGE);
            return;
        }
        Task t = model.getElementAt(i);
        t.done = !t.done;
        model.setElementAt(t, i);
        list.repaint();
    }

    private void saveTasks() {
        try (BufferedWriter w = Files.newBufferedWriter(saveFile)) {
            for (int i = 0; i < model.size(); i++) {
                Task t = model.get(i);

                w.write((t.done ? "1" : "0") + ";" + t.text.replace("\n", "\\n"));
                w.newLine();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void loadTasks() {
        if (!Files.exists(saveFile)) return;
        try {
            List<String> lines = Files.readAllLines(saveFile);
            for (String line : lines) {
                if (line.isEmpty()) continue;
                int sep = line.indexOf(';');
                if (sep <= 0) continue;
                boolean done = line.charAt(0) == '1';
                String text = line.substring(sep + 1).replace("\\n", "\n");
                model.addElement(new Task(text, done));
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }


    private static class Task {
        String text;
        boolean done;
        Task(String text, boolean done) { this.text = text; this.done = done; }
        public String toString() { return text; }
    }


    private static class TaskCellRenderer extends JLabel implements ListCellRenderer<Task> {
        public Component getListCellRendererComponent(JList<? extends Task> list, Task value, int index, boolean isSelected, boolean cellHasFocus) {
            String label;
            if (value.done) label = "<html><strike>" + escapeHtml(value.text) + "</strike></html>";
            else label = "<html>" + escapeHtml(value.text) + "</html>";
            setText(label);
            setOpaque(true);
            setBorder(BorderFactory.createEmptyBorder(4, 6, 4, 6));
            if (isSelected) {
                setBackground(list.getSelectionBackground());
                setForeground(list.getSelectionForeground());
            } else {
                setBackground(list.getBackground());
                setForeground(list.getForeground());
            }
            return this;
        }

        private static String escapeHtml(String s) {
            return s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;").replace("\n", "<br>");
        }
    }
}
