import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.*;
import java.math.BigInteger;

public class Main {

    private static double ans = 0; 
    private static boolean useDegrees = true; 
    private static final Map<String, Double> variables = new HashMap<>(); 

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
                    // Новая команда для удаления переменных
                    if (input.toLowerCase().startsWith("del ")) {
                        String varName = input.substring(4).trim().toLowerCase();
                        if (variables.containsKey(varName)) {
                            variables.remove(varName);
                            System.out.println("Переменная '" + varName + "' удалена.");
                            history.add(input);
                        } else {
                            System.out.println("Ошибка! Переменная '" + varName + "' не найдена.");
                        }
                        continue;
                    }

                    if (input.contains("=")) {
                        handleAssignment(input, history);
                        continue;
                    }

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

    public static double evaluate(String expression) {
        List<String> tokens = tokenize(expression);
        List<String> rpn = shuntingYard(tokens);
        return calculateRPN(rpn);
    }

    private static List<String> tokenize(String expr) {
        List<String> rawTokens = new ArrayList<>();
        int i = 0;
        while (i < expr.length()) {
            char c = expr.charAt(i);
            if (Character.isWhitespace(c)) {
                i++;
                continue;
            }

            if (Character.isDigit(c) || c == '.') {
                StringBuilder sb = new StringBuilder();
                // Обработка отрицательного числа, если минус стоит в начале или после оператора/скобки
                if (c == '-' && (i == 0 || "+-*/^%!(),".indexOf(expr.charAt(i - 1)) != -1)) {
                    sb.append(c);
                    i++;
                    if (i >= expr.length() || !(Character.isDigit(expr.charAt(i)) || expr.charAt(i) == '.')) {
                        throw new IllegalArgumentException("Некорректный унарный минус.");
                    }
                }
                while (i < expr.length() && (Character.isDigit(expr.charAt(i)) || expr.charAt(i) == '.')) {
                    sb.append(expr.charAt(i));
                    i++;
                }
                rawTokens.add(sb.toString());
                continue;
            }

            if (Character.isLetter(c)) {
                StringBuilder sb = new StringBuilder();
                while (i < expr.length() && Character.isLetterOrDigit(expr.charAt(i))) {
                    sb.append(expr.charAt(i));
                    i++;
                }
                rawTokens.add(sb.toString().toLowerCase());
                continue;
            }

            if ("+-*/^%!(),".indexOf(c) != -1) {
                rawTokens.add(String.valueOf(c));
                i++;
                continue;
            }

            throw new IllegalArgumentException("Неизвестный символ в выражении: " + c);
        }

        List<String> tokens = new ArrayList<>();
        for (int k = 0; k < rawTokens.size(); k++) {
            String current = rawTokens.get(k);
            // Если минус стоит перед числом (например, -5), объединяем в один токен u-5
            if (current.equals("-") && k + 1 < rawTokens.size() && isNumber(rawTokens.get(k + 1))) {
                tokens.add("u-" + rawTokens.get(k + 1));
                k++; // пропускаем следующее число, так как мы его уже обработали
            } else {
                tokens.add(current);
            }

            if (k + 1 < tokens.size()) {
                String next = tokens.get(k + 1);
                if (current.equals(")") && next.equals("(")) {
                    tokens.add(k + 1, "*");
                } else if (isNumber(current) && (isVariableOrConstant(next) || next.equals("("))) {
                    tokens.add(k + 1, "*");
                } else if (isVariableOrConstant(current) && (next.equals("(") || isVariableOrConstant(next))) {
                    tokens.add(k + 1, "*");
                } else if (current.equals(")") && (isNumber(next) || isVariableOrConstant(next))) {
                    tokens.add(k + 1, "*");
                }
            }
        }
        return tokens;
    }

