import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;

public class Calculator {

    // --- NEW CLASS: Fraction (внутри Calculator) ---
    private static class Fraction {
        private final long num;
        private final long denom;

        Fraction(long num, long denom) {
            if (denom == 0) throw new IllegalArgumentException("Division by zero");
            if (denom < 0) { num = -num; denom = -denom; }
            long g = gcd(Math.abs(num), denom);
            this.num = num / g;
            this.denom = denom / g;
        }

        private static Fraction of(long num, long denom) {
            if (denom == 0) throw new IllegalArgumentException("Division by zero");
            if (denom < 0) { num = -num; denom = -denom; }
            long g = gcd(Math.abs(num), denom);
            num /= g;
            denom /= g;
            // Защита от переполнения long
            if (num / g * g != num || denom / g * g != denom) {
                throw new ArithmeticException("Long overflow in fraction arithmetic");
            }
            return new Fraction(num, denom);
        }

        Fraction add(Fraction o) { return of(num * o.denom + o.num * denom, denom * o.denom); }
        Fraction sub(Fraction o) { return of(num * o.denom - o.num * denom, denom * o.denom); }
        Fraction mul(Fraction o) { return of(num * o.num, denom * o.denom); }
        Fraction div(Fraction o) { return of(num * o.denom, denom * o.num); }

        double toDouble() { return (double) num / (double) denom; }

        private static long gcd(long a, long b) {
            while (b != 0) { long t = b; b = a % b; a = t; }
            return a;
        }

        @Override
        public String toString() {
            if (denom == 1) return Long.toString(num);
            if (Math.abs(num) > denom) {
                long whole = num / denom;
                long rest = Math.abs(num % denom);
                return (num < 0 ? "-" : "") + Math.abs(whole) + " " + rest + "/" + denom;
            }
            return num + "/" + denom;
        }
    }

    // --- Calculator State ---
    private Double lastResult = null;
    private final List<String> commandLog = new ArrayList<>();

    // --- REPL Constants ---
    private static final String CMD_EXIT = "/exit";
    private static final String CMD_HELP = "/help";
    private static final String CMD_RESET = "/reset";
    private static final String CMD_LOG = "/log";
    private static final String CMD_DEL = "/del";

    public static void main(String[] args) {
        new Calculator().repl();
    }

    private void repl() {
        java.util.Scanner scanner = new java.util.Scanner(System.in);
        System.out.println("Calculator REPL. Type /help for commands.");

        while (true) {
            System.out.print("> ");
            String line = scanner.nextLine().trim();

            if (line.isEmpty()) continue;
            if (line.equals(CMD_EXIT)) {
                System.out.println("Bye!");
                break;
            }
            if (line.equals(CMD_HELP)) {
                System.out.println("Enter expressions (e.g. 2 + 2 * 2).");
                System.out.println("Fractions: 1/2 + 3/4");
                System.out.println("Complex: (2+3i) * 2");
                System.out.println("Commands: /exit, /help, /reset, /log, /del <n>, frac <n> <d>, asfrac");
                continue;
            }
            if (line.equals(CMD_RESET)) {
                lastResult = null;
                commandLog.clear();
                System.out.println("State reset.");
                continue;
            }
            if (line.equals(CMD_LOG)) {
                commandLog.forEach(System.out::println);
                continue;
            }
            if (line.startsWith(CMD_DEL + " ")) {
                try {
                    int n = Integer.parseInt(line.substring(CMD_DEL.length() + 1).trim());
                    if (n > 0 && n <= commandLog.size()) {
                        commandLog.remove(commandLog.size() - n);
                        System.out.println("Deleted log #" + n);
                    } else {
                        System.err.println("! Invalid log index");
                    }
                } catch (Exception ex) {
                    System.err.println("! Usage: /del <number>");
                }
                continue;
            }
            if (line.startsWith("frac ")) {
                String[] parts = line.substring(5).trim().split("\\s+");
                if (parts.length == 2) {
                    try {
                        Fraction f = new Fraction(Long.parseLong(parts[0]), Long.parseLong(parts[1]));
                        logResult("frac " + parts[0] + " " + parts[1], f);
                    } catch (Exception ex) {
                        System.err.println("! " + ex.getMessage());
                    }
                } else {
                    System.err.println("! Usage: frac <numerator> <denominator>");
                }
                continue;
            }
            if (line.equals("asfrac")) {
                if (lastResult instanceof Double d) {
                    // Упрощенный поиск дроби (алгоритм Фарея)
                    long maxDenom = 1_000_000;
                    long a = 0, b = 1, c = 1, dnom = 1;
                    while (dnom <= maxDenom) {
                        long mediantN = a + c;
                        long mediantD = b + dnom;
                        double mediantVal = (double) mediantN / mediantD;
                        if (Math.abs(d - mediantVal) < Math.abs(d - (double) a / b)) {
                            a = mediantN;
                            b = mediantD;
                        } else if (mediantVal < d) {
                            c = mediantN;
                            dnom = mediantD;
                        } else {
                            break;
                        }
                    }
                    Fraction f = new Fraction(a, b);
                    logResult("asfrac", f);
                } else {
                    System.err.println("! No double value to convert");
                }
                continue;
            }

            try {
                Object result = evaluate(line);
                if (result instanceof Double d) logResult(line, d);
                else if (result instanceof Fraction f) logResult(line, f);
                else if (result instanceof String s) logResult(line, s);
            } catch (Exception ex) {
                System.err.println("! " + ex.getMessage());
            }
        }
        scanner.close();
    }

