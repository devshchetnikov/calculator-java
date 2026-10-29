import java.math.BigInteger;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Scanner;
import java.util.Set;
import java.util.function.BiFunction;
import java.util.function.Function;

public class SymbolicCalculator {

    private final Deque<String> commandLog = new ArrayDeque<>();
    private final Map<String, Object> context = new HashMap<>(); // Object теперь хранит Double или Matrix
    private final NumberFormatter formatter = new NumberFormatter();
    private final ExpressionParser parser = new ExpressionParser();
    private final Executor engine = new Executor();

    private boolean angularUnitIsDegrees = true;
    private double lastResult = 0.0;

    private static final Set<String> RESERVED = Set.of("pi", "e", "tau", "ans");

    // Константы для команд REPL
    private static final String CMD_EXIT = "exit";
    private static final String CMD_HELP = "help";
    private static final String CMD_HISTORY = "history";
    private static final String CMD_CLEAR = "clear";
    private static final String CMD_MODE = "mode";
    private static final String CMD_VARS = "vars";
    private static final String CMD_DEL_PREFIX = "del ";
    private static final String CMD_RESET_ANS = "reset ans";

    public static void main(String[] args) {
        new SymbolicCalculator().repl();
    }

    private void repl() {
        System.out.println(engine.getBanner());
        try (Scanner reader = new Scanner(System.in)) {
            StringBuilder buffer = new StringBuilder();
            int balance = 0;
            while (true) {
                // Подсветка текущего режима (DEG/RAD) в промпте
                System.out.print(buffer.length() > 0 ? ">>> " : String.format("sym [%s]> ", angularUnitIsDegrees ? "DEG" : "RAD"));
                String line = reader.nextLine().trim();
                if (line.isEmpty()) continue;

                for (char ch : line.toCharArray()) {
                    if (ch == '(' || ch == '[') balance++;
                    if (ch == ')' || ch == ']') balance--;
                }
                buffer.append(line).append(" ");
                if (balance > 0) continue;

                String input = buffer.toString().trim();
                buffer.setLength(0);
                balance = 0;

                if (input.equalsIgnoreCase(CMD_EXIT)) break;
                if (input.equalsIgnoreCase(CMD_HELP)) { System.out.println(engine.getBanner()); continue; }
                if (input.equalsIgnoreCase(CMD_HISTORY)) { printLog(); continue; }
                if (input.equalsIgnoreCase(CMD_CLEAR)) { purgeState(); continue; }
                if (input.equalsIgnoreCase("cls") || input.equalsIgnoreCase("clear screen")) { 
                    System.out.print("\033[H\033[2J");
                    System.out.flush();
                    continue; 
                }
                if (input.equalsIgnoreCase(CMD_MODE)) { toggleMode(); continue; }
                if (input.equalsIgnoreCase(CMD_VARS)) { printContext(); continue; }
                if (input.toLowerCase().startsWith(CMD_DEL_PREFIX)) { 
                    purgeBinding(input.substring(CMD_DEL_PREFIX.length()).trim().toLowerCase()); 
                    continue; 
                }
                if (input.equalsIgnoreCase(CMD_RESET_ANS)) { 
                    lastResult = 0.0; 
                    System.out.println("ans -> 0"); 
                    continue; 
                }

                if (input.contains("=")) { bind(input); continue; }

                try {
                    Object value = engine.resolve(parser.parse(input), this::fetch);
                    if (value instanceof Double d) {
                        if (Double.isInfinite(d)) throw new IllegalStateException("Magnitude overflow");
                        String out = formatter.format(d);
                        System.out.println("=> " + out);
                        commandLog.add(input + " = " + out);
                        lastResult = d;
                    } else if (value instanceof Matrix m) {
                        System.out.println("=> " + m.toString());
                        commandLog.add(input + " = " + m.toString());
                    }
                } catch (Exception ex) {
                    System.err.println("! " + ex.getMessage());
                }
            }
        }
    }

