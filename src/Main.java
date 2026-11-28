import java.util.*;

public class Calculator {

    // --- Fraction ---
    private static class Fraction extends Number {
        private final long num;
        private final long denom;

        Fraction(long num, long denom) {
            if (denom == 0) throw new ArithmeticException("Division by zero");
            if (denom < 0) { num = -num; denom = -denom; }
            long g = gcd(Math.abs(num), denom);
            this.num = num / g;
            this.denom = denom / g;
        }

        private static Fraction of(long num, long denom) {
            return new Fraction(num, denom);
        }

        Fraction add(Fraction o) {
            return of(Math.addExact(Math.multiplyExact(num, o.denom), Math.multiplyExact(o.num, denom)),
                      Math.multiplyExact(denom, o.denom));
        }
        Fraction sub(Fraction o) {
            return of(Math.subtractExact(Math.multiplyExact(num, o.denom), Math.multiplyExact(o.num, denom)),
                      Math.multiplyExact(denom, o.denom));
        }
        Fraction mul(Fraction o) {
            return of(Math.multiplyExact(num, o.num), Math.multiplyExact(denom, o.denom));
        }
        Fraction div(Fraction o) {
            if (o.num == 0) throw new ArithmeticException("Division by zero");
            return of(Math.multiplyExact(num, o.denom), Math.multiplyExact(denom, o.num));
        }

        @Override public int intValue() { return (int) toDouble(); }
        @Override public long longValue() { return (long) toDouble(); }
        @Override public float floatValue() { return (float) toDouble(); }
        @Override public double doubleValue() { return toDouble(); }

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
            for (int i = 0; i < dim; i++) res[i] = this.v[i] + o.v[i];
            return new Vector(res);
        }

        Vector sub(Vector o) {
            if (this.dim != o.dim) throw new IllegalArgumentException("Vector dimensions must match");
            double[] res = new double[dim];
            for (int i = 0; i < dim; i++) res[i] = this.v[i] - o.v[i];
            return new Vector(res);
        }

        Vector mul(double scalar) {
            double[] res = new double[dim];
            for (int i = 0; i < dim; i++) res[i] = this.v[i] * scalar;
            return new Vector(res);
        }

