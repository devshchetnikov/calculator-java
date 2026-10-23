import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.*;

public class Main {

    private static double ans = 0; 
    private static boolean useDegrees = true; 
    private static final Map<String, Double> variables = new HashMap<>(); 

    // Вынесены в статические поля для оптимизации
    private static final Map<String, Integer> PRECEDENCE = new HashMap<>();
    private static final Set<String> FUNCTIONS = new HashSet<>();

    static {
        PRECEDENCE.put("+", 1); 
        PRECEDENCE.put("-", 1);
        PRECEDENCE.put("*", 2); 
        PRECEDENCE.put("/", 2); 
        PRECEDENCE.put("%", 2);
        PRECEDENCE.put("//", 2); // Новое: целочисленное деление
        PRECEDENCE.put("^", 3);
        PRECEDENCE.put("u-", 4); 
        PRECEDENCE.put("!", 5);  

        FUNCTIONS.addAll(Arrays.asList("sin", "cos", "tan", "sqrt", "log", "log10", "ln", "min", "max", "abs"));
        // Новые функции
        FUNCTIONS.addAll(Arrays.asList("asin", "acos", "atan", "sinh", "cosh", "tanh"));
    }

    public static void main(String[] args) {
        try (Scanner scanner = new Scanner(System.in)) {
            List<String> history = new ArrayList<>();
            showWelcomeMessage();

            StringBuilder multiLineInput = new StringBuilder();
            int openBrackets = 0;

            while (true) {
                try {
                    System.out.print((multiLineInput.length() > 0 ? "... " : "\nВведите выражение или команду: "));
                    String input = scanner.nextLine().trim();

                    if (input.isEmpty()) continue;

                    // Поддержка многострочного ввода для скобок
                    for (char c : input.toCharArray()) {
                        if (c == '(') openBrackets++;
                        if (c == ')') openBrackets--;
                    }
                    if (multiLineInput.length() > 0) {
                        multiLineInput.append(" ").append(input);
                    } else {
                        multiLineInput.append(input);
                    }

                    if (openBrackets > 0) continue;

                    String currentInput = multiLineInput.toString();
                    multiLineInput.setLength(0);
                    openBrackets = 0;

                    if (currentInput.equalsIgnoreCase("exit")) break;
                    if (currentInput.equalsIgnoreCase("help")) {
                        showWelcomeMessage();
                        continue;
                    }
                    if (currentInput.equalsIgnoreCase("history")) {
                        printHistory(history);
                        continue;
                    }
                    if (currentInput.equalsIgnoreCase("clear")) {
                        history.clear();
                        variables.clear();
                        ans = 0;
                        System.out.println("История, переменные и ans успешно очищены.");
                        continue;
                    }
                    if (currentInput.equalsIgnoreCase("mode")) {
                        useDegrees = !useDegrees;
                        System.out.println("Режим тригонометрии изменен! Текущий: " + (useDegrees ? "ГРАДУСЫ" : "РАДИАНЫ"));
                        continue;
                    }
                    if (currentInput.equalsIgnoreCase("vars")) {
                        printVariables();
                        continue;
                    }
                    if (currentInput.toLowerCase().startsWith("del ")) {
                        String varName = currentInput.substring(4).trim().toLowerCase();
                        if (variables.containsKey(varName)) {
                            variables.remove(varName);
                            System.out.println("Переменная '" + varName + "' удалена.");
                            history.add(currentInput);
                        } else {
                            System.out.println("Ошибка! Переменная '" + varName + "' не найдена.");
                        }
                        continue;
                    }
                    if (currentInput.toLowerCase().startsWith("reset ans")) {
                        ans = 0;
                        System.out.println("ans сброшен в 0.");
                        continue;
                    }

                    if (currentInput.contains("=")) {
                        handleAssignment(currentInput, history);
                        continue;
                    }

                    double res = evaluate(currentInput);
                    // Защита от переполнения (Infinity)
                    if (Double.isInfinite(res)) {
                        throw new IllegalArgumentException("Переполнение: результат слишком велик.");
                    }

                    String formattedRes = formatResult(res);
                    System.out.println("Результат: " + formattedRes);
                    
                    history.add(currentInput + " = " + formattedRes);
                    ans = res;

                } catch (Exception e) {
                    System.out.println("Ошибка! " + e.getMessage());
                    multiLineInput.setLength(0);
                    openBrackets = 0;
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
        if (varName.equals("pi") || varName.equals("e") || varName.equals("ans") || varName.equals("tau")) {
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

            // Обработка двухсимвольного оператора //
            if (c == '/' && i + 1 < expr.length() && expr.charAt(i + 1) == '/') {
                rawTokens.add("//");
                i += 2;
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

            // Объединение минуса с числом (унарный минус)
            if (current.equals("-") && k + 1 < rawTokens.size() && isNumber(rawTokens.get(k + 1))) {
                tokens.add("u-" + rawTokens.get(k + 1));
                k++;
            } else {
                tokens.add(current);
            }

            // Автодополнение умножением (implicit multiplication)
            if (k + 1 < tokens.size()) {
                String next = tokens.get(k + 1);
                String prev = tokens.get(k);
                if (prev.equals(")") && next.equals("(")) {
                    tokens.add(k + 1, "*");
                } else if (isNumber(prev) && (isVariableOrConstant(next) || next.equals("("))) {
                    tokens.add(k + 1, "*");
                } else if (isVariableOrConstant(prev) && (next.equals("(") || isVariableOrConstant(next))) {
                    tokens.add(k + 1, "*");
                } else if (prev.equals(")") && (isNumber(next) || isVariableOrConstant(next))) {
                    tokens.add(k + 1, "*");
                }
            }
        }

        // Обработка постфиксного факториала (5! -> 5 !)
        List<String> finalTokens = new ArrayList<>();
        for (int i = 0; i < tokens.size(); i++) {
            if (tokens.get(i).equals("!") && i > 0) {
                finalTokens.set(finalTokens.size() - 1, finalTokens.get(finalTokens.size() - 1) + "!");
            } else {
                finalTokens.add(tokens.get(i));
            }
        }

        return finalTokens;
    }

    private static List<String> shuntingYard(List<String> tokens) {
        List<String> output = new ArrayList<>();
        Stack<String> stack = new Stack<>();
        boolean expectUnary = true;

        for (String token : tokens) {
            if (isNumberOrVariable(token)) {
                output.add(token);
                expectUnary = false;
            } else if (FUNCTIONS.contains(token)) {
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
                if (!stack.isEmpty() && FUNCTIONS.contains(stack.peek())) {
                    output.add(stack.pop());
                }
                expectUnary = false;
            } else { 
                if (expectUnary && token.equals("-")) {
                    token = "u-";
                }
                
                while (!stack.isEmpty() && !stack.peek().equals("(") && 
                       (PRECEDENCE.getOrDefault(stack.peek(), 0) > PRECEDENCE.getOrDefault(token, 0) ||
                       (PRECEDENCE.getOrDefault(stack.peek(), 0) == PRECEDENCE.getOrDefault(token, 0) && !token.equals("^")))) {
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
                if (token.endsWith("!")) {
                    if (stack.isEmpty()) throw new IllegalArgumentException("Неверное выражение.");
                    String numPart = token.substring(0, token.length() - 1);
                    if (!isNumber(numPart)) throw new IllegalArgumentException("Факториал применен к нечислу.");
                    double val = Double.parseDouble(numPart);
                    if (val < 0 || val != Math.floor(val)) throw new IllegalArgumentException("Факториал определен только для целых неотрицательных чисел.");
                    if (val > 20) throw new IllegalArgumentException("Слишком большое число для факториала (риск переполнения).");
                    stack.push(factorial((int) val));
                    continue;
                }

                if (token.equals("min") || token.equals("max")) {
                    if (stack.size() < 2) throw new IllegalArgumentException(token + " требует два аргумента.");
                    double b = stack.pop();
                    double a = stack.pop();
                    stack.push(token.equals("min") ? Math.min(a, b) : Math.max(a, b));
                    double res = stack.peek();
                    if (Double.isInfinite(res)) throw new IllegalArgumentException("Переполнение в функции " + token);
                    continue;
                }

                if (token.equals("sin") || token.equals("cos") || token.equals("tan") || 
                    token.equals("asin") || token.equals("acos") || token.equals("atan") ||
                    token.equals("sinh") || token.equals("cosh") || token.equals("tanh") || token.equals("abs")) {
                    
                    if (stack.isEmpty()) throw new IllegalArgumentException("Функция " + token + " требует аргумент.");
                    double val = stack.pop();
                    
                    double result;
                    switch (token) {
                        case "sin": result = Math.sin(useDegrees ? Math.toRadians(val) : val); break;
                        case "cos": result = Math.cos(useDegrees ? Math.toRadians(val) : val); break;
                        case "tan": 
                            double cosVal = Math.cos(useDegrees ? Math.toRadians(val) : val);
                            if (Math.abs(cosVal) < 1e-12) throw new IllegalArgumentException("Тангенс не определен для этого угла.");
                            result = Math.tan(useDegrees ? Math.toRadians(val) : val); 
                            break;
                        case "asin": 
                            if (val < -1 || val > 1) throw new IllegalArgumentException("asin определен для [-1, 1].");
                            result = Math.asin(val); 
                            break;
                        case "acos": 
                            if (val < -1 || val > 1) throw new IllegalArgumentException("acos определен для [-1, 1].");
                            result = Math.acos(val); 
                            break;
                        case "atan": result = Math.atan(val); break;
                        case "sinh": result = Math.sinh(val); break;
                        case "cosh": result = Math.cosh(val); break;
                        case "tanh": result = Math.tanh(val); break;
                        case "abs": result = Math.abs(val); break;
                        default: throw new IllegalStateException("Unexpected value: " + token);
                    }
                    if (Double.isInfinite(result)) throw new IllegalArgumentException("Переполнение в функции " + token);
                    stack.push(result);
                    continue;
                }

                if (token.equals("sqrt")) {
                    if (stack.isEmpty()) throw new IllegalArgumentException("Функция sqrt требует аргумент.");
                    double val = stack.pop();
                    if (val < 0) throw new IllegalArgumentException("Корень из отрицательного числа.");
                    double res = Math.sqrt(val);
                    if (Double.isInfinite(res)) throw new IllegalArgumentException("Переполнение в sqrt");
                    stack.push(res);
                    continue;
                }

                if (token.equals("ln") || token.equals("log")) {
                    if (stack.isEmpty()) throw new IllegalArgumentException("Функция ln требует аргумент.");
                    double val = stack.pop();
                    if (val <= 0) throw new IllegalArgumentException("Логарифм определен только для положительных чисел.");
                    double res = Math.log(val);
                    if (Double.isInfinite(res)) throw new IllegalArgumentException("Переполнение в ln");
                    stack.push(res);
                    continue;
                }

                if (token.equals("log10")) {
                    if (stack.isEmpty()) throw new IllegalArgumentException("Функция log10 требует аргумент.");
                    double val = stack.pop();
                    if (val <= 0) throw new IllegalArgumentException("Логарифм определен только для положительных чисел.");
                    double res = Math.log10(val);
                    if (Double.isInfinite(res)) throw new IllegalArgumentException("Переполнение в log10");
                    stack.push(res);
                    continue;
                }

                if (stack.size() < 2) throw new IllegalArgumentException("Неверное выражение.");
                double b = stack.pop();
                double a = stack.pop();
                double res;

                switch (token) {
                    case "+": res = a + b; break;
                    case "-": res = a - b; break;
                    case "*": res = a * b; break;
                    case "/": 
                        if (b == 0) throw new IllegalArgumentException("Деление на ноль."); 
                        res = a / b; 
                        break;
                    case "//": 
                        if (b == 0) throw new IllegalArgumentException("Деление на ноль."); 
                        res = (double)((long)a / (long)b); 
                        break;
                    case "%": 
                        if (b == 0) throw new IllegalArgumentException("Деление на ноль.");
                        res = a % b;
                        if (res < 0) res += Math.abs(b);
                        break;
                    case "^": res = Math.pow(a, b); break;
                    default: throw new IllegalArgumentException("Неизвестный оператор: " + token);
                }
                if (Double.isInfinite(res)) throw new IllegalArgumentException("Переполнение в операции " + token);
                stack.push(res);
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
        return variables.containsKey(s) || s.equals("pi") || s.equals("e") || s.equals("ans") || s.equals("tau");
    }

    private static boolean isNumberOrVariable(String s) {
        return isNumber(s) || isVariableOrConstant(s);
    }

    private static double getVariableOrConstantValue(String s) {
        switch (s) {
            case "pi": return Math.PI;
            case "e": return Math.E;
            case "ans": return ans;
            case "tau": return 2 * Math.PI;
            default: return variables.get(s);
        }
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
        System.out.println("Команды: help, history, clear, mode, vars, exit, del <var>, reset ans");
        System.out.println("Текущий режим тригонометрии: " + (useDegrees ? "ГРАДУСЫ" : "РАДИАНЫ"));
        System.out.println("Доступные функции: sin, cos, tan, asin, acos, atan, sinh, cosh, tanh, sqrt, log (ln), log10, abs, min, max, !");
        System.out.println("Доступные константы: pi, e, ans, tau");
        System.out.println("Операторы: +, -, *, /, // (целочисленное), %, ^");
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