    private static List<String> shuntingYard(List<String> tokens) {
        List<String> output = new ArrayList<>();
        Stack<String> stack = new Stack<>();

        Map<String, Integer> precedence = new HashMap<>();
        precedence.put("+", 1); precedence.put("-", 1);
        precedence.put("*", 2); precedence.put("/", 2); precedence.put("%", 2);
        precedence.put("^", 3);
        precedence.put("u-", 4); 
        precedence.put("!", 5);  

        // Добавлены ln и log10
        Set<String> functions = new HashSet<>(Arrays.asList("sin", "cos", "tan", "sqrt", "log", "log10", "ln", "min", "max"));
        boolean expectUnary = true;

        for (String token : tokens) {
            if (isNumberOrVariable(token)) {
                output.add(token);
                expectUnary = false;
            } else if (functions.contains(token)) {
                stack.push(token);
                expectUnary = false;
            } else if (token.equals(",")) {
                while (!stack.isEmpty() && !stack.peek().equals("(")) {
                    output.add(stack.pop());
                }
                if (stack.isEmpty()) {
                    throw new IllegalArgumentException("Разделитель ',' использован вне скобок.");
                }
                expectUnary = true;
            } else if (token.equals("(")) {
                stack.push(token);
                expectUnary = true;
            } else if (token.equals(")")) {
                while (!stack.isEmpty() && !stack.peek().equals("(")) {
                    output.add(stack.pop());
                }
                if (stack.isEmpty()) {
                    throw new IllegalArgumentException("Пропущена открывающая скобка.");
                }
                stack.pop(); 
                if (!stack.isEmpty() && functions.contains(stack.peek())) {
                    output.add(stack.pop());
                }
                expectUnary = false;
            } else { 
                if (expectUnary && token.equals("-")) {
                    token = "u-";
                }
                
                while (!stack.isEmpty() && !stack.peek().equals("(") && 
                       (precedence.getOrDefault(stack.peek(), 0) > precedence.getOrDefault(token, 0) ||
                       (precedence.getOrDefault(stack.peek(), 0) == precedence.getOrDefault(token, 0) && !token.equals("^")))) {
                    output.add(stack.pop());
                }
                stack.push(token);
                expectUnary = true;
            }
        }

        while (!stack.isEmpty()) {
            String top = stack.pop();
            if (top.equals("(") || top.equals(")")) {
                throw new IllegalArgumentException("Несогласованные скобки.");
            }
            output.add(top);
        }

        return output;
    }

    private static double calculateRPN(List<String> rpn) {
        Stack<Double> stack = new Stack<>();

        for (String token : rpn) {
            // Обработка отрицательного числа (токен вида u-5)
            if (token.startsWith("u-") && isNumber(token.substring(2))) {
                stack.push(-Double.parseDouble(token.substring(2)));
                continue;
            }

            if (isNumber(token)) {
                stack.push(Double.parseDouble(token));
            } else if (isVariableOrConstant(token)) {
                stack.push(getVariableOrConstantValue(token));
            } else {
                if (token.equals("u-")) {
                    if (stack.isEmpty()) throw new IllegalArgumentException("Неверное выражение.");
                    stack.push(-stack.pop());
                    continue;
                }
                if (token.equals("!")) {
                    if (stack.isEmpty()) throw new IllegalArgumentException("Неверное выражение.");
                    double val = stack.pop();
                    if (val < 0 || val != Math.floor(val)) throw new IllegalArgumentException("Факториал определен только для целых неотрицательных чисел.");
                    stack.push(factorial((int) val));
                    continue;
                }

                if (token.equals("min") || token.equals("max")) {
                    if (stack.size() < 2) throw new IllegalArgumentException("Функции min/max требуют два аргумента.");
                    double b = stack.pop();
                    double a = stack.pop();
                    if (token.equals("min")) {
                        stack.push(Math.min(a, b));
                    } else {
                        stack.push(Math.max(a, b));
                    }
                    continue;
                }

                if (token.equals("sin") || token.equals("cos") || token.equals("tan")) {
                    if (stack.isEmpty()) throw new IllegalArgumentException("Функция " + token + " требует аргумент.");
                    double val = stack.pop();
                    if (useDegrees) {
                        val = Math.toRadians(val);
                    }
                    double result;
                    switch (token) {
                        case "sin": result = Math.sin(val); break;
                        case "cos": result = Math.cos(val); break;
                        case "tan": 
                            double cosVal = Math.cos(val);
                            if (Math.abs(cosVal) < 1e-12) {
                                throw new IllegalArgumentException("Тангенс не определен для этого угла.");
                            }
                            result = Math.tan(val); 
                            break;
                        default: throw new IllegalStateException("Unexpected value: " + token);
                    }
                    stack.push(result);
                    continue;
                }

                if (token.equals("sqrt")) {
                    if (stack.isEmpty()) throw new IllegalArgumentException("Функция sqrt требует аргумент.");
                    double val = stack.pop();
                    if (val < 0) throw new IllegalArgumentException("Корень из отрицательного числа.");
                    stack.push(Math.sqrt(val));
                    continue;
                }

                // Добавлена обработка ln (натуральный логарифм)
                if (token.equals("ln") || token.equals("log")) {
                    if (stack.isEmpty()) throw new IllegalArgumentException("Функция ln требует аргумент.");
                    double val = stack.pop();
                    if (val <= 0) throw new IllegalArgumentException("Логарифм определен только для положительных чисел.");
                    stack.push(Math.log(val));
                    continue;
                }

                // Добавлена обработка log10 (десятичный логарифм)
                if (token.equals("log10")) {
                    if (stack.isEmpty()) throw new IllegalArgumentException("Функция log10 требует аргумент.");
                    double val = stack.pop();
                    if (val <= 0) throw new IllegalArgumentException("Логарифм определен только для положительных чисел.");
                    stack.push(Math.log10(val));
                    continue;
                }

                if (stack.size() < 2) throw new IllegalArgumentException("Неверное выражение.");
                double b = stack.pop();
                double a = stack.pop();

                switch (token) {
                    case "+": stack.push(a + b); break;
                    case "-": stack.push(a - b); break;
                    case "*": stack.push(a * b); break;
                    case "/": 
                        if (b == 0) throw new IllegalArgumentException("Деление на ноль."); 
                        stack.push(a / b); 
                        break;
                    case "%": 
                        if (b == 0) throw new IllegalArgumentException("Деление на ноль.");
                        double res = a % b;
                        if (res < 0) {
                            res += Math.abs(b);
                        }
                        stack.push(res); 
                        break;
                    case "^": stack.push(Math.pow(a, b)); break;
                    default: throw new IllegalArgumentException("Неизвестный оператор: " + token);
                }
            }
        }

        if (stack.size() != 1) throw new IllegalArgumentException("Неверное выражение.");
        return stack.pop();
    }