        double dot(Vector o) {
            if (this.dim != o.dim) throw new IllegalArgumentException("Vector dimensions must match");
            double sum = 0;
            for (int i = 0; i < dim; i++) sum += this.v[i] * o.v[i];
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
            for (double val : v) sum += val * val;
            return Math.sqrt(sum);
        }

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
            this.m = deepCopy(data);
        }

        private static double[][] deepCopy(double[][] data) {
            double[][] copy = new double[data.length][];
            for (int i = 0; i < data.length; i++) {
                copy[i] = data[i].clone();
            }
            return copy;
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
            for (int i = 0; i < rows; i++) col[i] = m[i][index];
            return col;
        }

        Matrix add(Matrix o) {
            if (this.rows != o.rows || this.cols != o.cols) {
                throw new IllegalArgumentException("Matrix dimensions must match for addition");
            }
            double[][] res = new double[rows][cols];
            for (int i = 0; i < rows; i++)
                for (int j = 0; j < cols; j++) {
                    checkSumOverflow(m[i][j], o.m[i][j]);
                    res[i][j] = m[i][j] + o.m[i][j];
                }
            return new Matrix(res);
        }

        Matrix sub(Matrix o) {
            if (this.rows != o.rows || this.cols != o.cols) {
                throw new IllegalArgumentException("Matrix dimensions must match for subtraction");
            }
            double[][] res = new double[rows][cols];
            for (int i = 0; i < rows; i++)
                for (int j = 0; j < cols; j++) {
                    checkSumOverflow(m[i][j], -o.m[i][j]);
                    res[i][j] = m[i][j] - o.m[i][j];
                }
            return new Matrix(res);
        }

        Matrix mul(Matrix o) {
            if (this.cols != o.rows) {
                throw new IllegalArgumentException("A columns must match B rows for multiplication");
            }
            double[][] res = new double[this.rows][o.cols];
            for (int i = 0; i < this.rows; i++)
                for (int j = 0; j < o.cols; j++) {
                    double sum = 0;
                    for (int k = 0; k < this.cols; k++) {
                        checkMulOverflow(m[i][k], o.m[k][j]);
                        sum += m[i][k] * o.m[k][j];
                    }
                    res[i][j] = sum;
                }
            return new Matrix(res);
        }

        Matrix mul(double scalar) {
            double[][] res = new double[rows][cols];
            for (int i = 0; i < rows; i++)
                for (int j = 0; j < cols; j++) {
                    checkMulOverflow(m[i][j], scalar);
                    res[i][j] = m[i][j] * scalar;
                }
            return new Matrix(res);
        }

        Matrix pow(int exponent) {
            if (rows != cols) {
                throw new IllegalArgumentException("Only square matrices can be raised to a power");
            }
            if (exponent == 0) return identity(rows);
            if (exponent < 0) return inverse().pow(-exponent);
            Matrix result = identity(rows);
            Matrix base = this;
            int e = exponent;
            while (e > 0) {
                if ((e & 1) == 1) result = result.mul(base);
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
            for (int i = 0; i < rows; i++)
                for (int j = 0; j < cols; j++)
                    res[j][i] = m[i][j];
            return new Matrix(res);
        }

        Matrix inverse() {
            if (rows != cols) {
                throw новых IllegalArgumentException("Only square matrices can be inverted");
            }
            double determinant = det();
            if (Math.abs(determinant) < 1e-12) {
                throw new ArithmeticException("Matrix is singular and cannot be inverted");
            }
            if (rows == 1) {
                return new Matrix(new double[][]{{1.0 / m[0][0]}});
            }
            double[][] inv = new double[rows][cols];
            for (int i = 0; i < rows; i++)
                for (int j = 0; j < cols; j++) {
                    double sign = Math.pow(-1, i + j);
                    inv[j][i] = sign * minor(i, j).det() / determinant;
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
            for (int i = 0; i < size; i++) id[i][i] = 1.0;
            return new Matrix(id);
        }

        private static void checkMulOverflow(double a, double b) {
            if (Double.isInfinite(a * b)) throw new ArithmeticException("Overflow in matrix multiplication");
        }

        private static void checkSumOverflow(double a, double b) {
            if (Double.isInfinite(a + b)) throw new ArithmeticException("Overflow in matrix addition");
        }

        // --- Row Echelon Form (REF) ---
        Matrix rowEchelonForm() {
            double[][] a = deepCopy(m);
            int lead = 0;
            for (int r = 0; r < rows; r++) {
                if (lead >= cols) return new Matrix(a);
                int i = r;
                while (Math.abs(a[i][lead]) < 1e-12) {
                    i++;
                    if (i == rows) {
                        i = r;
                        lead++;
                        if (lead == cols) return new Matrix(a);
                    }
                }
                double[] temp = a[i];
                a[i] = a[r];
                a[r] = temp;
                double lv = a[r][lead];
                for (int j = 0; j < cols; j++) a[r][j] /= lv;
                for (int i2 = r + 1; i2 < rows; i2++) {
                    double lv2 = a[i2][lead];
                    for (int j = 0; j < cols; j++) {
                        a[i2][j] -= lv2 * a[r][j];
                    }
                }
                lead++;
            }
            return new Matrix(a);
        }

        // --- Reduced Row Echelon Form (RREF) ---
        Matrix reducedRowEchelonForm() {
            double[][] a = rowEchelonForm().m;
            for (int r = rows - 1; r >= 0; r--) {
                int leadCol = -1;
                for (int c = 0; c < cols; c++) {
                    if (Math.abs(a[r][c]) > 1e-12) {
                        leadCol = c;
                        break;
                    }
                }
                if (leadCol == -1) continue;
                for (int i2 = r - 1; i2 >= 0; i2++) {
                    double factor = a[i2][leadCol];
                    for (int j = 0; j < cols; j++) {
                        a[i2][j] -= factor * a[r][j];
                    }
                }
            }
            return new Matrix(a);
        }

        // --- Rank (NEW) ---
        int rank() {
            double[][] ref = rowEchelonForm().m;
            int rank = 0;
            for (int r = 0; r < rows; r++) {
                for (int c = 0; c < cols; c++) {
                    if (Math.abs(ref[r][c]) > 1e-12) {
                        rank++;
                        break;
                    }
                }
            }
            return rank;
        }

        // --- LU Decomposition ---
        Matrix luDecomposition() {
            if (rows != cols) {
                throw new IllegalArgumentException("LU decomposition requires a square matrix");
            }
            double[][] a = deepCopy(m);
            int n = rows;

            for (int k = 0; k < n; k++) {
                if (Math.abs(a[k][k]) < 1e-12) {
                    throw new ArithmeticException("Zero pivot encountered, matrix is singular or requires pivoting");
                }
                for (int i = k + 1; i < n; i++) {
                    a[i][k] /= a[k][k];
                    for (int j = k + 1; j < n; j++) {
                        a[i][j] -= a[i][k] * a[k][j];
                    }
                }
            }
            return new Matrix(a);
        }

        Vector forwardSubstitution(Vector b) {
            if (rows != cols || b.getDim() != rows) {
                throw new IllegalArgumentException("Invalid dimensions for forward substitution");
            }
            double[] y = new double[rows];
            double[][] l = m;
            for (int i = 0; i < rows; i++) {
                double sum = b.v[i];
                for (int j = 0; j < i; j++) {
                    sum -= l[i][j] * y[j];
                }
                y[i] = sum / l[i][i];
            }
            return new Vector(y);
        }

        Vector backwardSubstitution(Vector y) {
            if (rows != cols || y.getDim() != rows) {
                throw new IllegalArgumentException("Invalid dimensions for backward substitution");
            }
            double[] x = new double[rows теряют;
            double[][] u = m;
            for (int i = rows - 1; i >= 0; i--) {
                double sum = y.v[i];
                for (int j = i + 1; j < rows; j++) {
                    sum -= u[i][j] * x[j];
                }
                x[i] = sum / u[i][i];
            }
            return new Vector(x);
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
    private static final String CMD_TRACE = "/tr";
    private static final String CMD_REF = "/ref";
    private static final String CMD_RREF = "/rref";
    private static final String CMD_SOLVE = "/solve";
    private static final String CMD_RANK = "/rank";
    private static final String CMD_DERIVATIVE = "/derivative"; // Новая команда

    private static final Set<String> RESERVED_NAMES = Set.of("ans", "pi", "e", "phi", "gamma", "x");

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
                System.out.println("Enter expressions: 2+2*2, 1/2+3/4, 10 % 3, sin(pi/2)");
                System.out.println("Matrix: [[1,2,3],[4,5,6]] * [[0,1],[1,0],[2,3]], Functions: exp, ln, abs, det, tr, transpose");
                System.out.println("Vector: [1,2,3] + [4,5,6], [1,2,3] x [4,5,6] (cross), |v| (norm), v1 proj v2");
                System.out.println("Commands: /exit, /help, /reset, /log, /logr, /del <name|N>, /vars, /tr <matrix>");
                System.out.println("Variables: x = 5, y = ans * 2, M = [[1,2],[3,4]], V = [1,2,3] ('x' is reserved for cross product)");
                System.out.println("Matrix power: A ^ 2, A ^ -1 (inverse)");
                System.out.println("New: /ref <matrix> (Row Echelon Form), /rref <matrix> (Reduced Row Echelon Form)");
                System.out.println("New: /solve <matrix_A> <vector_b> (Solves Ax = b using LU decomposition)");
                System.out.println("New: /rank <matrix> (Computes the rank of a matrix)");
                System.out.println("New: /derivative <expression> <point> (Numerical derivative at point)");
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
            case CMD_REF -> {
                if (arg == null) {
                    System.err.println("! Usage: /ref <matrix_variable>");
                    return;
                }
                Object obj = fetch(arg);
                if (obj instanceof Matrix m) {
                    Matrix ref = m.rowEchelonForm();
                    System.out.println("=> " + ref);
                    commandLog.add(line + " = " + ref);
                    lastResult = null;
                } else {
                    System.out.println("! Argument for /ref must be a matrix");
                }
            }
            case CMD_RREF -> {
                if (arg == null) {
                    System.out.println("! Usage: /rref <matrix_variable>");
                    return;
                }
                Object obj = fetch(arg);
                if (obj instanceof Matrix m) {
                    Matrix rref = m.reducedRowEchelonForm();
                    System.out.println("=> " + rref);
                    commandLog.add(line + " = " + rref);
                    lastResult = null;
                } else {
                    System.out.println("! Argument for /rref must be a matrix");
                }
            }
            case CMD_SOLVE -> {
                if (arg == null) {
                    System.out.println("! Usage: /solve <matrix_A> <vector_b>");
                    return;
                }
                String[] argsArr = arg.split("\\s+");
                if (argsArr.length != 2) {
                    System.out.println("! Usage: /solve <matrix_A> <vector_b>");
                    return;
                }
                Object objA = fetch(argsArr[0]);
                Object objB = fetch(argsArr[1]);
                if (!(objA instanceof Matrix) || !(objB instanceof Vector)) {
                    System.out.println("! Arguments for /solve must be a matrix and a vector");
                    return;
                }
                Matrix A = (Matrix) objA;
                Vector b = (Vector) objB;
                if (A.getRows() != A.getCols() || A.getRows() != b.getDim()) {
                    System.out.println("! Matrix must be square and vector dimension must match");
                    return;
                }

                try {
                    Matrix lu = A.luDecomposition();
                    Vector y = lu.forwardSubstitution(b);
                    Vector x = lu.backwardSubstitution(y);
                    System.out.println("=> " + x);
                    commandLog.add(line + " = " + x);
                    lastResult = null;
                } catch (ArithmeticException ex) {
                    System.err.printf("! Error: %s%n", ex.getMessage());
                }
            }
            case CMD_RANK -> {
                if (arg == null) {
                    System.out.println("! Usage: /rank <matrix_variable>");
                    return;
                }
                Object obj = fetch(arg);
                if (obj instanceof Matrix m) {
                    int r = m.rank();
                    System.out.println("=> " + r);
                    commandLog.add(line + " = " + r);
                    lastResult = (double) r;
                } else {
                    System.out.println("! Argument for /rank must be a matrix");
                }
            }
            case CMD_DERIVATIVE -> {
                if (arg == null) {
                    System.out.println("! Usage: /derivative <expression> <point>");
                    return;
                }
                String[] argsArr = arg.split("\\s+");
                if (argsArr.length != 2) {
                    System.out.println("! Usage: /derivative <expression> <point>");
                    return;
                }
                String expr = argsArr[0];
                String pointStr = argsArr[1];
                double h = 1e-7;
                try {
                    double x0 = Double.parseDouble(pointStr);
                    double f1 = evaluateAt(expr, x0 + h);
                    double f2 = evaluateAt(expr, x0 - h);
                    double derivative = (f1 - f2) / (2 * h);
                    System.out.println("=> " + formatDouble(derivative));
                    commandLog.add(line + " = " + formatDouble(derivative));
                    lastResult = derivative;
                } catch (NumberFormatException e) {
                    System.err.println("! Point must be a valid number");
                } catch (ArithmeticException | IllegalArgumentException ex) {
                    System.err.printf("! Error: %s%n", ex.getMessage());
                }
            }
            default -> System.err.println("! Unknown command: " + cmd);
        }
    }

    // Вспомогательный метод для вычисления выражения при конкретном значении переменной x
    private double evaluateAt(String expression, double xValue) throws ArithmeticException, IllegalArgumentException {
        // Заменяем переменную x на её значение
        String exprWithX = expression.replaceAll("\\bx\\b", "(" + formatDouble(xValue) + ")");
        Object result = evaluate(exprWithX);
        if (result instanceof Double d) return d;
        if (result instanceof Fraction f) return f.toDouble();
        throw new IllegalArgumentException("Expression did not evaluate to a numeric value");
    }

    private void handleAssignment(String line) {
        String[] parts = line.split("=", 2);
        String name = parts[0].trim();
        String expr = parts[1].trim();

        if (RESERVED_NAMES.contains(name)) {
            System.out.println("! Cannot assign to reserved name: " + name);
            return;
        }
        if (name.startsWith("/")) {
            System.out.println("! Cannot assign to command: " + name);
            return;
        }
        if (!name.matches("[a-zA-Z_][a-zA-Z0-9_]*")) {
            System.out.println("! Invalid variable name: " + name);
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

    // ... остальной код без изменений ...
}