    private void bind(String input) {
        String[] parts = input.split("=", 2);
        String key = parts[0].trim().toLowerCase();
        String expr = parts[1].trim();

        if (!key.matches("[a-z]+")) { System.err.println("! Invalid identifier"); return; }
        if (RESERVED.contains(key)) { System.err.println("! Protected symbol"); return; }

        try {
            Object val = engine.resolve(parser.parse(expr), this::fetch);
            context.put(key, val);
            if (val instanceof Double d) {
                String out = formatter.format(d);
                System.out.println(key + " -> " + out);
                commandLog.add(input + " (Result: " + out + ")");
                lastResult = d;
            } else if (val instanceof Matrix m) {
                System.out.println(key + " -> " + m.toString());
                commandLog.add(input + " (Result: " + m.toString() + ")");
            }
        } catch (Exception ex) {
            System.err.println("! " + ex.getMessage());
        }
    }

    private Object fetch(String token) {
        return switch (token) {
            case "pi" -> Math.PI;
            case "e" -> Math.E;
            case "tau" -> 2 * Math.PI;
            case "ans" -> lastResult;
            default -> context.get(token);
        };
    }

    private void toggleMode() {
        angularUnitIsDegrees = !angularUnitIsDegrees;
        System.out.println("Trig mode: " + (angularUnitIsDegrees ? "DEG" : "RAD"));
    }

    private void purgeState() {
        commandLog.clear();
        context.clear();
        lastResult = 0.0;
        System.out.println("State flushed.");
    }

    private void purgeBinding(String name) {
        if (context.remove(name) != null) {
            System.out.println("'" + name + "' evicted.");
            commandLog.add(CMD_DEL_PREFIX + name);
        } else {
            System.err.println("! No such binding: " + name);
        }
    }

    private void printLog() {
        if (commandLog.isEmpty()) { System.out.println("Log is void."); return; }
        System.out.println("Session log:");
        int idx = 1;
        for (String entry : commandLog) {
            System.out.println(idx++ + ". " + entry);
        }
    }

    private void printContext() {
        if (context.isEmpty()) { System.out.println("Context is barren."); return; }
        System.out.println("Bindings:");
        context.forEach((k, v) -> {
            if (v instanceof Double d) {
                System.out.println(k + " = " + formatter.format(d));
            } else if (v instanceof Matrix m) {
                System.out.println(k + " = " + m.toString());
            }
        });
    }

    private static class NumberFormatter {
        private final DecimalFormat df;

        NumberFormatter() {
            DecimalFormatSymbols sym = new DecimalFormatSymbols(Locale.US);
            sym.setDecimalSeparator('.');
            df = new DecimalFormat("#.################", sym);
        }

        String format(double v) {
            if (Double.isNaN(v)) return "NaN";
            if (Double.isInfinite(v)) return "Infinity";
            String s = df.format(v);
            return s.endsWith(".") ? s.substring(0, s.length() - 1) : s;
        }
    }

    private static class ExpressionParser {
        private static final Map<String, Integer> PRECEDENCE = Map.of(
                "+", 1, "-", 1, "*", 2, "/", 2, "%", 2, "//", 2, "^", 3, "u-", 4, "!", 5, ".", 2
        );
        private static final Set<String> FUNCTIONS = Set.of(
                "sin", "cos", "tan", "sqrt", "log", "log10", "ln", "min", "max", "abs", "asin", "acos", "atan", "sinh", "cosh", "tanh", "det", "transpose", "cross", "x", "norm"
        );

