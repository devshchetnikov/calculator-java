import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Map;

public class SymbolicCalculator {

    private final Deque<String> commandLog = new ArrayDeque<>();
    private final Map<String, Object> context = Map.of("pi", Math.PI, "e", Math.E, "tau", 2 * Math.PI);
    private boolean angularUnitIsDegrees = true;
    private double lastResult = 0.0;

    private static final String CMD_EXIT = "exit", CMD_HELP = "help", CMD_HISTORY = "history", CMD_CLEAR = "clear";
    private static final String CMD_MODE = "mode", CMD_VARS = "vars", CMD_DEL = "del", CMD_RESET = "reset ans";

    public static void main(String[] args) { new SymbolicCalculator().repl(); }

    private void repl() {
        System.out.println(Executor.getBanner());
        try (var reader = new java.util.Scanner(System.in)) {
            while (true) {
                System.out.print(String.format("sym [%s]> ", angularUnitIsDegrees ? "DEG" : "RAD"));
                String line = reader.nextLine().trim();
                if (line.isEmpty()) continue;

                switch (line.toLowerCase()) {
                    case CMD_EXIT -> { return; }
                    case CMD_HELP -> System.out.println(Executor.getBanner());
                    case CMD_HISTORY -> printLog();
                    case CMD_CLEAR -> purgeState();
                    case "cls", "clear screen" -> clearScreen();
                    case CMD_MODE -> toggleMode();
                    case CMD_VARS -> printContext();
                    default -> {
                        if (line.startsWith(CMD_DEL + " ")) {
                            purgeBinding(line.substring(CMD_DEL.length() + 1).trim().toLowerCase());
                        } else if (line.equalsIgnoreCase(CMD_RESET)) {
                            lastResult = 0.0;
                            System.out.println("ans -> 0");
                        } else if (line.contains("=")) {
                            bind(line);
                        } else {
                            try {
                                var value = Executor.resolve(Executor.parse(line), this::fetch);
                                if (value instanceof Double d) {
                                    logResult(line, d);
                                } else if (value instanceof Matrix m) {
                                    System.out.println("=> " + m);
                                    commandLog.add(line + " = " + m);
                                }
                            } catch (Exception ex) {
                                System.err.println("! " + ex.getMessage());
                            }
                        }
                    }
                }
            }
        }
    }

    private void bind(String input) {
        int idx = input.indexOf('=');
        String key = input.substring(0, idx).trim().toLowerCase();
        String expr = input.substring(idx + 1).trim();

        if (!key.matches("[a-z]+") || context.containsKey(key)) {
            System.err.println("! Invalid or protected identifier"); return;
        }
        try {
            Object val = Executor.resolve(Executor.parse(expr), this::fetch);
            if (val instanceof Double d) {
                context.put(key, d);
                logResult(key + " = " + expr, d);
            } else if (val instanceof Matrix m) {
                context.put(key, m);
                System.out.println(key + " -> " + m);
                commandLog.add(input + " (Result: " + m + ")");
            }
        } catch (Exception ex) {
            System.err.println("! " + ex.getMessage());
        }
    }

    private Object fetch(String token) {
        return switch (token) {
            case "ans" -> lastResult;
            default -> context.getOrDefault(token, null);
        };
    }

    private void toggleMode() {
        angularUnitIsDegrees = !angularUnitIsDegrees;
        System.out.println("Trig mode: " + (angularUnitIsDegrees ? "DEG" : "RAD"));
    }

    private void purgeState() {
        commandLog.clear();
        lastResult = 0.0;
        System.out.println("State flushed.");
    }

