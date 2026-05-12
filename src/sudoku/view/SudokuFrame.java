package sudoku.view;

import sudoku.controller.SudokuController;
import sudoku.model.SudokuModel;

import javax.swing.*;
import javax.swing.border.LineBorder;
import java.awt.*;
import java.awt.event.*;
import java.util.Observable;
import java.util.Observer;

/**
 * The main Swing window for the Sudoku GUI version.
 *
 * Implements Observer so it can react to Model changes.
 * Has a reference to both Model (to read state) and Controller (to send actions).
 * Contains NO game logic — all decisions go through the Controller.
 */
@SuppressWarnings("deprecation")
public class SudokuFrame extends JFrame implements Observer, SudokuView {

    // ── Colours (match the reference website palette) ─────────────────────────
    private static final Color CLR_BG         = new Color(240, 240, 245);
    private static final Color CLR_GIVEN_BG   = new Color(220, 225, 235);
    private static final Color CLR_GIVEN_FG   = new Color( 30,  30,  70);
    private static final Color CLR_USER_FG    = new Color( 40, 100, 200);
    private static final Color CLR_INVALID_BG = new Color(255, 180, 180);
    private static final Color CLR_SELECT_BG  = new Color(180, 210, 255);
    private static final Color CLR_BOX_BORDER = new Color( 80,  80,  80);
    private static final Color CLR_CELL_BORDER = new Color(200, 200, 205);
    private static final Color CLR_BTN_PANEL  = new Color(230, 230, 238);
    private static final Font  FONT_CELL      = new Font("SansSerif", Font.BOLD,  22);
    private static final Font  FONT_BTN       = new Font("SansSerif", Font.PLAIN, 13);
    private static final Font  FONT_KEYPAD    = new Font("SansSerif", Font.BOLD,  18);

    // ── Model / Controller references ─────────────────────────────────────────

    private final SudokuModel      model;
    private       SudokuController controller;   // set after construction

    // ── UI Components ─────────────────────────────────────────────────────────

    private final JLabel[][] cells   = new JLabel[9][9];
    private       int selectedRow    = -1;
    private       int selectedCol    = -1;

    private JButton btnErase;
    private JButton btnUndo;
    private JButton btnHint;
    private JButton btnReset;
    private JButton btnNew;

    private JCheckBox chkValidation;
    private JCheckBox chkHint;
    private JCheckBox chkRandom;

    // ── Constructor ───────────────────────────────────────────────────────────

    public SudokuFrame(SudokuModel model) {
        super("Sudoku");
        this.model = model;
        model.addObserver(this);

        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setBackground(CLR_BG);
        setLayout(new BorderLayout(8, 8));

        add(buildGrid(),    BorderLayout.CENTER);
        add(buildSidebar(), BorderLayout.EAST);

        pack();
        setMinimumSize(getSize());
        setLocationRelativeTo(null);
        setVisible(true);
    }

    /** Called after construction to wire the controller. */
    public void setController(SudokuController ctrl) {
        this.controller = ctrl;
        controller.updateButtonStates();
    }

    // ── Grid panel ────────────────────────────────────────────────────────────

