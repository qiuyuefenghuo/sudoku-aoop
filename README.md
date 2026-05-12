# Sudoku – CHC6186 AOOP Coursework

## Project Structure
```
src/
  sudoku/
    model/   SudokuModel.java (interface + full JML specs)
             SudokuModelImpl.java (Observable; 79 JML tags; 43 assert statements)
    view/    SudokuView.java (interface)
             SudokuFrame.java (Swing GUI; implements Observer)
    controller/ SudokuController.java (no Swing imports)
    test/    SudokuModelTest.java (JUnit 4; 3 JML-annotated scenarios)
    SudokuGUI.java  (main – GUI entry point)
    SudokuCLI.java  (main – CLI entry point; uses SudokuModel interface)
  puzzles.txt  (10,000 puzzles)
uml_class_diagram.svg
```

## Compile
```bash
mkdir -p out
cp src/puzzles.txt out/
javac -cp /usr/share/java/junit4.jar:src -d out \
  src/sudoku/model/SudokuModel.java \
  src/sudoku/model/SudokuModelImpl.java \
  src/sudoku/view/SudokuView.java \
  src/sudoku/controller/SudokuController.java \
  src/sudoku/view/SudokuFrame.java \
  src/sudoku/SudokuGUI.java \
  src/sudoku/SudokuCLI.java \
  src/sudoku/test/SudokuModelTest.java
```

## Run GUI
```bash
java -cp out sudoku.SudokuGUI
```

## Run CLI
```bash
java -cp out sudoku.SudokuCLI
```

## Run JUnit Tests (with assertions enabled)
```bash
java -ea -cp out:/usr/share/java/junit4.jar:/usr/share/java/hamcrest-core.jar \
  org.junit.runner.JUnitCore sudoku.test.SudokuModelTest
```

## Checklist summary
| Item | Status |
|------|--------|
| GUI – Swing MVC + Observer | ✅ |
| CLI – uses SudokuModel interface only | ✅ |
| FR1 Completion dialog / message | ✅ |
| FR2 Validation feedback flag | ✅ |
| FR3 Three boolean flags | ✅ |
| FR4 Only editable cells, digits 1-9 | ✅ |
| FR5 Erase/Undo/Hint/Reset/NewGame | ✅ |
| FR6 9×9 grid, 3×3 boxes, keyboard + mouse + virtual keyboard | ✅ |
| FR7 CLI commands | ✅ |
| JML specs (@invariant/@requires/@ensures) – 79 tags | ✅ |
| assert statements – 43 in model | ✅ |
| JUnit 4 – 3 scenarios, all passing | ✅ |
| UML class diagram | ✅ |
