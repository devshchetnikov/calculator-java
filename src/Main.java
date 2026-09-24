import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Scanner;
import java.util.NoSuchElementException;

public class Main {

    public static void main(String[] args) {
        try (Scanner scanner = new Scanner(System.in)) {
            List<String> history = new ArrayList<>();

            System.out.println("--- Добро пожаловать в расширенный калькулятор! ---");
            System.out.println("Доступные команды: 'exit' - выход, 'history' - история, 'clear' - очистить историю.");
            System.out.println("Поддерживаются константы: 'pi', 'e'");
            System.out.println("Функции: +, -, *, /, ^, %, sin, cos, tan, log, log10, sqrt");

            while (true) {
                try {
                    System.out.print("\nВведите первое число/константу (или команду): ");
                    String input1 = scanner.nextLine().trim();

                    if (input1.equalsIgnoreCase("exit")) break;
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
                                // Проверка на тангенс 90, 270 и т.д. градусов с учетом погрешности double
                                if (Math.abs(Math.cos(Math.toRadians(num1))) < 1e-10) {
                                    System.out.println("Ошибка! Тангенс " + num1 + " градусов не существует (стремится к бесконечности).");
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

                        System.out.println("Результат: " + formatResult(res));
                        history.add(expression);
                        continue;
                    }

                    // Бинарные операции (требуют второе число)
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

    // Вспомогательный метод парсинга чисел и констант (Улучшение)
    private static double parseNumber(String input) throws NumberFormatException {
        String cleanInput = input.replace(",", ".").toLowerCase();
        if (cleanInput.equals("pi")) return Math.PI;
        if (cleanInput.equals("e")) return Math.E;
        return Double.parseDouble(cleanInput);
    }

    private static String formatResult(double val) {
        if (Double.isNaN(val)) return "NaN";
        if (Double.isInfinite(val)) return "Бесконечность";

        // Исправление округления Java для тригонометрии
        if (Math.abs(val) < 1e-10) {
            val = 0.0;
        }

        DecimalFormat df = new DecimalFormat("#.##########");
        df.setDecimalFormatSymbols(DecimalFormatSymbols.getInstance(Locale.US));
        return df.format(val);
    }

    // Исправлено: добавлены закрывающие скобки и вывод элементов (Исправление ошибки)
    private static void printHistory(List<String> history) {
        System.out.println("\n--- История операций ---");
        if (history.isEmpty()) {
            System.out.println("История пока пуста.");
        } else {
            for (String record : history) {
                System.out.println(record);
            }
        }
    }
}