    private JPanel buildGrid() {
        // 3×3 box panels inside a 3×3 outer grid
        JPanel outer = new JPanel(new GridLayout(3, 3, 3, 3));
        outer.setBackground(CLR_BOX_BORDER);
        outer.setBorder(new LineBorder(CLR_BOX_BORDER, 3));

        for (int br = 0; br < 3; br++) {
            for (int bc = 0; bc < 3; bc++) {
                JPanel box = new JPanel(new GridLayout(3, 3, 1, 1));
                box.setBackground(CLR_CELL_BORDER);

                for (int r = br * 3; r < br * 3 + 3; r++) {
                    for (int c = bc * 3; c < bc * 3 + 3; c++) {
                        cells[r][c] = makeCell(r, c);
                        box.add(cells[r][c]);
                    }
                }
                outer.add(box);
            }
        }

        JPanel wrapper = new JPanel(new BorderLayout());
        wrapper.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 4));
        wrapper.add(outer, BorderLayout.CENTER);
        return wrapper;
    }

    private JLabel makeCell(int row, int col) {
        JLabel lbl = new JLabel("", SwingConstants.CENTER);
        lbl.setFont(FONT_CELL);
        lbl.setOpaque(true);
        lbl.setPreferredSize(new Dimension(54, 54));
        lbl.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));

        // Mouse click → select cell
        lbl.addMouseListener(new MouseAdapter() {
            @Override public void mousePressed(MouseEvent e) {
                selectCell(row, col);
            }
        });
        return lbl;
    }

    // ── Sidebar (buttons + virtual keyboard + options) ────────────────────────

    private JPanel buildSidebar() {
        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.setBackground(CLR_BG);
        panel.setBorder(BorderFactory.createEmptyBorder(10, 4, 10, 10));

        // Toolbar buttons
        JPanel toolbar = new JPanel(new GridLayout(5, 1, 5, 5));
        toolbar.setBackground(CLR_BTN_PANEL);
        toolbar.setBorder(BorderFactory.createTitledBorder("Controls"));

        btnErase = makeBtn("Erase",    e -> onErase());
        btnUndo  = makeBtn("Undo",     e -> controller.onUndo());
        btnHint  = makeBtn("Hint",     e -> controller.onHint());
        btnReset = makeBtn("Reset",    e -> controller.onReset());
        btnNew   = makeBtn("New Game", e -> controller.onNewGame());

        toolbar.add(btnErase);
        toolbar.add(btnUndo);
        toolbar.add(btnHint);
        toolbar.add(btnReset);
        toolbar.add(btnNew);

        // Virtual keyboard 1-9
        JPanel keypad = new JPanel(new GridLayout(3, 3, 4, 4));
        keypad.setBackground(CLR_BTN_PANEL);
        keypad.setBorder(BorderFactory.createTitledBorder("Keyboard"));
        for (int d = 1; d <= 9; d++) {
            final int digit = d;
            JButton kb = new JButton(String.valueOf(d));
            kb.setFont(FONT_KEYPAD);
            kb.setFocusable(false);
            kb.addActionListener(e -> onDigit(digit));
            keypad.add(kb);
        }

        // Option checkboxes
        JPanel opts = new JPanel(new GridLayout(3, 1, 2, 2));
        opts.setBackground(CLR_BTN_PANEL);
        opts.setBorder(BorderFactory.createTitledBorder("Options"));

        chkValidation = new JCheckBox("Validation feedback", model.isValidationFeedbackEnabled());
        chkHint       = new JCheckBox("Hints enabled",       model.isHintEnabled());
        chkRandom     = new JCheckBox("Random puzzle",       model.isRandomPuzzleSelection());

        styleCheckbox(chkValidation);
        styleCheckbox(chkHint);
        styleCheckbox(chkRandom);

        chkValidation.addActionListener(e -> controller.onToggleValidation(chkValidation.isSelected()));
        chkHint      .addActionListener(e -> controller.onToggleHint(chkHint.isSelected()));
        chkRandom    .addActionListener(e -> controller.onToggleRandomSelection(chkRandom.isSelected()));

        opts.add(chkValidation);
        opts.add(chkHint);
        opts.add(chkRandom);

        panel.add(toolbar);
        panel.add(Box.createVerticalStrut(10));
        panel.add(keypad);
        panel.add(Box.createVerticalStrut(10));
        panel.add(opts);
        panel.add(Box.createVerticalGlue());

        return panel;
    }

    private JButton makeBtn(String text, ActionListener al) {
        JButton btn = new JButton(text);
        btn.setFont(FONT_BTN);
        btn.setFocusable(false);
        btn.addActionListener(al);
        return btn;
    }

    private void styleCheckbox(JCheckBox cb) {
        cb.setBackground(CLR_BTN_PANEL);
        cb.setFont(FONT_BTN);
        cb.setFocusable(false);
    }

    // ── Cell selection ────────────────────────────────────────────────────────

    private void selectCell(int row, int col) {
        selectedRow = row;
        selectedCol = col;
        refreshDisplay();

        // Also enable keyboard input from actual keyboard
        requestFocusInWindow();
    }

    // ── Action handlers ───────────────────────────────────────────────────────

    private void onDigit(int digit) {
        if (selectedRow < 0) return;
        controller.onDigitEntered(selectedRow, selectedCol, digit);
    }

    private void onErase() {
        if (selectedRow < 0) return;
        controller.onErase(selectedRow, selectedCol);
    }

    // ── Observer callback ─────────────────────────────────────────────────────

    @Override
    public void update(Observable o, Object arg) {
        SwingUtilities.invokeLater(() -> {
            refreshDisplay();
            if ("complete".equals(arg)) {
                showCompletionDialog();
            }
            if ("newgame".equals(arg) || "reset".equals(arg)) {
                selectedRow = -1;
                selectedCol = -1;
            }
            if (controller != null) controller.updateButtonStates();
        });
    }

    // ── Display refresh ───────────────────────────────────────────────────────

    private void refreshDisplay() {
        boolean validationOn = model.isValidationFeedbackEnabled();

        for (int r = 0; r < 9; r++) {
            for (int c = 0; c < 9; c++) {
                JLabel lbl  = cells[r][c];
                int    val  = model.getCell(r, c);
                boolean giv = model.isGiven(r, c);
                boolean inv = validationOn && model.isInvalid(r, c);
                boolean sel = (r == selectedRow && c == selectedCol);

                // Text
                lbl.setText(val == 0 ? "" : String.valueOf(val));
                lbl.setForeground(giv ? CLR_GIVEN_FG : CLR_USER_FG);

                // Background priority: selected > invalid > given/plain
                if (sel) {
                    lbl.setBackground(CLR_SELECT_BG);
                } else if (inv) {
                    lbl.setBackground(CLR_INVALID_BG);
                } else if (giv) {
                    lbl.setBackground(CLR_GIVEN_BG);
                } else {
                    lbl.setBackground(Color.WHITE);
                }
            }
        }
    }

    private void showCompletionDialog() {
        JOptionPane.showMessageDialog(
            this,
            "Congratulations! You solved the puzzle!",
            "Puzzle Complete",
            JOptionPane.INFORMATION_MESSAGE);
    }

    // ── SudokuView interface ──────────────────────────────────────────────────

    @Override
    public void setHintButtonEnabled(boolean enabled) {
        btnHint.setEnabled(enabled);
    }

    // ── Keyboard support ──────────────────────────────────────────────────────

    /**
     * Wire physical keyboard so digits and arrow keys work.
     * Call this after the frame is visible.
     */
    public void installKeyboardHandler() {
        setFocusable(true);
        addKeyListener(new KeyAdapter() {
            @Override public void keyPressed(KeyEvent e) {
                int k = e.getKeyCode();
                if (k >= KeyEvent.VK_1 && k <= KeyEvent.VK_9) {
                    onDigit(k - KeyEvent.VK_0);
                } else if (k >= KeyEvent.VK_NUMPAD1 && k <= KeyEvent.VK_NUMPAD9) {
                    onDigit(k - KeyEvent.VK_NUMPAD0);
                } else if (k == KeyEvent.VK_DELETE || k == KeyEvent.VK_BACK_SPACE) {
                    onErase();
                } else {
                    handleArrow(k);
                }
            }
        });
    }

    private void handleArrow(int k) {
        if (selectedRow < 0) { selectCell(0, 0); return; }
        int r = selectedRow;
        int c = selectedCol;
        switch (k) {
            case KeyEvent.VK_UP:    r = Math.max(0, r - 1); break;
            case KeyEvent.VK_DOWN:  r = Math.min(8, r + 1); break;
            case KeyEvent.VK_LEFT:  c = Math.max(0, c - 1); break;
            case KeyEvent.VK_RIGHT: c = Math.min(8, c + 1); break;
            default: return;
        }
        selectCell(r, c);
    }
}
