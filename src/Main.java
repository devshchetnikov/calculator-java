import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.*;

public class Main {

    private static double ans = 0; // Последний результат
    private static boolean useDegrees = true; // true - градусы, false - радианы
    private static final Map<String, Double> variables = new HashMap<>(); // Пользовательские переменные

    public static void main(String[] args) {
        try (Scanner scanner = new Scanner(System.in)) {
            List<String> history = new ArrayList<>();

            showWelcomeMessage();

            while (true) {
                try {
                    System.out.print("\nВведите выражение или команду: ");
                    String input = scanner.nextLine().trim();

                    if (input.isEmpty()) continue;
                    if (input.equalsIgnoreCase("exit")) break;
                    if (input.equalsIgnoreCase("help")) {
                        showWelcomeMessage();
                        continue;
                    }
                    if (input.equalsIgnoreCase("history")) {
                        printHistory(history);
                        continue;
                    }
                    if (input.equalsIgnoreCase("clear")) {
                        history.clear();
                        variables.clear();
                        ans = 0;
                        System.out.println("История, переменные и ans успешно очищены.");
                        continue;
                    }
                    if (input.equalsIgnoreCase("mode")) {
                        useDegrees = !useDegrees;
                        System.out.println("Режим тригонометрии изменен! Текущий: " + (useDegrees ? "ГРАДУСЫ" : "РАДИАНЫ"));
                        continue;
                    }
                    if (input.equalsIgnoreCase("vars")) {
                        printVariables();
                        continue;
                    }

                    // Обработка присвоения переменной (например: x = 5 + pi)
                    if (input.contains("=")) {
                        handleAssignment(input, history);
                        continue;
                    }

                    // Обычное вычисление выражения
                    double res = evaluate(input);
                    String formattedRes = formatResult(res);
                    System.out.println("Результат: " + formattedRes);
                    
                    history.add(input + " = " + formattedRes);
                    ans = res;

                } catch (Exception e) {
                    System.out.println("Ошибка! " + e.getMessage());
                }
            }
        }
    }

    // Логика присвоения переменной
    private static void handleAssignment(String input, List<String> history) {
        String[] parts = input.split("=", 2);
        String varName = parts[0].trim().toLowerCase();
        String expression = parts[1].trim();

        if (!varName.matches("[a-z]+")) {
            System.out.println("Ошибка! Имя переменной должно состоять только из латинских букв.");
            return;
        }
        if (varName.equals("pi") || varName.equals("e") || varName.equals("ans")) {
            System.out.println("Ошибка! Нельзя перезаписывать системные константы и ans.");
            return;
        }

        double res = evaluate(expression);
        variables.put(varName, res);
        String formattedRes = formatResult(res);
        System.out.println(varName + " = " + formattedRes);
        history.add(input + " (Результат: " + formattedRes + ")");
        ans = res;
    }

    // Главный метод вычисления строкового выражения
    public static double evaluate(String expression) {
        List<String> tokens = tokenize(expression);
        List<String> rpn = shuntingYard(tokens);
        return calculateRPN(rpn);
    }

    // Разбиение строки на токены (числа, операторы, функции, переменные)
    private static List<String> tokenize(String expr) {
        List<String> tokens = new ArrayList<>();
        int i = 0;
        while (i < expr.length()) {
            char c = expr.charAt(i);
            if (Character.isWhitespace(c)) {
                i++;
                continue;
            }

            // Числа (включая точку/запятую)
            if (Character.isDigit(c) || c == '.') {
                StringBuilder sb = new StringBuilder();
                while (i < expr.length() && (Character.isDigit(expr.charAt(i)) || expr.charAt(i) == '.' || expr.charAt(i) == ',')) {
                    char nextChar = expr.charAt(i);
                    sb.append(nextChar == ',' ? '.' : nextChar);
                    i++;
                }
                tokens.add(sb.toString());
                continue;
            }

            // Буквы (функции, константы, переменные)
            if (Character.isLetter(c)) {
                StringBuilder sb = new StringBuilder();
                while (i < expr.length() && Character.isLetterOrDigit(expr.charAt(i))) {
                    sb.append(expr.charAt(i));
                    i++;
                }
                tokens.add(sb.toString().toLowerCase());
                continue;
            }

            // Операторы и скобки
            if ("+-*/^%!()".indexOf(c) != -1) {
                tokens.add(String.valueOf(c));
                i++;
                continue;
            }

            throw new IllegalArgumentException("Неизвестный символ в выражении: " + c);
        }
        return tokens;
    }