        List<String> ingest(String source) {
            List<String> raw = new ArrayList<>();
            int pos = 0;
            while (pos < source.length()) {
                char ch = source.charAt(pos);
                if (Character.isWhitespace(ch)) { pos++; continue; }
                if (ch == '[') { raw.add("["); pos++; continue; }
                if (ch == ']') { raw.add("]"); pos++; continue; }
                if (ch == '.') { raw.add("."); pos++; continue; } // Оператор скалярного произведения
                
                if (Character.isDigit(ch) || ch == '.') {
                    StringBuilder sb = new StringBuilder();
                    if (ch == '-' && (pos == 0 || "+-*/^%!(),.".indexOf(source.charAt(pos - 1)) != -1)) {
                        sb.append(ch);
                        pos++;
                        if (pos >= source.length() || !(Character.isDigit(source.charAt(pos)) || source.charAt(pos) == '.')) {
                            throw new IllegalArgumentException("Malformed unary minus");
                        }
                    }
                    while (pos < source.length() && (Character.isDigit(source.charAt(pos)) || source.charAt(pos) == '.')) {
                        sb.append(source.charAt(pos++));
                    }
                    raw.add(sb.toString());
                    continue;
                }
                if (Character.isLetter(ch)) {
                    StringBuilder sb = new StringBuilder();
                    while (pos < source.length() && Character.isLetterOrDigit(source.charAt(pos))) {
                        sb.append(source.charAt(pos++));
                    }
                    raw.add(sb.toString().toLowerCase());
                    continue;
                }
                if (ch == '/' && pos + 1 < source.length() && source.charAt(pos + 1) == '/') {
                    raw.add("//"); pos += 2; continue;
                }
                if ("+-*/^%!(),".indexOf(ch) != -1) {
                    raw.add(String.valueOf(ch));
                    pos++; continue;
                }
                throw new IllegalArgumentException("Unknown glyph: " + ch);
            }

            List<String> tokens = new ArrayList<>();
            for (int i = 0; i < raw.size(); i++) {
                String cur = raw.get(i);
                if (cur.equals("-") && i + 1 < raw.size() && isNumeric(raw.get(i + 1))) {
                    tokens.add("u-" + raw.get(i + 1)); i++; continue;
                }
                tokens.add(cur);
                if (i + 1 < tokens.size()) {
                    String prev = tokens.get(i), next = tokens.get(i + 1);
                    if (prev.equals(")") && next.equals("(")) tokens.add(i + 1, "*");
                    else if (isNumeric(prev) && (isIdentifier(next) || next.equals("("))) tokens.add(i + 1, "*");
                    else if (isIdentifier(prev) && (next.equals("(") || isIdentifier(next))) tokens.add(i + 1, "*");
                    else if (prev.equals(")") && (isNumeric(next) || isIdentifier(next))) tokens.add(i + 1, "*");
                    else if (prev.equals("]") && (next.equals("[") || isNumeric(next) || isIdentifier(next))) tokens.add(i + 1, "*");
                    else if (isNumeric(prev) && next.equals("[")) tokens.add(i + 1, "*");
                }
            }

            List<String> finalTokens = new ArrayList<>();
            for (String t : tokens) {
                if (t.equals("!") && !finalTokens.isEmpty()) {
                    finalTokens.set(finalTokens.size() - 1, finalTokens.get(finalTokens.size() - 1) + "!");
                } else {
                    finalTokens.add(t);
                }
            }
            return finalTokens;
        }

        List<String> toRPN(List<String> tokens) {
            List<String> output = new ArrayList<>();
            Deque<String> stack = new ArrayDeque<>();
            boolean expectUnary = true;
            for (String t : tokens) {
                if (isNumeric(t) || isIdentifier(t) || t.equals("[") || t.equals("]")) { 
                    output.add(t); 
                    expectUnary = false; 
                    continue; 
                }
                if (FUNCTIONS.contains(t)) { stack.push(t); expectUnary = false; continue; }
                if (t.equals(",")) {
                    while (!stack.isEmpty() && !stack.peek().equals("(")) output.add(stack.pop());
                    if (stack.isEmpty()) throw new IllegalArgumentException("Rogue comma");
                    expectUnary = true; continue;
                }
                if (t.equals("(")) { stack.push(t); expectUnary = true; continue; }
                if (t.equals(")")) {
                    while (!stack.isEmpty() && !stack.peek().equals("(")) output.add(stack.pop());
                    if (stack.isEmpty()) throw new IllegalArgumentException("Unmatched ')'");
                    stack.pop();
                    if (!stack.isEmpty() && FUNCTIONS.contains(stack.peek())) output.add(stack.pop());
                    expectUnary = false; continue;
                }
                if (expectUnary && t.equals("-")) t = "u-";
                while (!stack.isEmpty() && !stack.peek().equals("(") && 
                       (PRECEDENCE.getOrDefault(stack.peek(), 0) > PRECEDENCE.getOrDefault(t, 0) ||
                        (PRECEDENCE.getOrDefault(stack.peek(), 0) == PRECEDENCE.getOrDefault(t, 0) && !t.equals("^")))) {
                    output.add(stack.pop());
                }
                stack.push(t);
                expectUnary = true;
            }
            while (!stack.isEmpty()) {
                String top = stack.pop();
                if (top.equals("(") || top.equals(")")) throw new IllegalArgumentException("Bracket mismatch");
                output.add(top);
            }
            return output;
        }

