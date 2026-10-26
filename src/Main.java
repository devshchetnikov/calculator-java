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
    private final Map<String, Double> context = new HashMap<>();
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
                System.out.print(buffer.length() > 0 ? ">>> " : "sym> ");
                String line = reader.nextLine().trim();
                if (line.isEmpty()) continue;

                for (char ch : line.toCharArray()) {
                    if (ch == '(') balance++;
                    if (ch == ')') balance--;
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
                    double value = engine.resolve(parser.parse(input), this::fetch);
                    if (Double.isInfinite(value)) throw new IllegalStateException("Magnitude overflow");
                    String out = formatter.format(value);
                    System.out.println("=> " + out);
                    commandLog.add(input + " = " + out);
                    lastResult = value;
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

        double val = engine.resolve(parser.parse(expr), this::fetch);
        context.put(key, val);
        String out = formatter.format(val);
        System.out.println(key + " -> " + out);
        commandLog.add(input + " (Result: " + out + ")");
        lastResult = val;
    }

    private Double fetch(String token) {
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
        context.forEach((k, v) -> System.out.println(k + " = " + formatter.format(v)));
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
                "sin", "cos", "tan", "sqrt", "log", "log10", "ln", "min", "max", "abs", "asin", "acos", "atan", "sinh", "cosh", "tanh"
        );

        List<String> ingest(String source) {
            List<String> raw = new ArrayList<>();
            int pos = 0;
            while (pos < source.length()) {
                char ch = source.charAt(pos);
                if (Character.isWhitespace(ch)) { pos++; continue; }
                if (Character.isDigit(ch) || ch == '.') {
                    StringBuilder sb = new StringBuilder();
                    // Захват унарного минуса перед числом
                    if (ch == '-' && (pos == 0 || "+-*/^%!(),".indexOf(source.charAt(pos - 1)) != -1)) {
                        sb.append(ch);
                        pos++;
                        if (pos >= source.length() || !(Character.isDigit(source.charAt(pos)) || source.charAt(pos) == '.')) {
                            throw new IllegalArgumentException("Malformed unary minus");
                        }
                    }
                    // Захват самого числа
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
                if (isNumeric(t) || isIdentifier(t)) { output.add(t); expectUnary = false; continue; }
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
                "sin", v -> Math.sin(v), "cos", v -> Math.cos(v), "tan", v -> Math.tan(v),
                "asin", v -> Math.asin(v), "acos", v -> Math.acos(v), "atan", v -> Math.atan(v),
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

        String getBanner() {
            return """
                   === Symbolic Engine ===
                   Enter expressions. Commands: help, history, clear, mode, vars, exit, del <id>, reset ans
                   """;
        }

        double resolve(List<String> rpn, Function<String, Double> lookup) {
            Deque<Double> stack = new ArrayDeque<>();
            for (String t : rpn) {
                if (t.startsWith("u-") && isNumeric(t.substring(2))) {
                    stack.push(-Double.parseDouble(t.substring(2))); continue;
                }
                if (isNumeric(t)) { stack.push(Double.parseDouble(t)); continue; }
                if (isIdentifier(t)) { 
                    Double val = lookup.apply(t); 
                    if (val == null) throw new IllegalArgumentException("Unknown: " + t); 
                    stack.push(val); 
                    continue; 
                }

                if (t.equals("u-")) { stack.push(-stack.pop()); continue; }
                if (t.endsWith("!")) {
                    String num = t.substring(0, t.length() - 1);
                    if (!isNumeric(num)) throw new IllegalArgumentException("Factorial on non-numeric");
                    double v = Double.parseDouble(num);
                    if (v < 0 || v != Math.floor(v)) throw new IllegalArgumentException("Factorial domain error");
                    if (v > 20) throw new IllegalArgumentException("Factorial overflow");
                    stack.push(factorial((int) v)); continue;
                }
                if (t.equals("min") || t.equals("max")) {
                    if (stack.size() < 2) throw new IllegalArgumentException(t + " needs two operands");
                    double b = stack.pop(), a = stack.pop();
                    stack.push(t.equals("min") ? Math.min(a, b) : Math.max(a, b)); continue;
                }
                if (UNARY.containsKey(t)) {
                    if (stack.isEmpty()) throw new IllegalArgumentException(t + " needs an argument");
                    double v = stack.pop();
                    Double res = UNARY.get(t).apply(v);
                    if (Double.isInfinite(res)) throw new IllegalArgumentException("Overflow in " + t);
                    stack.push(res); continue;
                }
                if (stack.size() < 2) throw new IllegalArgumentException("Malformed RPN");
                double b = stack.pop(), a = stack.pop();
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
            }
            if (stack.size() != 1) throw new IllegalArgumentException("Invalid expression");
            return stack.pop();
        }

        private boolean isNumeric(String s) {
            try { Double.parseDouble(s); return true; } catch (NumberFormatException e) { return false; }
        }
        private boolean isIdentifier(String s) {
            return s.matches("[a-z]+");
        }

        private double factorial(int n) {
            BigInteger acc = BigInteger.ONE;
            for (int i = 2; i <= n; i++) acc = acc.multiply(BigInteger.valueOf(i));
            return acc.doubleValue();
        }
    }
}
