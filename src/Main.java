import java.util.*;

public class Calculator {
    // --- Fraction ---
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

        private static long gcd(long a, long b) {
            while (b != 0) { long t = b; b = a % b; a = t; }
            return a;
        }
    }

    // --- Complex ---
    private static class Complex {
        private final double re;
        private final double im;

        Complex(double re, double im) {
            this.re = re;
            this.im = im;
        }

        Complex add(Complex o) { return new Complex(re + o.re, im + o.im); }
        Complex sub(Complex o) { return new Complex(re - o.re, im - o.im); }
        Complex mul(Complex o) { return new Complex(re * o.re - im * o.im, re * o.im + im * o.re); }
        Complex div(Complex o) {
            double denom = o.re * o.re + o.im * o.im;
            return new Complex((re * o.re + im * o.im) / denom, (im * o.re - re * o.im) / denom);
        }

        Complex pow(double exponent) {
            double r = Math.hypot(re, im);
            double theta = Math.atan2(im, re);
            double newR = Math.pow(r, exponent);
            return new Complex(newR * Math.cos(exponent * theta), newR * Math.sin(exponent * theta));
        }

        @Override
        public String toString() {
            StringBuilder sb = new StringBuilder();
            if (Math.abs(re) > 1e-9) sb.append(formatDouble(re));
            if (Math.abs(im) > 1e-9) {
                if (sb.length() > 0 && im >= 0) sb.append("+");
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
    }

    // --- Vector ---
    private static class Vector {
        private final double[] v;
        private final int dim;

        Vector(double[] data) {
            if (data.length == 0) {
                throw new IllegalArgumentException("Vector dimension cannot be zero");
            }
            this.dim = data.length;
            this.v = data.clone();
        }

        int getDim() { return dim; }

        Vector add(Vector o) {
            if (this.dim != o.dim) throw new IllegalArgumentException("Vector dimensions must match");
            double[] res = new double[dim];
            for (int i = 0; i < dim; i++) {
                res[i] = this.v[i] + o.v[i];
            }
            return new Vector(res);
        }

        Vector sub(Vector o) {
            if (this.dim != o.dim) throw new IllegalArgumentException("Vector dimensions must match");
            double[] res = new double[dim];
            for (int i = 0; i < dim; i++) {
                res[i] = this.v[i] - o.v[i];
            }
            return new Vector(res);
        }

        Vector mul(double scalar) {
            double[] res = new double[dim];
            for (int i = 0; i < dim; i++) {
                res[i] = this.v[i] * scalar;
            }
            return new Vector(res);
        }

        double dot(Vector o) {
            if (this.dim != o.dim) throw new IllegalArgumentException("Vector dimensions must match");
            double sum = 0;
            for (int i = 0; i < dim; i++) {
                sum += this.v[i] * o.v[i];
            }
            return sum;
        }

        Vector cross(Vector o) {
            if (this.dim != 3 || o.dim != 3) {
                throw new IllegalArgumentException("Cross product is defined only for 3D vectors");
            }
            return new Vector(new double[]{
                this.v[1] * o.v[2] - this.v[2] * o.v[1],
                this.v[2] * o.v[0] - this.v[0] * o.v[2],
                this.v[0] * o.v[1] - this.v[1] * o.v[0]
            });
        }

        double norm() {
            double sum = 0;
            for (double val : v) {
                sum += val * val;
            }
            return Math.sqrt(sum);
        }

        // --- NEW: Vector projection ---
        Vector projectOnto(Vector o) {
            double denom = o.dot(o);
            if (Math.abs(denom) < 1e-12) {
                throw new ArithmeticException("Cannot project onto a zero vector");
            }
            double scale = this.dot(o) / denom;
            return o.mul(scale);
        }

        @Override
        public String toString() {
            StringBuilder sb = new StringBuilder();
            sb.append("[");
            for (int i = 0; i < dim; i++) {
                sb.append(formatDouble(v[i]));
                if (i < dim - 1) sb.append(", ");
            }
            sb.append("]");
            return sb.toString();
        }
    }

    // --- Matrix (NxM) ---
    private static class Matrix {
        private final double[][] m;
        private final int rows;
        private final int cols;

        Matrix(double[][] data) {
            if (data.length == 0 || data[0].length == 0) {
                throw new IllegalArgumentException("Matrix dimensions cannot be zero");
            }
            this.rows = data.length;
            this.cols = data[0].length;
            for (double[] row : data) {
                if (row.length != this.cols) {
                    throw new IllegalArgumentException("All rows must have the same number of columns");
                }
            }
            this.m = data;
        }

        int getRows() { return rows; }
        int getCols() { return cols; }

        double[] getRow(int index) {
            if (index < 0 || index >= rows) throw new IndexOutOfBoundsException();
            return m[index].clone();
        }

        double[] getCol(int index) {
            if (index < 0 || index >= cols) throw new IndexOutOfBoundsException();
            double[] col = new double[rows];
            for (int i = 0; i < rows; i++) {
                col[i] = m[i][index];
            }
            return col;
        }

        Matrix add(Matrix o) {
            if (this.rows != o.rows || this.cols != o.cols) {
                throw new IllegalArgumentException("Matrix dimensions must match for addition");
            }
            double[][] res = new double[rows][cols];
            for (int i = 0; i < rows; i++) {
                for (int j = 0; j < cols; j++) {
                    checkSumOverflow(m[i][j], o.m[i][j]);
                    res[i][j] = m[i][j] + o.m[i][j];
                }
            }
            return new Matrix(res);
        }

        Matrix sub(Matrix o) {
            if (this.rows != o.rows || this.cols != o.cols) {
                throw new IllegalArgumentException("Matrix dimensions must match for subtraction");
            }
            double[][] res = new double[rows][cols];
            for (int i = 0; i < rows; i++) {
                for (int j = 0; j < cols; j++) {
                    checkSumOverflow(m[i][j], -o.m[i][j]);
                    res[i][j] = m[i][j] - o.m[i][j];
                }
            }
            return new Matrix(res);
        }

        Matrix mul(Matrix o) {
            if (this.cols != o.rows) {
                throw new IllegalArgumentException("A columns must match B rows for multiplication");
            }
            double[][] res = new double[this.rows][o.cols];
            for (int i = 0; i < this.rows; i++) {
                for (int j = 0; j < o.cols; j++) {
                    double sum = 0;
                    for (int k = 0; k < this.cols; k++) {
                        checkMulOverflow(m[i][k], o.m[k][j]);
                        sum += m[i][k] * o.m[k][j];
                    }
                    res[i][j] = sum;
                }
            }
            return new Matrix(res);
        }

        Matrix mul(double scalar) {
            double[][] res = new double[rows][cols];
            for (int i = 0; i < rows; i++) {
                for (int j = 0; j < cols; j++) {
                    checkMulOverflow(m[i][j], scalar);
                    res[i][j] = m[i][j] * scalar;
                }
            }
            return new Matrix(res);
        }

        Matrix pow(int exponent) {
            if (rows != cols) {
                throw new IllegalArgumentException("Only square matrices can be raised to a power");
            }
            if (exponent == 0) {
                return identity(rows);
            }
            // --- NEW: Support for negative power (inverse) ---
            if (exponent < 0) {
                return inverse().pow(-exponent);
            }
            Matrix result = identity(rows);
            Matrix base = this;
            int e = exponent;
            while (e > 0) {
                if ((e & 1) == 1) {
                    result = result.mul(base);
                }
                base = base.mul(base);
                e >>= 1;
            }
            return result;
        }

        double det() {
            if (rows != cols) {
                throw new IllegalArgumentException("Determinant is defined only for square matrices");
            }
            if (rows == 1) return m[0][0];
            if (rows == 2) return m[0][0] * m[1][1] - m[0][1] * m[1][0];
            
            double detVal = 0;
            for (int c = 0; c < cols; c++) {
                detVal += Math.pow(-1, c) * m[0][c] * minor(0, c).det();
            }
            return detVal;
        }

        // --- NEW: Matrix trace ---
        double trace() {
            if (rows != cols) {
                throw new IllegalArgumentException("Trace is defined only for square matrices");
            }
            double sum = 0;
            for (int i = 0; i < rows; i++) {
                checkSumOverflow(sum, m[i][i]);
                sum += m[i][i];
            }
            return sum;
        }

        Matrix transpose() {
            double[][] res = new double[cols][rows];
            for (int i = 0; i < rows; i++) {
                for (int j = 0; j < cols; j++) {
                    res[j][i] = m[i][j];
                }
            }
            return new Matrix(res);
        }

        Matrix inverse() {
            if (rows != cols) {
                throw new IllegalArgumentException("Only square matrices can be inverted");
            }
            double determinant = det();
            if (Math.abs(determinant) < 1e-12) {
                throw new ArithmeticException("Matrix is singular and cannot be inverted");
            }
            if (rows == 1) {
                return new Matrix(new double[][]{{1.0 / m[0][0]}});
            }
            double[][] inv = new double[rows][cols];
            for (int i = 0; i < rows; i++) {
                for (int j = 0; j < cols; j++) {
                    double sign = Math.pow(-1, i + j);
                    inv[j][i] = sign * minor(i, j).det() / determinant; // Transposed cofactors
                }
            }
            return new Matrix(inv);
        }

        private Matrix minor(int skipRow, int skipCol) {
            double[][] res = new double[rows - 1][cols - 1];
            int r = 0;
            for (int i = 0; i < rows; i++) {
                if (i == skipRow) continue;
                int c = 0;
                for (int j = 0; j < cols; j++) {
                    if (j == skipCol) continue;
                    res[r][c] = m[i][j];
                    c++;
                }
                r++;
            }
            return new Matrix(res);
        }

        private static Matrix identity(int size) {
            double[][] id = new double[size][size];
            for (int i = 0; i < size; i++) {
                id[i][i] = 1.0;
            }
            return new Matrix(id);
        }

        private static void checkMulOverflow(double a, double b) {
            double res = a * b;
            if (Double.isInfinite(res)) {
                throw new ArithmeticException("Overflow in matrix multiplication");
            }
        }

        private static void checkSumOverflow(double a, double b) {
            double res = a + b;
            if (Double.isInfinite(res)) {
                throw new ArithmeticException("Overflow in matrix addition");
            }
        }

        @Override
        public String toString() {
            StringBuilder sb = new StringBuilder();
            sb.append("[\n");
            for (int i = 0; i < rows; i++) {
                sb.append("  [");
                for (int j = 0; j < cols; j++) {
                    sb.append(String.format("%.3f", m[i][j]));
                    if (j < cols - 1) sb.append(", ");
                }
                sb.append("]");
                if (i < rows - 1) sb.append(",");
                sb.append("\n");
            }
            sb.append("]");
            return sb.toString();
        }
    }

    // --- State ---
    private Double lastResult = null;
    private final List<String> commandLog = new ArrayList<>();
    private final Map<String, Object> variables = new HashMap<>();

    // --- REPL Constants ---
    private static final String CMD_EXIT = "/exit";
    private static final String CMD_HELP = "/help";
    private static final String CMD_RESET = "/reset";
    private static final String CMD_LOG = "/log";
    private static final String CMD_LOGR = "/logr"; 
    private static final String CMD_DEL = "/del";
    private static final String CMD_VARS = "/vars";
    // --- NEW: Command for trace ---
    private static final String CMD_TRACE = "/tr";

    private static final Set<String> RESERVED_NAMES = Set.of("ans", "pi", "e", "phi", "gamma");

    public static void main(String[] args) {
        new Calculator().repl();
    }

    private void repl() {
        Scanner scanner = new Scanner(System.in);
        System.out.println("Calculator REPL. Type /help for commands.");

        while (true) {
            System.out.print("> ");
            String line = scanner.nextLine().trim();

            if (line.isEmpty()) continue;
            if (line.equals(CMD_EXIT)) break;
            if (line.equals(CMD_HELP)) {
                System.out.println("Enter expressions: 2+2*2, 1/2+3/4, sin(pi/2)");
                System.out.println("Matrix: [[1,2,3],[4,5,6]] * [[0,1],[1,0],[2,3]], Functions: exp, ln, abs, det, tr");
                System.out.println("Vector: [1,2,3] + [4,5,6], [1,2,3] x [4,5,6] (cross), |v| (norm), v1 proj v2");
                System.out.println("Commands: /exit, /help, /reset, /log, /logr, /del <name|N>, /vars, /tr <matrix>");
                System.out.println("Variables: x = 5, y = ans * 2, M = [[1,2],[3,4]], V = [1,2,3]");
                System.out.println("Matrix power: A ^ 2, A ^ -1 (inverse)");
                continue;
            }

            if (line.startsWith("/")) {
                handleCommand(line);
                continue;
            }

            if (line.contains("=")) {
                handleAssignment(line);
                continue;
            }

            try {
                Object result = evaluate(line);
                logResult(line, result);
            } catch (ArithmeticException | IllegalArgumentException ex) {
                System.err.printf("! Error: %s%n", ex.getMessage());
            }
        }
        scanner.close();
    }

    private void handleCommand(String line) {
        String[] parts = line.split("\\s+", 2);
        String cmd = parts[0];
        String arg = parts.length > 1 ? parts[1] : null;

        switch (cmd) {
            case CMD_LOG -> commandLog.forEach(System.out::println);
            case CMD_LOGR -> {
                List<String> reversed = new ArrayList<>(commandLog);
                Collections.reverse(reversed);
                reversed.forEach(System.out::println);
            }
            case CMD_DEL -> {
                if (arg == null) {
                    if (!commandLog.isEmpty()) {
                        commandLog.remove(commandLog.size() - 1);
                        System.out.println("Last command removed from log.");
                    }
                } else if (arg.matches("-?\\d+")) {
                    int index = Integer.parseInt(arg);
                    if (index > 0 && index <= commandLog.size()) {
                        commandLog.remove(index - 1);
                        System.out.println("Command #" + index + " removed from log.");
                    } else {
                        System.err.println("! Invalid log index");
                    }
                } else {
                    if (variables.containsKey(arg)) {
                        variables.remove(arg);
                        System.out.println("Variable '" + arg + "' deleted.");
                    } else {
                        System.err.println("! Variable '" + arg + "' not found");
                    }
                }
            }
            case CMD_VARS -> {
                if (variables.isEmpty()) {
                    System.out.println("(no user variables)");
                } else {
                    variables.forEach((k, v) -> System.out.println(k + " = " + formatValue(v)));
                }
            }
            case CMD_RESET -> {
                commandLog.clear();
                variables.clear();
                lastResult = null;
                System.out.println("Calculator state reset.");
            }
            // --- NEW: Handle trace command ---
            case CMD_TRACE -> {
                if (arg == null) {
                    System.err.println("! Usage: /tr <matrix_variable>");
                    return;
                }
                Object obj = fetch(arg);
                if (obj instanceof Matrix m) {
                    double t = m.trace();
                    System.out.println("=> " + formatDouble(t));
                    commandLog.add(line + " = " + formatDouble(t));
                    lastResult = t;
                } else {
                    System.err.println("! Argument for /tr must be a square matrix");
                }
            }
            default -> System.err.println("! Unknown command: " + cmd);
        }
    }

    private void handleAssignment(String line) {
        String[] parts = line.split("=", 2);
        String name = parts[0].trim();
        String expr = parts[1].trim();

        if (RESERVED_NAMES.contains(name)) {
            System.err.println("! Cannot assign to reserved name: " + name);
            return;
        }
        if (name.startsWith("/")) {
            System.err.println("! Cannot assign to command: " + name);
            return;
        }
        if (!name.matches("[a-zA-Z_][a-zA-Z0-9_]*")) {
            System.err.println("! Invalid variable name: " + name);
            return;
        }

        try {
            Object value = evaluate(expr);
            variables.put(name, value);
            logResult(line, value);
        } catch (ArithmeticException | IllegalArgumentException ex) {
            System.err.printf("! Error: %s%n", ex.getMessage());
        }
    }

    private void logResult(String expr, Object value) throws ArithmeticException {
        if (value instanceof Double d) {
            checkOverflow(d);
            System.out.println("=> " + formatDouble(d));
            commandLog.add(expr + " = " + formatDouble(d));
            lastResult = d;
        } else if (value instanceof Fraction f) {
            System.out.println("=> " + f);
            commandLog.add(expr + " = " + f);
            lastResult = f.toDouble();
        } else if (value instanceof Complex c) {
            System.out.println("=> " + c);
            commandLog.add(expr + " = " + c);
            lastResult = null;
        } else if (value instanceof Matrix m) {
            System.out.println("=> " + m);
            commandLog.add(expr + " = " + m);
            lastResult = null;
        } else if (value instanceof Vector v) {
            System.out.println("=> " + v);
            commandLog.add(expr + " = " + v);
            lastResult = null;
        } else if (value instanceof String s) {
            System.out.println("=> " + s);
            commandLog.add(expr + " = " + s);
            lastResult = null;
        }
    }

    private void checkOverflow(Double d) throws ArithmeticException {
        if (d == null || !Double.isFinite(d)) {
            throw new ArithmeticException("Overflow or division by zero");
        }
    }

    private String formatValue(Object value) {
        if (value instanceof Double d) return formatDouble(d);
        if (value instanceof Fraction f) return f.toString();
        return value.toString();
    }

    private boolean isFunction(String token) {
        return switch (token) {
            case "sin", "cos", "tan", "ln", "log", "sqrt", "abs", "exp", "det", "tr" -> true;
            default -> false;
        };
    }

    private Object applyFunc(String func, Object arg) {
        double val;
        if (arg instanceof Double d) val = d;
        else if (arg instanceof Fraction f) val = f.toDouble();
        else if (arg instanceof Complex c) {
            if ("abs".equals(func)) return Math.hypot(c.re, c.im);
            throw new IllegalArgumentException("Function not supported for complex numbers");
        } else if (arg instanceof Matrix m) {
            if ("det".equals(func)) return m.det();
            // --- NEW: Function for trace ---
            if ("tr".equals(func)) return m.trace();
            throw new IllegalArgumentException("Function not supported for matrices");
        } else if (arg instanceof Vector v) {
            if ("abs".equals(func) || "norm".equals(func)) return v.norm();
            throw new IllegalArgumentException("Function not supported for vectors");
        } else {
            throw new IllegalArgumentException("Unsupported type for function");
        }

        return switch (func) {
            case "sin" -> Math.sin(val);
            case "cos" -> Math.cos(val);
            case "tan" -> Math.tan(val);
            case "ln" -> Math.log(val);
            case "log" -> Math.log10(val);
            case "sqrt" -> Math.sqrt(val);
            case "abs" -> Math.abs(val);
            case "exp" -> Math.exp(val);
            default -> throw new IllegalArgumentException("Unknown function: " + func);
        };
    }

    private Object fetch(String name) {
        return switch (name) {
            case "ans" -> lastResult != null ? lastResult : 0.0;
            case "pi" -> Math.PI;
            case "e" -> Math.E;
            case "phi" -> (Math.sqrt(5) + 1) / 2; // Золотое сечение
            case "gamma" -> 0.577_215_664_901_532_860_606_512_09; // Эйлера–Маскерони
            default -> {
                if (name.matches("-?\\d+/\\d+")) yield Fraction.of(Long.parseLong(name.split("/")[0]), Long.parseLong(name.split("/")[1]));
                if (name.matches("-?\\d+(?:\\.\\d+)?")) yield Double.parseDouble(name);
                if (name.contains("i") && !name.startsWith("[[")) yield parseComplex(name);
                if (name.startsWith("[[")) yield parseMatrix(name);
                if (name.startsWith("[")) yield parseVector(name);
                if (variables.containsKey(name)) yield variables.get(name);
                yield name;
            }
        };
    }

    private String formatDouble(Double d) {
        return Math.abs(d - Math.round(d)) < 1e-9 ? Long.toString(Math.round(d)) : d.toString();
    }

    private Complex parseComplex(String comp) {
        double realPart = 0, imagPart = 0;
        for (var part : comp.replaceAll("\\s+", "").split("(?=[+-])")) {
            if (!part.endsWith("i")) {
                realPart += Double.parseDouble(part);
                continue;
            }
            var n = part.substring(0, part.length()-1).replaceFirst("^([+-]?)$", "$11");
            imagPart += Double.parseDouble(n);
        }
        return new Complex(realPart, imagPart);
    }

    private Matrix parseMatrix(String input) {
        var inner = input.trim()
            .substring(input.indexOf('[')+1, input.lastIndexOf(']'))
            .replaceAll("\\s+", "");
        var rows = inner.split("],\\[");
        double[][] data = new double[rows.length][];
        
        for (int i = 0; i < rows.length; i++) {
            var cells = rows[i].replaceAll("[\\[\\]]", "").split(",");
            data[i] = Arrays.stream(cells)
                .mapToDouble(Double::parseDouble)
                .toArray();
            if (i > 0 && data[i].length != data[0].length) {
                throw new IllegalArgumentException("Invalid matrix: inconsistent column count");
            }
        }
        return new Matrix(data);
    }

    private Vector parseVector(String input) {
        var inner = input.trim()
            .substring(input.indexOf('[')+1, input.lastIndexOf(']'))
            .replaceAll("\\s+", "");
        if (inner.isEmpty()) throw new IllegalArgumentException("Vector cannot be empty");
        var cells = inner.split(",");
        double[] data = new double[cells.length];
        for (int i = 0; i < cells.length; i++) {
            data[i] = Double.parseDouble(cells[i]);
        }
        return new Vector(data);
    }

    private String[] tokenize(String input) {
        String protectedInput = input.trim()
            .replaceAll("(?<=^)-", "NEG")
            .replaceAll("(?<=[\\(\\+\\-*/^])-(?=\\d)", "NEG");

        return protectedInput
            .replaceAll("([()+\\-*/^])", " $1 ")
            .replace(" NEG ", " - ")
            .split("\\s+");
    }

    private int precedence(String op) {
        return switch (op) {
            case "^" -> 4;
            case "*", "/" -> 3;
            case "+", "-" -> 2;
            // --- NEW: Precedence for projection operator ---
            case "proj" -> 1;
            default -> 0;
        };
    }

    private Object evaluate(String input) {
        var tokens = tokenize(input);

        if (tokens.length == 0) throw new IllegalArgumentException("Empty expression");
        if (tokens.length == 1) return fetch(tokens[0]);

        Deque<String> output = new ArrayDeque<>();
        Deque<String> ops = new ArrayDeque<>();

        for (var token : tokens) {
            if (isFunction(token)) {
                ops.push(token);
            } else if ("(".equals(token)) {
                ops.push(token);
            } else if (")".equals(token)) {
                while (!ops.isEmpty() && !"(".equals(ops.peek())) {
                    output.addLast(ops.pop());
                }
                if (ops.isEmpty()) throw new IllegalArgumentException("Mismatched parentheses");
                ops.pop(); // '('
                if (!ops.isEmpty() && isFunction(ops.peek())) {
                    output.addLast(ops.pop()); // Функция после ")"
                }
            } else if ("+".equals(token) || "-".equals(token) ||
                       "*".equals(token) || "/".equals(token) || "^".equals(token) || "proj".equals(token)) {
                while (!ops.isEmpty() &&
                       !"(" .equals(ops.peek()) &&
                        precedence(ops.peek()) >= precedence(token)) {
                    output.addLast(ops.pop());
                }
                ops.push(token);
            } else {
                output.addLast(token);
            }
        }

        while (!ops.isEmpty()) {
            if ("(".equals(ops.peek()) || ")".equals(ops.peek()))
                throw new IllegalArgumentException("Mismatched parentheses");
            output.addLast(ops.pop());
        }

        Deque<Object> stack = new ArrayDeque<>();

        for (var token : output) {
            if (isFunction(token)) {
                var arg = stack.pop();
                stack.push(applyFunc(token, arg));
            } else if ("+".equals(token) || "-".equals(token) ||
                       "*".equals(token) || "/".equals(token) || "^".equals(token) || "proj".equals(token)) {
                var right = stack.pop();
                var left = stack.pop();
                stack.push(applyOp(left, right, token));
            } else {
                stack.push(fetch(token));
            }
        }

        if (stack.size() != 1) throw new IllegalArgumentException("Invalid expression");
        return stack.pop();
    }

    private Object applyOp(Object left, Object right, String op) {
        // Vector operations
        if (left instanceof Vector va && right instanceof Vector vb) {
            return switch (op) {
                case "+" -> va.add(vb);
                case "-" -> va.sub(vb);
                case "*" -> va.dot(vb);
                case "x" -> va.cross(vb);
                // --- NEW: Vector projection operator ---
                case "proj" -> va.projectOnto(vb);
                default -> throw new IllegalArgumentException("Vector operation not supported: " + op);
            };
        }

        // Scalar multiplication for Vector
        if (left instanceof Vector va && right instanceof Number rn) {
            if ("*".equals(op)) {
                return va.mul(rn.doubleValue());
            }
            if ("/".equals(op)) {
                if (rn.doubleValue() == 0) throw new ArithmeticException("Division by zero");
                return va.mul(1.0 / rn.doubleValue());
            }
        }
        if (left instanceof Number ln && right instanceof Vector vb) {
            if ("*".equals(op)) {
                return vb.mul(ln.doubleValue());
            }
        }

        // Matrix operations
        if (left instanceof Matrix ma && right instanceof Matrix mb) {
            return switch (op) {
                case "+" -> ma.add(mb);
                case "-" -> ma.sub(mb);
                case "*" -> ma.mul(mb);
                default -> throw new IllegalArgumentException("Matrix operation not supported: " + op);
            };
        }

        if (left instanceof Matrix ma && right instanceof Number rn) {
            return switch (op) {
                case "*" -> ma.mul(rn.doubleValue());
                default -> throw new IllegalArgumentException("Matrix scalar operation not supported: " + op);
            };
        }

        if (left instanceof Matrix ma && op.equals("^")) {
            if (!(right instanceof Integer)) {
                throw new IllegalArgumentException("Matrix exponent must be an integer");
            }
            return ma.pow((int) right);
        }

        // Fraction and Number operations
        if ((left instanceof Fraction fa && right instanceof Fraction fb) ||
            (left instanceof Number na && right instanceof Number nb)) {
            var la = (Number) left;
            var ra = (Number) right;

            return switch (op) {
                case "+" -> la.doubleValue() + ra.doubleValue();
                case "-" -> la.doubleValue() - ra.doubleValue();
                case "*" -> la.doubleValue() * ra.doubleValue();
                case "/" -> la.doubleValue() / ra.doubleValue();
                case "^" -> Math.pow(la.doubleValue(), ra.doubleValue());
                default -> throw new IllegalArgumentException("Operation not supported: " + op);
            };
        }

        // Complex operations
        if (left instanceof Complex ca && right instanceof Complex cb) {
            return switch (op) {
                case "+" -> ca.add(cb);
                case "-" -> ca.sub(cb);
                case "*" -> ca.mul(cb);
                case "/" -> ca.div(cb);
                case "^" -> {
                    if (!(right instanceof Number)) throw new IllegalArgumentException("Exponent must be numeric");
                    yield ca.pow(((Number) right).doubleValue());
                }
                default -> throw new IllegalArgumentException("Complex operation not supported: " + op);
            };
        }

        throw new IllegalArgumentException(
            "Incompatible types for operator '" + op +
            "': " + left.getClass().getSimpleName() + ", " + right.getClass().getSimpleName());
    }
}