    private void logResult(String expr, Double d) {
        System.out.println("=> " + formatDouble(d));
        commandLog.add(expr + " = " + formatDouble(d));
        lastResult = d;
    }

    private void logResult(String expr, Fraction f) {
        String out = f.toString();
        System.out.println("=> " + out);
        commandLog.add(expr + " = " + out);
        lastResult = f.toDouble();
    }

    private void logResult(String expr, String s) {
        System.out.println("=> " + s);
        commandLog.add(expr + " = " + s);
        lastResult = null;
    }

    private String formatDouble(Double d) {
        if (d == null) return "null";
        long rounded = Math.round(d);
        return Math.abs(d - rounded) < 1e-10 ? Long.toString(rounded) : d.toString();
    }

    private Object evaluate(String input) {
        String[] tokens = tokenize(input);
        if (tokens.length == 0) throw new IllegalArgumentException("Empty expression");

        // Single token (number, fraction, complex or 'ans')
        if (tokens.length == 1) {
            return fetch(tokens[0]);
        }

        // Shunting-yard algorithm
        Deque<String> output = new ArrayDeque<>();
        Deque<String> ops = new ArrayDeque<>();

        for (String t : tokens) {
            if (t.matches("-?\\d+(\\.\\d+)?") || t.matches("-?\\d+/\\d+") || t.matches("[a-zA-Z]+") || t.contains("i")) {
                output.add(t);
            } else if (t.equals("(")) {
                ops.push(t);
            } else if (t.equals(")")) {
                while (!ops.isEmpty() && !ops.peek().equals("(")) {
                    output.add(ops.pop());
                }
                if (ops.isEmpty()) throw new IllegalArgumentException("Mismatched parentheses");
                ops.pop(); // pop '('
            } else {
                // Operator
                while (!ops.isEmpty() && precedence(ops.peek()) >= precedence(t) && !ops.peek().equals("(")) {
                    output.add(ops.pop());
                }
                ops.push(t);
            }
        }
        while (!ops.isEmpty()) {
            if (ops.peek().equals("(") || ops.peek().equals(")")) throw new IllegalArgumentException("Mismatched parentheses");
            output.add(ops.pop());
        }

        // RPN Evaluation
        Deque<Object> stack = new ArrayDeque<>();
        for (String t : output) {
            if (t.matches("-?\\d+(\\.\\d+)?") || t.matches("-?\\d+/\\d+") || t.matches("[a-zA-Z]+") || t.contains("i")) {
                stack.push(fetch(t));
            } else {
                if (stack.size() < 2) throw new IllegalArgumentException("Invalid expression");
                Object bObj = stack.pop();
                Object aObj = stack.pop();
                stack.push(applyOp(aObj, bObj, t));
            }
        }
        if (stack.size() != 1) throw new IllegalArgumentException("Invalid expression");
        return stack.pop();
    }

    private Object fetch(String t) {
        return switch (t) {
            case "ans" -> lastResult != null ? lastResult : 0.0;
            case "pi" -> Math.PI;
            case "e" -> Math.E;
            default -> {
                if (t.matches("-?\\d+/\\d+")) {
                    String[] parts = t.split("/");
                    yield new Fraction(Long.parseLong(parts[0]), Long.parseLong(parts[1]));
                } else if (t.matches("-?\\d+(\\.\\d+)?")) {
                    yield Double.parseDouble(t);
                } else {
                    yield t; // variable or complex string
                }
            }
        };
    }

