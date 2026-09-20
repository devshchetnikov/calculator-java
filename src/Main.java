import java.util.Scanner;
import java.util.ArrayList;
import java.util.List;

public class Main {

    public static void main(String[] args) {
        try (Scanner scanner = new Scanner(System.in)) {
            List<String> history = new ArrayList<>();

            System.out.println("--- Добро пожаловать в расширенный калькулятор! ---");
            System.out.println("Доступные команды: 'exit' - выход, 'history' - история.");

            while (true) {
                System.out.print("\nВведите первое число (или команду): ");
                String input1 = scanner.nextLine().trim();

                if (input1.equalsIgnoreCase("exit")) break;
                if (input1.equalsIgnoreCase("history")) {
                    printHistory(history);
                    continue;
                }

                double num1;
                try {
                    num1 = Double.parseDouble(input1.replace(",", "."));
                } catch (NumberFormatException e) {
                    System.out.println("Ошибка! Некорректное число или команда.");
                    continue;
                }

                System.out.print("Выберите действие (+, -, *, /, ^, %, sin, cos, sqrt): ");
                String op = scanner.nextLine().trim();
                if (op.equalsIgnoreCase("exit")) break;

                if (op.equalsIgnoreCase("sin") || op.equalsIgnoreCase("cos") || op.equalsIgnoreCase("sqrt")) {
                    double res = 0;
                    String expression = "";

                    switch (op.toLowerCase()) {
                        case "sin":
                            res = Math.sin(Math.toRadians(num1));
                            expression = String.format("sin(%s) = %s", formatResult(num1), formatResult(res));
                            break;
                        case "cos":
                            res = Math.cos(Math.toRadians(num1));
                            expression = String.format("cos(%s) = %s", formatResult(num1), formatResult(res));
                            break;
                        case "sqrt":
                            if (num1 < 0) {
                                System.out.println("Ошибка! Нельзя извлечь корень из отрицательного числа.");
                                continue;
                            }
                            res = Math.sqrt(num1);
                            expression = String.format("sqrt(%s) = %s", formatResult(num1), formatResult(res));
                            break;
                    }

                    System.out.println("Результат: " + formatResult(res));
                    history.add(expression);
                    continue;
                }

                System.out.print("Введите второе число: ");
                String input2 = scanner.nextLine().trim();
                if (input2.equalsIgnoreCase("exit")) break;

                double num2;
                try {
                    num2 = Double.parseDouble(input2.replace(",", "."));
                } catch (NumberFormatException e) {
                    System.out.println("Ошибка! Некорректное второе число.");
                    continue;
                }

                double res;
                switch (op) {
                    case "+": res = num1 + num2; break;
                    case "-": res = num1 - num2; break;
                    case "*": res = num1 * num2; break;
                    case "/":
                        if (num2 == 0) {
                            System.out.println("Ошибка! Делить на ноль нельзя.");
                            continue;
                        }
                        res = num1 / num2;
                        break;
                    case "^": res = Math.pow(num1, num2); break;
                    case "%": res = num1 % num2; break;
                    default:
                        System.out.println("Ошибка! Неверная операция.");
                        continue;
                }

                String formattedRes = formatResult(res);
                System.out.println("Результат: " + formattedRes);

                // Сохраняем в историю
                history.add(String.format("%s %s %s = %s", formatResult(num1), op, formatResult(num2), formattedRes));
            }

            System.out.println("Программа завершена. До свидания!");
        }
    }

    // Метод для красивого вывода чисел (убирает .0 у целых)
    private static String formatResult(double val) {
        if (val % 1 == 0) {
            return String.format("%.0f", val);
        }
        return String.valueOf(val);
    }

    // Метод для вывода истории
    private static void printHistory(List<String> history) {
        System.out.println("\n--- История операций ---");
        if (history.isEmpty()) {
            System.out.println("История пока пуста.");
        } else {
            for (String record : history) {
                System.out.println(record);
            }
        }
        System.out.println("--------------------");
    }
}