    private void purgeBinding(String name) {
        if (context.containsKey(name)) {
            context.remove(name);
            System.out.println("'" + name + "' evicted.");
            commandLog.add(CMD_DEL + " " + name);
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
            if (v instanceof Double d) System.out.println(k + " = " + formatDouble(d));
            else if (v instanceof Matrix m) System.out.println(k + " = " + m);
        });
    }

    private void logResult(String expr, double d) {
        if (Double.isInfinite(d)) throw new IllegalStateException("Magnitude overflow");
        String out = formatDouble(d);
        System.out.println("=> " + out);
        commandLog.add(expr + " = " + out);
        lastResult = d;
    }

    private static String formatDouble(double v) {
        if (Double.isNaN(v)) return "NaN";
        if (Double.isInfinite(v)) return "Infinity";
        return String.format("%.15f", v).replaceAll("\\.?0+$", "");
    }

    private static void clearScreen() {
        System.out.print("\033[H\033[2J");
        System.out.flush();
    }

    private static class Executor {
        private static final Map<String, Double> UNARY = Map.of(
            "sin", v -> Math.sin(v), "cos", v -> Math.cos(v), "tan", v -> Math.tan(v),
            "asin", v -> Math.asin(v), "acos", v -> Math.acos(v), "atan", v -> Math.atan(v),
            "sinh", v -> Math.sinh(v), "cosh", v -> Math.cosh(v), "tanh", v -> Math.tanh(v),
            "sqrt", v -> Math.sqrt(v), "ln", v -> Math.log(v), "log10", v -> Math.log10(v), "abs", v -> Math.abs(v)
        );
        private static final Map<String, Double> BINARY = Map.of(
            "+", (a, b) -> a + b, "-", (a, b) -> a - b, "*", (a, b) -> a * b,
            "/", (a, b) -> { if (b == 0) throw new IllegalArgumentException("Division by zero"); return a / b; },
            "//", (a, b) -> { if (b == 0) throw new IllegalArgumentException("Division by zero"); return (double) ((long) a / (long) b); },
            "%", (a, b) -> { if (b == 0) throw new IllegalArgumentException("Division by zero"); return ((a % b) + Math.abs(b)) % Math.abs(b); },
            "^", Math::pow
        );

        static String getBanner() {
            return """
                   === Symbolic Engine v2.1 (Compact) ===
                   Enter expressions. Commands: help, history, clear, mode, vars, exit, del <id>, reset ans, cls
                   Vectors: [1, 2, 3]. Matrices: [[1, 2], [3, 4]].
                   """;
        }

        static List<String> parse(String source) {
            var tokens = new java.util.ArrayList<String>();
            for (int i = 0; i < source.length(); i++) {
                char ch = source.charAt(i);
                if (Character.isWhitespace(ch)) continue;
                if ("+-*/^%!(),[]".indexOf(ch) != -1) {
                    if (ch == '/' && i + 1 < source.length() && source.charAt(i + 1) == '/') { tokens.add("//"); i++; }
                    else tokens.add(String.valueOf(ch));
                    continue;
                }
                if (Character.isDigit(ch) || ch == '.') {
                    var sb = new StringBuilder();
                    if (ch == '-' && (i == 0 || "+-*/^%!(),[".indexOf(source.charAt(i - 1)) != -1)) sb.append(ch);
                    while (i < source.length() && (Character.isDigit(source.charAt(i)) || source.charAt(i) == '.')) sb.append(source.charAt(i++));
                    tokens.add(sb.toString()); i--;
                    continue;
                }
                if (Character.isLetter(ch)) {
                    var sb = new StringBuilder();
                    while (i < source.length() && Character.isLetterOrDigit(source.charAt(i))) sb.append(source.charAt(i++));
                    tokens.add(sb.toString().toLowerCase()); i--;
                    continue;
                }
                throw new IllegalArgumentException("Unknown glyph: " + ch);
            }
            return tokens;
        }

        static Object resolve(List<String> rpn, java.util.function.Function<String, Object> lookup) {
            Deque<Object> stack = new ArrayDeque<>();
            for (String t : rpn) {
                if (t.equals("u-")) { Object a = stack.pop(); stack.push(a instanceof Double d ? -d : ((Matrix) a).scale(-1)); continue; }
                if (t.equals("ans")) { stack.push(lookup.apply(t)); continue; }
                if (UNARY.containsKey(t)) { Object a = stack.pop(); stack.push(UNARY.get(t).apply((Double) a)); continue; }
                if (t.equals("fact")) { Object a = stack.pop(); double v = (Double) a; if (v < 0 || v != Math.floor(v)) throw new IllegalArgumentException("Factorial domain error"); stack.push(factorial((int) v)); continue; }
                if (t.equals("gcd") || t.equals("lcm")) { Object b = stack.pop(), a = stack.pop(); long ai = (long)(Double)a, bi = (long)(Double)b; stack.push(t.equals("gcd") ? (double) gcd(ai, bi) : (double) lcm(ai, bi)); continue; }
                if (t.equals("det")) { Object a = stack.pop(); stack.push(((Matrix) a).determinant()); continue; }
                if (t.equals("transpose")) { Object a = stack.pop(); stack.push(((Matrix) a).transpose()); continue; }
                if (t.equals("cross") || t.equals("x")) { Object b = stack.pop(), a = stack.pop(); stack.push(((Matrix) a).cross((Matrix) b)); continue; }
                if (t.equals("norm")) { Object a = stack.pop(); stack.push(((Matrix) a).norm()); continue; }

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
                        case "+" -> ma.add(mb); case "-" -> ma.subtract(mb); case "*" -> ma.multiply(mb); case "." -> dot(ma, mb);
                        default -> throw new IllegalArgumentException("Matrix op: " + t);
                    };
                }
                if (aObj instanceof Double k && bObj instanceof Matrix m) { stack.push(m.scale(k)); continue; }
                if (aObj instanceof Matrix m && bObj instanceof Double k) { stack.push(m.scale(k)); continue; }
                throw new IllegalArgumentException("Type mismatch for operator: " + t);
            }
            return stack.pop();
        }

        private static double factorial(int n) { long acc = 1; for (int i = 2; i <= n; i++) acc *= i; return (double) acc; }
        private static long gcd(long a, long b) { while (b != 0) { long t = b; b = a % b; a = t; } return a; }
        private static long lcm(long a, long b) { return a == 0 || b == 0 ? 0 : Math.abs(a / gcd(a, b) * b); }
        private static double dot(Matrix a, Matrix b) { double sum = 0; for (int i = 0; i < a.cols(); i++) sum += a.get(0, i) * b.get(0, i); return sum; }
    }

    private static class Matrix {
        private final List<List<Double>> data;
        Matrix(List<List<Double>> data) { if (data.isEmpty() || data.get(0).isEmpty()) throw new IllegalArgumentException("Empty matrix"); int cols = data.get(0).size(); for (List<Double> r : data) if (r.size() != cols) throw new IllegalArgumentException("Jagged matrix"); this.data = data; }
        int rows() { return data.size(); } int cols() { return data.get(0).size(); }
        double get(int r, int c) { return data.get(r).get(c); }
        Matrix add(Matrix o) { List<List<Double>> res = new java.util.ArrayList<>(); for (int i = 0; i < rows(); i++) { List<Double> row = new java.util.ArrayList<>(); for (int j = 0; j < cols(); j++) row.add(data.get(i).get(j) + o.data.get(i).get(j)); res.add(row); } return new Matrix(res); }
        Matrix subtract(Matrix o) { List<List<Double>> res = new java.util.ArrayList<>(); for (int i = 0; i < rows(); i++) { List<Double> row = new java.util.ArrayList<>(); for (int j = 0; j < cols(); j++) row.add(data.get(i).get(j) - o.data.get(i).get(j)); res.add(row); } return new Matrix(res); }
        Matrix scale(double k) { List<List<Double>> res = new java.util.ArrayList<>(); for (int i = 0; i < rows(); i++) { List<Double> row = new java.util.ArrayList<>(); for (int j = 0; j < cols(); j++) row.add(data.get(i).get(j) * k); res.add(row); } return new Matrix(res); }
        Matrix multiply(Matrix o) { List<List<Double>> res = new java.util.ArrayList<>(); for (int i = 0; i < rows(); i++) { List<Double> row = new java.util.ArrayList<>(); for (int j = 0; j < o.cols(); j++) { double sum = 0; for (int k = 0; k < cols(); k++) sum += data.get(i).get(k) * o.data.get(k).get(j); row.add(sum); } res.add(row); } return new Matrix(res); }
        Matrix transpose() { List<List<Double>> res = new java.util.ArrayList<>(); for (int j = 0; j < cols(); j++) { List<Double> col = new java.util.ArrayList<>(); for (int i = 0; i < rows(); i++) col.add(data.get(i).get(j)); res.add(col); } return new Matrix(res); }
        Matrix cross(Matrix o) { return new Matrix(List.of(List.of(data.get(0).get(1) * o.data.get(0).get(2) - data.get(0).get(2) * o.data.get(0).get(1), data.get(0).get(2) * o.data.get(0).get(0) - data.get(0).get(0) * o.data.get(0).get(2), data.get(0).get(0) * o.data.get(0).get(1) - data.get(0).get(1) * o.data.get(0).get(0)))); }
        double norm() { double sum = 0; for (int i = 0; i < cols(); i++) sum += Math.pow(get(0, i), 2); return Math.sqrt(sum); }
        Double determinant() {
            if (rows() != cols()) throw new IllegalArgumentException("Determinant is defined only for square matrices");
            if (rows() == 1) return data.get(0).get(0);
            if (rows() == 2) return data.get(0).get(0) * data.get(1).get(1) - data.get(0).get(1) * data.get(1).get(0);
            if (rows() == 3) {
                double a = data.get(0).get(0), b = data.get(0).get(1), c = data.get(0).get(2);
                double d = data.get(1).get(0), e = data.get(1).get(1), f = data.get(1).get(2);
                double g = data.get(2).get(0), h = data.get(2).get(1), i = data.get(2).get(2);
                return a * (e * i - f * h) - b * (d * i - f * g) + c * (d * h - e * g);
            }
            double det = 0; for (int c = 0; c < cols(); c++) { double sign = (c % 2 == 0) ? 1 : -1; det += sign * data.get(0).get(c) * minor(0, c).determinant(); } return det;
        }
        private Matrix minor(int r, int c) { List<List<Double>> res = new java.util.ArrayList<>(); for (int i = 0; i < rows(); i++) { if (i == r) continue; List<Double> row = new java.util.ArrayList<>(); for (int j = 0; j < cols(); j++) { if (j == c) continue; row.add(data.get(i).get(j)); } res.add(row); } return new Matrix(res); }
        @Override public String toString() { if (rows() == 1) return "[" + String.join(", ", data.get(0()).stream().map(v -> formatDouble(v)).toList()) + "]"; var sb = new StringBuilder(); sb.append("[\n"); for (List<Double> row : data) sb.append("  [").append(String.join(", ", row.stream().map(v -> formatDouble(v)).toList())).append("]\n"); sb.append("]"); return sb.toString(); }
    }
}