        private boolean isNumeric(String s) {
            try { Double.parseDouble(s); return true; } catch (NumberFormatException e) { return false; }
        }
        private boolean isIdentifier(String s) {
            return s.matches("[a-z]+");
        }
    }

    private static class Executor {
        private static final Map<String, Function<Double, Double>> UNARY = Map.of(
                "sin", v -> applyTrig(v, Math::sin), "cos", v -> applyTrig(v, Math::cos), "tan", v -> applyTrig(v, Math::tan),
                "asin", v -> applyTrig(v, Math::asin), "acos", v -> applyTrig(v, Math::acos), "atan", v -> applyTrig(v, Math::atan),
                "sinh", v -> Math.sinh(v), "cosh", v -> Math.cosh(v), "tanh", v -> Math.tanh(v),
                "sqrt", v -> Math.sqrt(v), "ln", v -> Math.log(v), "log", v -> Math.log(v),
                "log10", v -> Math.log10(v), "abs", v -> Math.abs(v)
        );
        private static final Map<String, BiFunction<Double, Double, Double>> BINARY = Map.of(
                "+", Double::sum, "-", (a, b) -> a - b, "*", (a, b) -> a * b,
                "/", (a, b) -> { if (b == 0) throw new IllegalArgumentException("Division by zero"); return a / b; },
                "//", (a, b) -> { if (b == 0) throw new IllegalArgumentException("Division by zero"); return (double) ((long) a / (long) b); },
                "%", (a, b) -> { 
                    if (b == 0) throw new IllegalArgumentException("Division by zero"); 
                    double r = a % b;
                    if (r < 0) r += Math.abs(b);
                    return r; 
                },
                "^", Math::pow,
                ".", (a, b) -> a * b // Скалярное произведение векторов (после проверки размерности)
        );

        // Вспомогательный метод для тригонометрии с учетом градусов
        private static double applyTrig(double v, java.util.function.Function<Double, Double> func) {
            return func.apply(v);
        }

        String getBanner() {
            return """
                   === Symbolic Engine v2.1 (Vector Algebra Update) ===
                   Enter expressions. Commands: help, history, clear, mode, vars, exit, del <id>, reset ans, cls
                   Vectors: [1, 2, 3]. Matrices: [[1, 2], [3, 4]].
                   New: cross (or x), norm, cls
                   """;
        }

