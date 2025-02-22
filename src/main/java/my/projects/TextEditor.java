package my.projects;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.function.BiConsumer;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class TextEditor {

    private static WindowsTerminal wTerminal;
    private static int cursorX;
    private static int cursorY;
    private static int offsetX;
    private static int offsetY;
    private static int rows;
    private static int columns;
    private static List<String> content;
    private static String statusMessage;

    private final static int ARROW_UP = 1000;
    private final static int ARROW_DOWN = 1001;
    private final static int ARROW_LEFT = 1002;
    private final static int ARROW_RIGHT = 1003;
    private final static int HOME = 1004;
    private final static int END = 1005;
    private final static int PAGE_UP = 1006;
    private final static int PAGE_DOWN = 1007;
    private final static int DEL = 1008;
    private final static int BACKSPACE = 127;

    private static void editorSave() {
        if (currentFile == null) {
            return;
        }

        try {
            Files.write(currentFile, content);
            setStatusMessage("Successfully saved file");
        } catch (IOException e) {
            e.printStackTrace();
            setStatusMessage("There was an error saving your file %s".formatted(e.getMessage()));
        }
    }

    private static void insertChar(int c) {
        if (cursorY == content.size()) {
            // append row
            insertRowAt(content.size(), "");
        }
        insertCharIntoRow(cursorY, cursorX, c);
        cursorX++;
    }

    private static void deleteChar() {
        if (cursorY == content.size()) {
            return;
        }

        if (cursorX == 0 && cursorY == 0) {
            return;
        }
        if (cursorX > 0) {
            deleteCharFromRow(cursorY, cursorX - 1);
            cursorX--;
        } else {
            cursorX = content.get(cursorY - 1).length();
            appendStringToRow(cursorY - 1, content.get(cursorY));
            deleteRow(cursorY);
            cursorY--;
        }
    }

    private static void deleteRow(int at) {
        if (at < 0 || at >= content.size())
            return;
        content.remove(at);
    }

    private static void appendStringToRow(int at, String append) {
        content.set(at, content.get(at) + append);
    }

    private static void insertRowAt(int at, String rowContent) {
        if (at < 0 || at > content.size())
            return;

        content.add(at, rowContent);
    }

    private static void insertNewLine() {
        if (cursorX == 0) {
            insertRowAt(cursorY, "");
        } else {
            insertRowAt(cursorY + 1, content.get(cursorY).substring(cursorX));
            content.set(cursorY, content.get(cursorY).substring(0, cursorX));
        }
        cursorY++;
        cursorX = 0;
    }

    private static void insertCharIntoRow(int row, int at, int c) {
        if (at < 0 || at > content.get(row).length())
            at = content.get(row).length();
        String editedLine = new StringBuilder(content.get(row)).insert(at, (char) c).toString();
        content.set(row, editedLine);
    }

    private static void deleteCharFromRow(int row, int at) {
        if (at < 0 || at > content.get(row).length())
            return;
        String editedLine = new StringBuilder(content.get(row)).deleteCharAt(at).toString();
        content.set(row, editedLine);
    }

    private static void scroll() {
        if (cursorY >= rows + offsetY) {
            offsetY = cursorY - rows + 1;
        } else if (cursorY < offsetY) {
            offsetY = cursorY;
        }

        if (cursorX >= columns + offsetX) {
            offsetX = cursorX - columns + 1;
        } else if (cursorX < offsetX) {
            offsetX = cursorX;
        }
    }

    private static Path currentFile;

    public static void openFile(String[] args) {
        if (args.length == 1) {
            String filename = args[0];
            Path path = Path.of(filename);
            if (Files.exists(path)) {
                try (Stream<String> stream = Files.lines(path)) {
                    content = stream.collect((Collectors.toCollection(ArrayList::new)));
                } catch (IOException e) {
                    e.printStackTrace();
                    // TODO
                }
                currentFile = path;
            } else {
                try {
                    currentFile = Files.createFile(path);
                } catch (IOException e) {
                    e.printStackTrace();
                    // TODO
                }
            }
            while (true) {
                refreshScreen();
                int key;
                try {
                    key = readKey();
                    handleKey(key);
                } catch (IOException e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                }
            }
        }
    }

    public static void initEditor() {
        wTerminal = WindowsTerminal.getInstance();
        wTerminal.enableRawMode();
        offsetX = 0;
        offsetY = 0;
        cursorX = 1;
        cursorY = 1;
        content = List.of();
        WindowSize windowSize = wTerminal.getWindowSize();
        columns = windowSize.columns();
        rows = windowSize.rows() - 1;
    }

    private static void refreshScreen() {
        scroll();
        StringBuilder builder = new StringBuilder();

        drawCusorInTopLeft(builder);
        drawContent(builder);
        drawStatusBar(builder);
        drawCursor(builder);
        System.out.print(builder);
    }

    private static void drawCusorInTopLeft(StringBuilder builder) {
        builder.append("\033[H");
    }

    private static void drawCursor(StringBuilder builder) {
        builder.append(String.format("\033[%d;%dH", cursorY - offsetY + 1, cursorX - offsetX + 1));
    }

    public static void setStatusMessage(String message) {
        statusMessage = message;
    }

    public static void clearStatusMessage() {
        statusMessage = null;
    }

    public enum SearchDirection {
        FORWARDS, BACKWARDS
    }

    static int lastMatch = -1;

    static SearchDirection searchDirection = SearchDirection.FORWARDS;

    public static void editorFind() {
        prompt("Search: %s (Use ESC/Arrows/Enter)", (query, key) -> {
            if (query == null || query.isBlank()) {
                lastMatch = -1;
                searchDirection = SearchDirection.FORWARDS;
                return;
            }
            if (key == ARROW_RIGHT || key == ARROW_DOWN) {
                searchDirection = SearchDirection.FORWARDS;
            } else if (key == ARROW_LEFT || key == ARROW_UP) {
                searchDirection = SearchDirection.BACKWARDS;
            } else {
                lastMatch = -1;
                searchDirection = SearchDirection.FORWARDS;
            }

            if (lastMatch == -1)
                searchDirection = SearchDirection.FORWARDS;
            int current = lastMatch;

            for (int i = 0; i < content.size(); i++) {
                current += searchDirection == SearchDirection.FORWARDS ? 1 : -1;
                if (current == -1) {
                    current = content.size() - 1;
                } else if (current == content.size()) {
                    current = 0;
                }

                String line = content.get(current);

                int match = line.indexOf(query);

                if (match != -1) {
                    lastMatch = current;
                    cursorY = current;
                    cursorX = match;
                    offsetY = content.size();
                    return;
                }
            }
        });
    }

    private static void prompt(String initialMessage, BiConsumer<String, Integer> callback) {
        String message = initialMessage;

        StringBuilder userInputBuilder = new StringBuilder();

        while (true) {
            setStatusMessage(message);
            refreshScreen();

            int key;
            try {
                key = readKey();
            } catch (IOException e) {
                e.printStackTrace();
                key = '\033';
            }

            if (key == DEL || key == ctrl_key('h') || key == BACKSPACE) {
                if (userInputBuilder.length() > 0) {
                    userInputBuilder.deleteCharAt(userInputBuilder.length() - 1);
                    message = userInputBuilder.toString();
                }
            } else if (key == '\033') { // escap
                clearStatusMessage();
                callback.accept(userInputBuilder.toString(), key);
                return;
            } else if (key == 13) { // user pressed enter
                clearStatusMessage();
                callback.accept(userInputBuilder.toString(), key);
                return;
            } else if (!Character.isISOControl(key) && key < 128) {
                userInputBuilder.append((char) key);
                message = userInputBuilder.toString();
            }

            callback.accept(userInputBuilder.toString(), key);
        }
    }

    private static void drawStatusBar(StringBuilder builder) {
        String toDraw = statusMessage != null ? statusMessage : ("Rows: " + rows + "X:" + cursorX + " Y: " + cursorY + "  Save: Ctrl+s  Quit: Ctrl+q");

        builder.append("\033[7m")
                .append(toDraw)
                .append(" ".repeat(Math.max(0, columns - toDraw.length())))
                .append("\033[0m");
    }

    private static void drawContent(StringBuilder builder) {
        for (int i = 0; i < rows; i++) {
            int fileI = offsetY + i;
            if (fileI >= content.size()) {
                builder.append("~");
            } else {
                String line = content.get(fileI);
                int lengthToDraw = line.length() - offsetX;

                if (lengthToDraw < 0) {
                    lengthToDraw = 0;
                }
                if (lengthToDraw > columns) {
                    lengthToDraw = columns;
                }

                if (lengthToDraw > 0) {
                    builder.append(line, offsetX, offsetX + lengthToDraw);
                }

            }
            builder.append("\033[K\r\n");
        }
    }

    private static int readKey() throws IOException {
        int key = System.in.read();
        if (key != '\033') {
            return key;
        }

        int nextKey = System.in.read();
        if (nextKey != '[' && nextKey != 'O') {
            return nextKey;
        }

        int yetAnotherKey = System.in.read();

        if (nextKey == '[') {
            return switch (yetAnotherKey) {
                case 'A' -> ARROW_UP; // e.g. esc[A == arrow_up
                case 'B' -> ARROW_DOWN;
                case 'C' -> ARROW_RIGHT;
                case 'D' -> ARROW_LEFT;
                case 'H' -> HOME;
                case 'F' -> END;
                case '0', '1', '2', '3', '4', '5', '6', '7', '8', '9' -> { // e.g: esc[5~ == page_up
                    int yetYetAnotherChar = System.in.read();
                    if (yetYetAnotherChar != '~') {
                        yield yetYetAnotherChar;
                    }
                    switch (yetAnotherKey) {
                        case '1':
                        case '7':
                            yield HOME;
                        case '3':
                            yield DEL;
                        case '4':
                        case '8':
                            yield END;
                        case '5':
                            yield PAGE_UP;
                        case '6':
                            yield PAGE_DOWN;
                        default:
                            yield yetAnotherKey;
                    }
                }
                default -> yetAnotherKey;
            };
        } else { // if (nextKey == 'O') { e.g. escpOH == HOME
            return switch (yetAnotherKey) {
                case 'H' -> HOME;
                case 'F' -> END;
                default -> yetAnotherKey;
            };
        }
    }

    public static int ctrl_key(int key) {
        return key & 0x1f;
    }

    private static void handleKey(int key) {
        if (key == ctrl_key('q')) {
            exit();
        } else if (key == '\r') {
            insertNewLine();
        } else if (key == ctrl_key('f')) {
            editorFind();
        } else if (key == ctrl_key('s')) {
            editorSave();
        } else if (List.of(BACKSPACE, ctrl_key('h'), DEL).contains(key)) {
            deleteChar();
        } else if (List.of(ARROW_UP, ARROW_DOWN, ARROW_LEFT, ARROW_RIGHT, HOME, END, PAGE_UP, PAGE_DOWN)
                .contains(key)) {
            moveCursor(key);
        } else {
            insertChar(key);
        }
        /*
         * else {
         * System.out.print((char) + key + " -> (" + key + ")\r\n");
         * }
         */
    }

    private static void exit() {
        System.out.print("\033[2J");
        System.out.print("\033[H");
        wTerminal.disableRawMode();
        System.exit(0);
    }

    private static void moveCursor(int key) {
        String line = currentLine();
        switch (key) {
            case ARROW_UP -> {
                if (cursorY > 0) {
                    cursorY--;
                }
            }
            case ARROW_DOWN -> {
                if (cursorY < content.size()) {
                    cursorY++;
                }
            }
            case ARROW_LEFT -> {
                if (cursorX > 0) {
                    cursorX--;
                }
            }
            case ARROW_RIGHT -> {
                if (line != null && cursorX < line.length()) {
                    cursorX++;
                }
            }
            case PAGE_UP, PAGE_DOWN -> {

                if (key == PAGE_UP) {
                    moveCursorToTopOffScreen();
                } else if (key == PAGE_DOWN) {
                    moveCursorToBottomOffScreen();
                }

                for (int i = 0; i < rows; i++) {
                    moveCursor(key == PAGE_UP ? ARROW_UP : ARROW_DOWN);
                }

            }
            case HOME -> cursorX = 0;
            case END -> {
                if (line != null) {
                    cursorX = line.length();
                }
            }
        }

        String newLine = currentLine();
        if (newLine != null && cursorX > newLine.length()) {
            cursorX = newLine.length();
        }
    }

    private static String currentLine() {
        return cursorY < content.size() ? content.get(cursorY) : null;
    }

    private static void moveCursorToTopOffScreen() {
        cursorY = offsetY;
    }

    private static void moveCursorToBottomOffScreen() {
        cursorY = offsetY + rows - 1;
        if (cursorY > content.size())
            cursorY = content.size();
    }
}
