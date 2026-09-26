import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Scanner;
import java.util.NoSuchElementException;


public class Main {

    private static double ans = 0; // Переменная для хранения последнего результата

    public static void main(String[] args) {
        try (Scanner scanner = new Scanner(System.in)) {
            List<String> history = new ArrayList<>();

            System.out.println("--- Добро пожаловать в расширенный калькулятор! ---");
            System.out.println("Доступные команды: 'exit' - выход, 'history' - история, 'clear' - очистить историю.");
            System.out.println("Поддерживаются константы: 'pi', 'e', 'ans' (предыдущий ответ)");
            System.out.println("Функции: +, -, *, /, ^, %, sin, cos, tan, log, log10, sqrt");

            while (true) {
                try {
                    System.out.print("\nВведите первое число/константу (или команду): ");
                    String input1 = scanner.nextLine().trim();

                    if (input1.equalsIgnoreCase("exit")) 
                        break;
                    if (input1.equalsIgnoreCase("history")) {
                        printHistory(history);
                        continue;
                    }
                    if (input1.equalsIgnoreCase("clear")) {
                        history.clear();
                        System.out.println("История успешно очищена.");
                        continue;
                    }

                    double num1;
                    try {
                        num1 = parseNumber(input1);
                    } catch (NumberFormatException e) {
                        System.out.println("Ошибка! Некорректное число или команда.");
                        continue;
                    }

                    System.out.print("Выберите действие (+, -, *, /, ^, %, sin, cos, tan, log, log10, sqrt): ");
                    String op = scanner.nextLine().trim().toLowerCase();
                    if (op.equalsIgnoreCase("exit")) break;

                    // Унарные операции (с одним числом)
                    if (op.equals("sin") || op.equals("cos") || op.equals("tan") ||
                            op.equals("sqrt") || op.equals("log") || op.equals("log10")) {

                        double res = 0;
                        String expression = "";

                        switch (op) {
                            case "sin":
                                res = Math.sin(Math.toRadians(num1));
                                expression = String.format("sin(%s) = %s", formatResult(num1), formatResult(res));
                                break;
                            case "cos":
                                res = Math.cos(Math.toRadians(num1));
                                expression = String.format("cos(%s) = %s", formatResult(num1), formatResult(res));
                                break;
                            case "tan":
                                if (Math.abs(Math.cos(Math.toRadians(num1))) < 1e-10) {
                                    System.out.println("Ошибка! Тангенс " + formatResult(num1) + " градусов не существует.");
                                    continue;
                                }
                                res = Math.tan(Math.toRadians(num1));
                                expression = String.format("tan(%s) = %s", formatResult(num1), formatResult(res));
                                break;
                            case "sqrt":
                                if (num1 < 0) {
                                    System.out.println("Ошибка! Нельзя извлечь корень из отрицательного числа.");
                                    continue;
                                }
                                res = Math.sqrt(num1);
                                expression = String.format("sqrt(%s) = %s", formatResult(num1), formatResult(res));
                                break;
                            case "log":
                                if (num1 <= 0) {
                                    System.out.println("Ошибка! Натуральный логарифм определен только для чисел > 0.");
                                    continue;
                                }
                                res = Math.log(num1);
                                expression = String.format("log(%s) = %s", formatResult(num1), formatResult(res));
                                break;
                            case "log10":
                                if (num1 <= 0) {
                                    System.out.println("Ошибка! Десятичный логарифм определен только для чисел > 0.");
                                    continue;
                                }
                                res = Math.log10(num1);
                                expression = String.format("log10(%s) = %s", formatResult(num1), formatResult(res));
                                break;
                        }

                        ans = res;
                        System.out.println("Результат: " + formatResult(res));
                        history.add(expression);
                        continue;
                    }

                    System.out.print("Введите второе число или константу: ");
                    String input2 = scanner.nextLine().trim();
                    if (input2.equalsIgnoreCase("exit")) break;

                    double num2;
                    try {
                        num2 = parseNumber(input2);
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
                        case "%":
                            if (num2 == 0) {
                                System.out.println("Ошибка! Делить на ноль нельзя.");
                                continue;
                            }
                            res = num1 % num2;
                            break;
                        case "^": res = Math.pow(num1, num2); break;
                        default:
                            System.out.println("Ошибка! Неверная операция.");
                            continue;
                    }

                    ans = res;
                    String formattedRes = formatResult(res);
                    System.out.println("Результат: " + formattedRes);
                    history.add(String.format("%s %s %s = %s", formatResult(num1), op, formatResult(num2), formattedRes));

                } catch (NoSuchElementException e) {
                    System.out.println("\nВвод принудительно завершен. Выход из программы.");
                    break;
                }
            }

            System.out.println("Программа завершена. До свидания!");
        }
    }

    private static double parseNumber(String input) throws NumberFormatException {
        String cleanInput = input.replace(",", ".").toLowerCase();
        if (cleanInput.equals("pi")) return Math.PI;
        if (cleanInput.equals("e")) return Math.E;
        if (cleanInput.equals("ans")) return ans;
        return Double.parseDouble(cleanInput);
    }

    private static String formatResult(double val) {
        if (Double.isNaN(val)) return "NaN";
        if (Double.isInfinite(val)) return "Бесконечность";

        if (Math.abs(val) < 1e-10) {
            val = 0.0;
        }

        DecimalFormatSymbols symbols = new DecimalFormatSymbols(Locale.US);
        DecimalFormat df = new DecimalFormat("#.##########", symbols);
        return df.format(val);
    }

    private static void printHistory(List<String> history) {
        if (history.isEmpty()) {
            System.out.println("История пуста.");
        } else {
            System.out.println("--- История операций ---");
            for (int i = 0; i < history.size(); i++) {
                System.out.println((i + 1) + ". " + history.get(i));
            }
        }
    }
}
