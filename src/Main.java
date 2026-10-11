import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.*;

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

    // ТФИЧА: Токенизатор теперь автоматически вставляет "*", где это необходимо (неявное умножение)
    private static List<String> tokenize(String expr) {
        List<String> rawTokens = new ArrayList<>();
        int i = 0;
        while (i < expr.length()) {
            char c = expr.charAt(i);
            if (Character.isWhitespace(c)) {
                i++;
                continue;
            }

            // Числа
            if (Character.isDigit(c) || c == '.') {
                StringBuilder sb = new StringBuilder();
                while (i < expr.length() && (Character.isDigit(expr.charAt(i)) || expr.charAt(i) == '.')) {
                    sb.append(expr.charAt(i));
                    i++;
                }
                rawTokens.add(sb.toString());
                continue;
            }

            // Буквы (функции, константы, переменные)
            if (Character.isLetter(c)) {
                StringBuilder sb = new StringBuilder();
                while (i < expr.length() && Character.isLetterOrDigit(expr.charAt(i))) {
                    sb.append(expr.charAt(i));
                    i++;
                }
                rawTokens.add(sb.toString().toLowerCase());
                continue;
            }

            // Операторы, скобки и ЗАПЯТАЯ
            if ("+-*/^%!(),".indexOf(c) != -1) {
                rawTokens.add(String.valueOf(c));
                i++;
                continue;
            }

            throw new IllegalArgumentException("Неизвестный символ в выражении: " + c);
        }

        // Вставка неявного умножения
        List<String> tokens = new ArrayList<>();
        for (int k = 0; k < rawTokens.size(); k++) {
            String current = rawTokens.get(k);
            tokens.add(current);

            if (k + 1 < rawTokens.size()) {
                String next = rawTokens.get(k + 1);

                // Случаи для неявного умножения:
                // 1. Число перед переменной/функцией (2pi, 3sin(x)) или перед скобкой (5(x+1))
                // 2. Переменная/константа перед открывающей скобкой (pi(a+b)) или перед другой переменной
                // 3. Закрывающая скобка перед числом, переменной или открывающей скобкой (x+1)(y+2)
                boolean isCurrNumOrVar = isNumberOrVariable(current) || current.equals(")");
                boolean isNextNumOrVarOrBracket = isNumberOrVariable(next) || next.equals("(") || new HashSet<>(Arrays.asList("sin", "cos", "tan", "sqrt", "log", "min", "max", "log10")).contains(next);

                if (current.equals(")") && next.equals("(")) {
                    tokens.add("*");
                } else if (isNumber(current) && (isVariableOrConstant(next) || next.equals("("))) {
                    tokens.add("*");
                } else if (isVariableOrConstant(current) && (next.equals("(") || isVariableOrConstant(next))) {
                    tokens.add("*");
                } else if (current.equals(")") && (isNumber(next) || isVariableOrConstant(next))) {
                    tokens.add("*");
                }
            }
        }
        return tokens;
    }

    // ФИЧА: Изменен под поддержку функций с несколькими аргументами (обработка запятой)
    private static List<String> shuntingYard(List<String> tokens) {
        List<String> output = new ArrayList<>();
        Stack<String> stack = new Stack<>();

        Map<String, Integer> precedence = new HashMap<>();
        precedence.put("+", 1); precedence.put("-", 1);
        precedence.put("*", 2); precedence.put("/", 2); precedence.put("%", 2);
        precedence.put("^", 3);
        precedence.put("u-", 4);

        Set<String> functions = new HashSet<>(Arrays.asList("sin", "cos", "tan", "sqrt", "log", "log10", "min", "max"));

        boolean expectUnary = true;

        for (String token : tokens) {
            if (isNumberOrVariable(token)) {
                output.add(token);
                expectUnary = false;
            } else if (functions.contains(token)) {
                stack.push(token);
                expectUnary = false;
            } else if (token.equals(",")) {
                // Запятая выталкивает операторы до ближайшей открывающей скобки
                while (!stack.isEmpty() && !stack.peek().equals("(")) {
                    output.add(stack.pop());
                }
                if (stack.isEmpty()) {
                    throw new IllegalArgumentException("Разделитель аргументов ',' использован вне скобок.");
                }
                expectUnary = true;
            } else if (token.equals("(")) {
                stack.push(token);
                expectUnary = true;
            } else if (token.equals(")")) {
                while (!stack.isEmpty() && !stack.peek().equals("(")) {
                    output.add(stack.pop());
                }
                if (stack.isEmpty()) throw new IllegalArgumentException("Пропущена открывающая скобка.");
                stack.pop(); // Удаляем "("
                
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

        while (!stack.isEmpty()) {
            String token = stack.pop();
            if (token.equals("(") || token.equals(")")) {