    // Алгоритм Шантинг-Ярд (Сортировочная станция Дейкстры)
    private static List<String> shuntingYard(List<String> tokens) {
        List<String> output = new ArrayList<>();
        Stack<String> stack = new Stack<>();

        Map<String, Integer> precedence = new HashMap<>();
        precedence.put("+", 1); precedence.put("-", 1);
        precedence.put("*", 2); precedence.put("/", 2); precedence.put("%", 2);
        precedence.put("^", 3);

        Set<String> functions = new HashSet<>(Arrays.asList("sin", "cos", "tan", "sqrt", "log", "log10"));

        boolean expectUnary = true; // Флаг для определения унарного минуса/плюса

        for (String token : tokens) {
            if (isNumberOrVariable(token)) {
                output.add(token);
                expectUnary = false;
            } else if (functions.contains(token)) {
                stack.push(token);
                expectUnary = false;
            } else if (token.equals("(")) {
                stack.push(token);
                expectUnary = true;
            } else if (token.equals(")")) {
                while (!stack.isEmpty() && !stack.peek().equals("(")) {
                    output.add(stack.pop());
                }
                if (stack.isEmpty()) throw new IllegalArgumentException("Пропущена открывающая скобка.");
                stack.pop(); // Удаляем '('
                if (!stack.isEmpty() && functions.contains(stack.peek())) {
                    output.add(stack.pop());
                }
                expectUnary = false;
            } else if (token.equals("!")) { // Факториал — постфиксный унарный оператор
                output.add(token);
                expectUnary = false;
            } else if (precedence.containsKey(token)) {
                // Обработка унарного минуса/плюса
                if (expectUnary) {
                    if (token.equals("-")) {
                        stack.push("u-"); // u- означает унарный минус
                    } else if (!token.equals("+")) {
                        throw new IllegalArgumentException("Некорректное использование оператора " + token);
                    }
                } else {
                    while (!stack.isEmpty() && precedence.containsKey(stack.peek()) &&
                            ((isLeftAssociative(token) && precedence.get(token) <= precedence.get(stack.peek())) ||
                             (!isLeftAssociative(token) && precedence.get(token) < precedence.get(stack.peek())))) {
                        output.add(stack.pop());
                    }
                    stack.push(token);
                }
                expectUnary = true;
            }
        }

        while (!stack.isEmpty()) {
            String op = stack.pop();
            if (op.equals("(") || op.equals(")")) throw new IllegalArgumentException("Дисбаланс скобок.");
            output.add(op);
        }

        return output;
    }

    // Вычисление выражения из ОПЗ (RPN)
    private static double calculateRPN(List<String> rpn) {
        Stack<Double> stack = new Stack<>();

        for (String token : rpn) {
            if (isNumberOrVariable(token)) {
                stack.push(resolveValue(token));
            } else if (token.equals("u-")) {
                if (stack.isEmpty()) throw new IllegalArgumentException("Ошибка в унарном минусе.");
                stack.push(-stack.pop());
            } else if (token.equals("!")) {
                if (stack.isEmpty()) throw new IllegalArgumentException("Ошибка в факториале.");
                double num = stack.pop();
                if (num < 0 || num != Math.floor(num)) {
                    throw new IllegalArgumentException("Факториал определен только для целых неотрицательных чисел.");
                }
                stack.push(factorial((int) num));
            } else if (Arrays.asList("sin", "cos", "tan", "sqrt", "log", "log10").contains(token)) {
                if (stack.isEmpty()) throw new IllegalArgumentException("Недостаточно аргументов для функции " + token);
                double num = stack.pop();
                stack.push(applyFunction(token, num));
            } else { // Бинарные операторы
                if (stack.size() < 2) throw new IllegalArgumentException("Некорректное выражение (не хватает чисел).");
                double num2 = stack.pop();
                double num1 = stack.pop();
                stack.push(applyBinaryOp(token, num1, num2));
            }
        }