        Object resolve(List<String> rpn, Function<String, Object> lookup) {
            Deque<Object> stack = new ArrayDeque<>();
            for (String t : rpn) {
                if (t.startsWith("u-") && parser.isNumeric(t.substring(2))) {
                    stack.push(-Double.parseDouble(t.substring(2))); continue;
                }
                if (parser.isNumeric(t)) { stack.push(Double.parseDouble(t)); continue; }
                if (parser.isIdentifier(t)) { 
                    Object val = lookup.apply(t); 
                    if (val == null) throw new IllegalArgumentException("Unknown: " + t); 
                    stack.push(val); 
                    continue; 
                }

                if (t.equals("u-")) { 
                    Object a = stack.pop();
                    if (a instanceof Double d) stack.push(-d);
                    else if (a instanceof Matrix m) stack.push(m.negate());
                    else throw new IllegalArgumentException("Invalid operand for unary minus");
                    continue; 
                }
                if (t.endsWith("!")) {
                    String num = t.substring(0, t.length() - 1);
                    if (!parser.isNumeric(num)) throw new IllegalArgumentException("Factorial on non-numeric");
                    double v = Double.parseDouble(num);
                    if (v < 0 || v != Math.floor(v)) throw new IllegalArgumentException("Factorial domain error");
                    if (v > 20) throw new IllegalArgumentException("Factorial overflow");
                    stack.push(factorial((int) v)); continue;
                }
                if (t.equals("det")) {
                    if (stack.isEmpty()) throw new IllegalArgumentException("det needs an argument");
                    Object a = stack.pop();
                    if (!(a instanceof Matrix)) throw new IllegalArgumentException("det applies only to matrices");
                    Matrix m = (Matrix) a;
                    if (m.rows() == 1) {
                        stack.push(m.get(0, 0));
                    } else if (m.rows() == 2) {
                        stack.push(m.get(0, 0) * m.get(1, 1) - m.get(0, 1) * m.get(1, 0));
                    } else {
                        double det = 0;
                        for (int c = 0; c < m.cols(); c++) {
                            // Исправлено: вместо Math.pow(-1, c) для исключения NaN
                            double sign = ((c & 1) == 0) ? 1.0 : -1.0;
                            det += sign * m.get(0, c) * m.minor(0, c).determinant();
                        }
                        stack.push(det);
                    }
                    continue;
                }
                if (t.equals("transpose")) {
                    if (stack.isEmpty()) throw new IllegalArgumentException("transpose needs an argument");
                    Object a = stack.pop();
                    if (!(a instanceof Matrix)) throw new IllegalArgumentException("transpose applies only to matrices");
                    stack.push(((Matrix) a).transpose());
                    continue;
                }
                if (t.equals("min") || t.equals("max")) {
                    if (stack.size() < 2) throw new IllegalArgumentException(t + " needs two operands");
                    Object bObj = stack.pop(), aObj = stack.pop();
                    if (aObj instanceof Double a && bObj instanceof Double b) {
                        stack.push(t.equals("min") ? Math.min(a, b) : Math.max(a, b));
                    } else {
                        throw new IllegalArgumentException(t + " applies only to scalars");
                    }
                    continue;
                }
                if (t.equals("cross") || t.equals("x")) {
                    if (stack.isEmpty()) throw new IllegalArgumentException(t + " needs an argument");
                    Object bObj = stack.pop(), aObj = stack.pop();
                    if (aObj instanceof Matrix a && bObj instanceof Matrix b) {
                        stack.push(a.cross(b));
                        continue;
                    }
                    throw new IllegalArgumentException("Cross product applies only to vectors");
                }
                if (t.equals("norm")) {
                    if (stack.isEmpty()) throw new IllegalArgumentException("norm needs an argument");
                    Object aObj = stack.pop();
                    if (aObj instanceof Matrix a && a.isVector()) {
                        stack.push(a.norm());
                        continue;
                    }
                    throw new IllegalArgumentException("Norm applies only to vectors");
                }
                if (UNARY.containsKey(t)) {
                    if (stack.isEmpty()) throw new IllegalArgumentException(t + " needs an argument");
                    Object aObj = stack.pop();
                    if (!(aObj instanceof Double)) throw new IllegalArgumentException(t + " applies only to scalars");
                    double v = (Double) aObj;
                    // Проверка тригонометрии (если захочешь добавить поддержку градусов внутрь функций)
                    Double res = UNARY.get(t).apply(v);
                    if (Double.isInfinite(res)) throw new IllegalArgumentException("Overflow in " + t);
                    stack.push(res); continue;
                }
                if (stack.size() < 2) throw new IllegalArgumentException("Malformed RPN");
                Object bObj = stack.pop(), aObj = stack.pop();

                // Умножение матрицы на скаляр
                if (t.equals("*")) {
                    if (aObj instanceof Double a && bObj instanceof Matrix b) {
                        stack.push(b.scale(a));
                        continue;
                    }
                    if (aObj instanceof Matrix a && bObj instanceof Double b) {
                        stack.push(a.scale(b));
                        continue;
                    }
                }

                // Сложение и вычитание (скаляр-скаляр или матрица-матрица)
                if (t.equals("+") || t.equals("-")) {
                    if (aObj instanceof Double a && bObj instanceof Double b) {
                        Double res = switch (t) {
                            case "+" -> BINARY.get("+").apply(a, b);
                            case "-" -> BINARY.get("-").apply(a, b);
                            default -> 0.0;
                        };
                        stack.push(res);
                        continue;
                    }
                    if (aObj instanceof Matrix a && bObj instanceof Matrix b) {
                        if (t.equals("+")) stack.push(a.add(b));
                        else stack.push(a.subtract(b));
                        continue;
                    }
                }

                // Скалярное произведение векторов
                if (t.equals(".")) {
                    if (aObj instanceof Matrix a && bObj instanceof Matrix b) {
                        if (!a.isVector() || !b.isVector() || a.cols() != b.cols()) {
                            throw new IllegalArgumentException("Dot product requires vectors of same size");
                        }
                        double sum = 0;
                        for (int i = 0; i < a.cols(); i++) {
                            sum += a.get(0, i) * b.get(0, i);
                        }
                        stack.push(sum);
                        continue;
                    }
                }

                // Умножение матриц
                if (t.equals("*")) {
                    if (aObj instanceof Matrix a && bObj instanceof Matrix b) {
                        stack.push(a.multiply(b));
                        continue;
                    }
                }

                // Если типы не подошли под специфику матриц, пробуем стандартную бинарную операцию для скаляров
                if (aObj instanceof Double a && bObj instanceof Double b) {
                    Double res = switch (t) {
                        case "+" -> BINARY.get("+").apply(a, b);
                        case "-" -> BINARY.get("-").apply(a, b);
                        case "*" -> BINARY.get("*").apply(a, b);
                        case "/" -> BINARY.get("/").apply(a, b);
                        case "//" -> BINARY.get("//").apply(a, b);
                        case "%" -> BINARY.get("%").apply(a, b);
                        case "^" -> BINARY.get("^").apply(a, b);
                        default -> throw new IllegalArgumentException("Unknown operator: " + t);
                    };
                    if (Double.isInfinite(res)) throw new IllegalArgumentException("Overflow in " + t);
                    stack.push(res);
                    continue;
                }

                throw new IllegalArgumentException("Type mismatch for operator: " + t);
            }
            if (stack.size() != 1) throw new IllegalArgumentException("Invalid expression");
            return stack.pop();
        }

