package sudoku;

import sudoku.controller.SudokuController;
import sudoku.model.SudokuModel;
import sudoku.model.SudokuModelImpl;
import sudoku.view.SudokuFrame;

import javax.swing.SwingUtilities;

/**
 * Entry point for the GUI version of Sudoku.
 * Creates Model, View, and Controller and wires them together.
 */
public class SudokuGUI {

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            SudokuModel      model = new SudokuModelImpl();
            SudokuFrame      view  = new SudokuFrame(model);
            SudokuController ctrl  = new SudokuController(model, view);
            view.setController(ctrl);
            view.installKeyboardHandler();
        });
    }
}