    private Object applyOp(Object aObj, Object bObj, String t) {
        // Fraction + Fraction
        if (aObj instanceof Fraction fa && bObj instanceof Fraction fb) {
            return switch (t) {
                case "+" -> fa.add(fb);
                case "-" -> fa.sub(fb);
                case "*" -> fa.mul(fb);
                case "/" -> fa.div(fb);
                case "^" -> Math.pow(fa.toDouble(), fb.toDouble());
                default -> throw new IllegalArgumentException("Unknown operator for fraction: " + t);
            };
        }

        // Mixed Fraction and Double
        if (aObj instanceof Double a) {
            if (bObj instanceof Fraction fb) {
                return switch (t) {
                    case "+" -> a + fb.toDouble();
                    case "-" -> a - fb.toDouble();
                    case "*" -> a * fb.toDouble();
                    case "/" -> a / fb.toDouble();
                    case "^" -> Math.pow(a, fb.toDouble());
                    default -> throw new IllegalArgumentException("Mixed op (Double/Fraction): " + t);
                };
            }
            if (bObj instanceof Double b) {
                return switch (t) {
                    case "+" -> a + b;
                    case "-" -> a - b;
                    case "*" -> a * b;
                    case "/" -> a / b;
                    case "^" -> Math.pow(a, b);
                    default -> throw new IllegalArgumentException("Unknown operator: " + t);
                };
            }
        }
        if (aObj instanceof Fraction fa && bObj instanceof Double b) {
            return switch (t) {
                case "+" -> fa.toDouble() + b;
                case "-" -> fa.toDouble() - b;
                case "*" -> fa.toDouble() * b;
                case "/" -> fa.toDouble() / b;
                case "^" -> Math.pow(fa.toDouble(), b);
                default -> throw new IllegalArgumentException("Mixed op (Fraction/Double): " + t);
            };
        }

        // Complex number string arithmetic (very basic support)
        if (aObj instanceof String sa && bObj instanceof String sb) {
            if (sa.contains("i") && sb.contains("i")) {
                return "Complex string arithmetic not supported. Use parentheses.";
            }
            if (sa.contains("i")) {
                try {
                    double real = Double.parseDouble(sb);
                    return parseComplexCalc(sa, real, t);
                } catch (Exception ignored) {}
            }
            if (sb.contains("i")) {
                try {
                    double real = Double.parseDouble(sa);
                    return parseComplexCalc(sb, real, t, true);
                } catch (Exception ignored) {}
            }
        }
        if (aObj instanceof String s && bObj instanceof Double d) {
            if (s.contains("i")) return parseComplexCalc(s, d, t);
        }
        if (aObj instanceof Double d && bObj instanceof String s) {
            if (s.contains("i")) return parseComplexCalc(s, d, t, true);
        }

        throw new IllegalArgumentException("Unsupported types: " + aObj.getClass() + " " + bObj.getClass());
    }

    // Helper for (Complex String) <op> Double
    private String parseComplexCalc(String comp, double real, String op) {
        return parseComplexCalc(comp, real, op, false);
    }
    private String parseComplexCalc(String comp, double real, String op, boolean reverse) {
        String[] parts = comp.replace("i", "").split("(?=[+-])");
        double realPart = 0, imagPart = 0;
        for (String p : parts) {
            if (p.isEmpty()) continue;
            if (p.equals("+") || p.equals("-")) p = p + "1";
            if (p.endsWith("+") || p.endsWith("-")) p = p + "1";
            if (p.contains("i")) {
                imagPart += Double.parseDouble(p.replace("i", ""));
            } else {
                realPart += Double.parseDouble(p);
            }
        }
        if (reverse) { double tmp = realPart; realPart = real; real = tmp; }

        double resR = 0, resI = 0;
        switch (op) {
            case "+" -> { resR = realPart + real; resI = imagPart; }
            case "-" -> { resR = realPart - real; resI = imagPart; }
            case "*" -> { resR = realPart * real; resI = imagPart * real; }
            case "/" -> { resR = realPart / real; resI = imagPart / real; }
            default -> throw new IllegalArgumentException("Unsupported op for complex: " + op);
        }
        return formatComplex(resR, resI);
    }

    private String formatComplex(double re, double im) {
        StringBuilder sb = new StringBuilder();
        if (Math.abs(re) > 1e-9) sb.append(formatDouble(re));
        if (Math.abs(im) > 1e-9) {
            if (sb.length() > 0 && im > 0) sb.append("+");
            if (Math.abs(im - 1.0) < 1e-9) {
                sb.append("i");
            } else if (Math.abs(im + 1.0) < 1e-9) {
                sb.append("-i");
            } else {
                sb.append(formatDouble(im)).append("i");
            }
        }
        if (sb.length() == 0) return "0";
        return sb.toString();
    }

    private String[] tokenize(String input) {
        String spaced = input.replaceAll("([()+\\-*/^])", " $1 ");
        return spaced.trim().split("\\s+");
    }

    private int precedence(String op) {
        return switch (op) {
            case "^" -> 4;
            case "*", "/" -> 3;
            case "+", "-" -> 2;
            default -> 0;
        };
    }
}