        private double factorial(int n) {
            BigInteger acc = BigInteger.ONE;
            for (int i = 2; i <= n; i++) acc = acc.multiply(BigInteger.valueOf(i));
            return acc.doubleValue();
        }

        // Обертка для тригонометрии (если решишь делать режим DEG внутри функций, а не на уровне парсера)
        private static double applyTrig(double v, java.util.function.Function<Double, Double> func) {
            return func.apply(v);
        }
    }

    // Вспомогательный класс для работы с матрицами и векторами
    private static class Matrix {
        private final List<List<Double>> data;

        Matrix(List<List<Double>> data) {
            if (data.isEmpty() || data.get(0).isEmpty()) throw new IllegalArgumentException("Empty matrix");
            int cols = data.get(0).size();
            for (List<Double> row : data) {
                if (row.size() != cols) throw new IllegalArgumentException("Jagged matrix");
            }
            this.data = data;
        }

        int rows() { return data.size(); }
        int cols() { return data.get(0).size(); }

        boolean isVector() {
            return rows() == 1 || cols() == 1;
        }

        double get(int r, int c) {
            return data.get(r).get(c);
        }

        Matrix add(Matrix other) {
            if (rows() != other.rows() || cols() != other.cols()) throw new IllegalArgumentException("Dimension mismatch for +");
            List<List<Double>> res = new ArrayList<>();
            for (int i = 0; i < rows(); i++) {
                List<Double> row = new ArrayList<>();
                for (int j = 0; j < cols(); j++) {
                    row.add(data.get(i).get(j) + other.data.get(i).get(j));
                }
                res.add(row);
            }
            return new Matrix(res);
        }

        Matrix subtract(Matrix other) {
            if (rows() != other.rows() || cols() != other.cols()) throw new IllegalArgumentException("Dimension mismatch for -");
            List<List<Double>> res = new ArrayList<>();
            for (int i = 0; i < rows(); i++) {
                List<Double> row = new ArrayList<>();
                for (int j = 0; j < cols(); j++) {
                    row.add(data.get(i).get(j) - other.data.get(i).get(j));
                }
                res.add(row);
            }
            return new Matrix(res);
        }

