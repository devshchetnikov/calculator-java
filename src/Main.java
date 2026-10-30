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

import static java.lang.Math.toRadians;

public class SymbolicCalculator {

    private final Deque<String> commandLog = new ArrayDeque<>();
    private final Map<String, Object> context = new HashMap<>();
    private final NumberFormatter formatter = new NumberFormatter();
    private final ExpressionParser parser = new ExpressionParser();
    private final Executor engine = new Executor();

    private boolean angularUnitIsDegrees = true;
    private double lastResult = 0.0;

    private static final Set<String> RESERVED = Set.of("pi", "e", "tau", "ans");

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
                        System.out.println("=> " + m);
                        commandLog.add(input + " = " + m);
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
                System.out.println(key + " -> " + m);
                commandLog.add(input + " (Result: " + m + ")");
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
                System.out.println(k + " = " + m);
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
                "+", 1, "-", 1, "*", 2, "/", 2, "%", 2, "//", 2, "^", 3, "u-", 4, "!", 5
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
                if (ch == '.') { raw.add("."); pos++; continue; }

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
            }

            List<String> finalTokens = new ArrayList<>();
            for (String t : tokens) {
                if (t.equals("!") && !finalTokens.isEmpty() && isNumeric(finalTokens.get(finalTokens.size() - 1))) {
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
                "^", Math::pow
        );

        private static double applyTrig(double v, java.util.function.Function<Double, Double> func) {
            return func.apply(v);
        }

        String getBanner() {
            return """
                   === Symbolic Engine v2.1 (Compact) ===
                   Enter expressions. Commands: help, history, clear, mode, vars, exit, del <id>, reset ans, cls
                   Vectors: [1, 2, 3]. Matrices: [[1, 2], [3, 4]].
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
                    else if (a instanceof Matrix m) stack.push(m.scale(-1));
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
                    Object a = stack.pop();
                    if (!(a instanceof Matrix)) throw new IllegalArgumentException("det applies only to matrices");
                    stack.push(((Matrix) a).determinant());
                    continue;
                }
                if (t.equals("transpose")) {
                    Object a = stack.pop();
                    if (!(a instanceof Matrix)) throw new IllegalArgumentException("transpose applies only to matrices");
                    stack.push(((Matrix) a).transpose());
                    continue;
                }
                if (t.equals("cross") || t.equals("x")) {
                    Object bObj = stack.pop(), aObj = stack.pop();
                    if (aObj instanceof Matrix a && bObj instanceof Matrix b) {
                        stack.push(a.cross(b));
                        continue;
                    }
                    throw new IllegalArgumentException("Cross product applies only to 3D vectors");
                }
                if (t.equals("norm")) {
                    Object aObj = stack.pop();
                    if (aObj instanceof Matrix a && (a.rows() == 1 || a.cols() == 1)) {
                        stack.push(a.norm());
                        continue;
                    }
                    throw new IllegalArgumentException("Norm applies only to vectors");
                }
                if (UNARY.containsKey(t)) {
                    Object aObj = stack.pop();
                    if (!(aObj instanceof Double)) throw new IllegalArgumentException(t + " applies only to scalars");
                    double v = (Double) aObj;
                    stack.push(UNARY.get(t).apply(v));
                    continue;
                }
                if (stack.size() < 2) throw new IllegalArgumentException("Malformed RPN");
                Object bObj = stack.pop(), aObj = stack.pop();

                if (aObj instanceof Double a && bObj instanceof Double b) {
                    Double res = switch (t) {
                        case "+", "-", "*", "/", "//", "%", "^" -> BINARY.get(t).apply(a, b);
                        case "." -> a * b;
                        default -> throw new IllegalArgumentException("Unknown operator: " + t);
                    };
                    if (Double.isInfinite(res)) throw new IllegalArgumentException("Overflow in " + t);
                    stack.push(res);
                    continue;
                }

                if (aObj instanceof Matrix ma && bObj instanceof Matrix mb) {
                    return switch (t) {
                        case "+" -> ma.add(mb);
                        case "-" -> ma.subtract(mb);
                        case "*" -> ma.multiply(mb);
                        case "." -> {
                            if (!ma.isVector() || !mb.isVector() || ma.cols() != mb.cols()) {
                                throw new IllegalArgumentException("Dot product requires vectors of same size");
                            }
                            double sum = 0;
                            for (int i = 0; i < ma.cols(); i++) {
                                sum += ma.get(0, i) * mb.get(0, i);
                            }
                            yield sum;
                        }
                        default -> throw new IllegalArgumentException("Matrix op: " + t);
                    };
                }

                if (aObj instanceof Double k && bObj instanceof Matrix m) {
                    if (t.equals("*")) stack.push(m.scale(k));
                    else throw new IllegalArgumentException("Matrix-scalar op: " + t);
                    continue;
                }
                if (aObj instanceof Matrix m && bObj instanceof Double k) {
                    if (t.equals("*")) stack.push(m.scale(k));
                    else throw new IllegalArgumentException("Matrix-scalar op: " + t);
                    continue;
                }

                throw new IllegalArgumentException("Type mismatch for operator: " + t);
            }
            if (stack.size() != 1) throw new IllegalArgumentException("Invalid expression");
            return stack.pop();
        }

        private double factorial(int n) {
            long acc = 1;
            for (int i = 2; i <= n; i++) acc *= i;
            return (double) acc;
        }
    }

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

        public Matrix cross(Matrix other) {
            if (this.cols() != 3 || other.cols() != 3) {
                throw new IllegalArgumentException("Cross product is defined only for 3D vectors");
            }
            double x1 = this.get(0, 0), y1 = this.get(0, 1), z1 = this.get(0, 2);
            double x2 = other.get(0, 0), y2 = other.get(0, 1), z2 = other.get(0, 2);
            List<List<Double>> res = List.of(
                List.of(y1 * z2 - z1 * y2, z1 * x2 - x1 * z2, x1 * y2 - y1 * x2)
            );
            return new Matrix(res);
        }

        public double norm() {
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
                return "[" + String.join(", ", data.get(0).stream().map(formatter::format).toList()) + "]";
            }
            StringBuilder sb = new StringBuilder();
            sb.append("[\n");
            for (List<Double> row : data) {
                sb.append("  [")
                  .append(String.join(", ", row.stream().map(formatter::format).toList()))
                  .append("]\n");
            }
            sb.append("]");
            return sb.toString();
        }
    }
}
