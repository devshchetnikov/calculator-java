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

    // Алгоритм Шантинг-Ярд (Перевод в ОПН)
    private static List<String> shuntingYard(List<String> tokens) {
        List<String> output = new ArrayList<>();
        Stack<String> stack = new Stack<>();

        Map<String, Integer> precedence = new HashMap<>();
        precedence.put("+", 1); precedence.put("-", 1);
        precedence.put("*", 2); precedence.put("/", 2); precedence.put("%", 2);
        precedence.put("^", 3);
        precedence.put("u-", 4); // Высокий приоритет унарного минуса

        Set<String> functions = new HashSet<>(Arrays.asList("sin", "cos", "tan", "sqrt", "log", "log10"));

        boolean expectUnary = true;

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
                stack.pop();
                if (!stack.isEmpty() && functions.contains(stack.peek())) {
                    output.add(stack.pop());
                }
                expectUnary = false;
            } else if (token.equals("!")) {
                output.add(token);
                expectUnary = false;
            } else if (precedence.containsKey(token)) {
                if (expectUnary) {
                    if (token.equals("-")) {
                        stack.push("u-");
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

        // ДОПИСАНО: Очистка стека в конце алгоритма
        while (!stack.isEmpty()) {
            String op = stack.pop();
            if (op.equals("(") || op.equals(")")) {
                throw new IllegalArgumentException("Несогласованные скобки в выражении.");
            }
            output.add(op);
        }
        return output;
    }

    // Вычисление выражения в Обратной Польской Нотации (ОПН)
    private static double calculateRPN(List<String> rpn) {
        Stack<Double> stack = new Stack<>();
        Set<String> functions = new HashSet<>(Arrays.asList("sin", "cos", "tan", "sqrt", "log", "log10"));

        for (String token : rpn) {
            if (isNumber(token)) {
                stack.push(Double.parseDouble(token));
            } else if (isVariableOrConstant(token)) {
                stack.push(resolveVariable(token));
            } else if (functions.contains(token)) {
                if (stack.isEmpty()) throw new IllegalArgumentException("Недостаточно аргументов для функции " + token);
                double arg = stack.pop();
                stack.push(executeFunction(token, arg));
            } else if (token.equals("u-")) {
                if (stack.isEmpty()) throw new IllegalArgumentException("Недостаточно аргументов для унарного минуса");
                stack.push(-stack.pop());
            } else if (token.equals("!")) {
                if (stack.isEmpty()) throw new IllegalArgumentException("Недостаточно аргументов для факториала");
                stack.push(factorial(stack.pop()));
            } else {
                if (stack.size() < 2) throw new IllegalArgumentException("Некорректное выражение (недостаточно операндов)");
                double b = stack.pop();
                double a = stack.pop();
                stack.push(executeOperator(token, a, b));
            }
        }

        if (stack.size() != 1) {