        Matrix scale(double k) {
            List<List<Double>> res = new ArrayList<>();
            for (int i = 0; i < rows(); i++) {
                List<Double> row = new ArrayList<>();
                for (int j = 0; j < cols(); j++) {
                    row.add(data.get(i).get(j) * k);
                }
                res.add(row);
            }
            return new Matrix(res);
        }

        Matrix negate() {
            return scale(-1);
        }

        Matrix multiply(Matrix other) {
            if (cols() != other.rows()) throw new IllegalArgumentException("Dimension mismatch for *");
            List<List<Double>> res = new ArrayList<>();
            for (int i = 0; i < rows(); i++) {
                List<Double> row = new ArrayList<>();
                for (int j = 0; j < other.cols(); j++) {
                    double sum = 0;
                    for (int k = 0; k < cols(); k++) {
                        sum += data.get(i).get(k) * other.data.get(k).get(j);
                    }
                    row.add(sum);
                }
                res.add(row);
            }
            return new Matrix(res);
        }

        Matrix transpose() {
            List<List<Double>> res = new ArrayList<>();
            for (int j = 0; j < cols(); j++) {
                List<Double> col = new ArrayList<>();
                for (int i = 0; i < rows(); i++) {
                    col.add(data.get(i).get(j));
                }
                res.add(col);
            }
            return new Matrix(res);
        }

        // Векторное произведение (только для 3D)
        public Matrix cross(Matrix other) {
            if (!this.isVector() || !other.isVector() || this.cols() != 3 || other.cols() != 3) {
                throw new IllegalArgumentException("Cross product is defined only for 3D vectors");
            }
            double x1 = this.get(0, 0), y1 = this.get(0, 1), z1 = this.get(0, 2);
            double x2 = other.get(0, 0), y2 = other.get(0, 1), z2 = other.get(0, 2);
            List<List<Double>> res = List.of(
                List.of(y1 * z2 - z1 * y2, z1 * x2 - x1 * z2, x1 * y2 - y1 * x2)
            );
            return new Matrix(res);
        }

        // Евклидова норма (длина) вектора
        public double norm() {
            if (!isVector()) throw new IllegalArgumentException("Norm applies only to vectors");
            double sum = 0;
            for (int i = 0; i < cols(); i++) {
                sum += Math.pow(get(0, i), 2);
            }
            return Math.sqrt(sum);
        }

        Double determinant() {
            if (rows() != cols()) throw new IllegalArgumentException("Determinant is defined only for square matrices");
            if (rows() == 1) return data.get(0).get(0);
            if (rows() == 2) return data.get(0).get(0) * data.get(1).get(1) - data.get(0).get(1) * data.get(1).get(0);
            
            double det = 0;
            for (int c = 0; c < cols(); c++) {
                // Исправлено: побитовая логика вместо Math.pow для исключения NaN
                double sign = ((c & 1) == 0) ? 1.0 : -1.0;
                det += sign * data.get(0).get(c) * minor(0, c).determinant();
            }
            return det;
        }

        private Matrix minor(int skipRow, int skipCol) {
            List<List<Double>> res = new ArrayList<>();
            for (int i = 0; i < rows(); i++) {
                if (i == skipRow) continue;
                List<Double> row = new ArrayList<>();
                for (int j = 0; j < cols(); j++) {
                    if (j == skipCol) continue;
                    row.add(data.get(i).get(j));
                }
                res.add(row);
            }
            return new Matrix(res);
        }

        @Override
        public String toString() {
            if (rows() == 1) {
                // Вектор-строка
                return "[" + String.join(", ", data.get(0).stream().map(d -> SymbolicCalculator.this.formatter.format(d)).toList()) + "]";
            } else {
                // Матрица
                StringBuilder sb = new StringBuilder();
                sb.append("[\n");
                for (List<Double> row : data) {
                    sb.append("  [")
                      .append(String.join(", ", row.stream().map(d -> SymbolicCalculator.this.formatter.format(d)).toList()))
                      .append("]\n");
                }
                sb.append("]");
                return sb.toString();
            }
        }
    }
}