    private static boolean isNumber(String s) {
        try {
            Double.parseDouble(s);
            return true;
        } catch (NumberFormatException e) {
            return false;
        }
    }

    private static boolean isVariableOrConstant(String s) {
        return variables.containsKey(s) || s.equals("pi") || s.equals("e") || s.equals("ans");
    }

    private static boolean isNumberOrVariable(String s) {
        return isNumber(s) || isVariableOrConstant(s);
    }

    private static double getVariableOrConstantValue(String s) {
        if (s.equals("pi")) return Math.PI;
        if (s.equals("e")) return Math.E;
        if (s.equals("ans")) return ans;
        return variables.get(s);
    }

    private static double factorial(int n) {
        BigInteger result = BigInteger.ONE;
        for (int i = 2; i <= n; i++) {
            result = result.multiply(BigInteger.valueOf(i));
        }
        return result.doubleValue();
    }

    private static String formatResult(double res) {
        if (Double.isNaN(res)) return "NaN";
        if (Double.isInfinite(res)) return "Infinity";
        
        DecimalFormatSymbols symbols = new DecimalFormatSymbols(Locale.US);
        symbols.setDecimalSeparator('.');
        DecimalFormat df = new DecimalFormat("#.################", symbols);
        String formatted = df.format(res);
        
        if (formatted.endsWith(".")) {
            formatted = formatted.substring(0, formatted.length() - 1);
        }
        return formatted;
    }

    private static void showWelcomeMessage() {
        System.out.println("=== Консольный калькулятор ===");
        System.out.println("Введите математическое выражение.");
        System.out.println("Команды: help, history, clear, mode, vars, exit, del <var>");
        System.out.println("Текущий режим тригонометрии: " + (useDegrees ? "ГРАДУСЫ" : "РАДИАНЫ"));
        // Обновлен список функций в приветственном сообщении
        System.out.println("Доступные функции: sin, cos, tan, sqrt, log (ln), log10, min, max, !");
        System.out.println("Доступные константы: pi, e, ans");
    }

    private static void printHistory(List<String> history) {
        if (history.isEmpty()) {
            System.out.println("История пуста.");
            return;
        }
        System.out.println("История вычислений:");
        for (int i = 0; i < history.size(); i++) {
            System.out.println((i + 1) + ". " + history.get(i));
        }
    }

    private static void printVariables() {
        if (variables.isEmpty()) {
            System.out.println("Пользовательских переменных нет.");
            return;
        }
        System.out.println("Переменные:");
        for (Map.Entry<String, Double> entry : variables.entrySet()) {
            System.out.println(entry.getKey() + " = " + formatResult(entry.getValue()));
        }
    }
}
