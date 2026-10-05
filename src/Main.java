import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Scanner;

public class Main {

    private static double ans = 0; // Переменная для хранения последнего результата
    private static boolean useDegrees = true; // Режим тригонометрии (true - градусы, false - радианы)

    public static void main(String[] args) {
        try (Scanner scanner = new Scanner(System.in)) {
            List<String> history = new ArrayList<>();

            showWelcomeMessage();

            while (true) {
                try {
                    System.out.print("\nВведите число/константу (или команду): ");
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
                    if (input1.equalsIgnoreCase("help")) {
                        showWelcomeMessage();
                        continue;
                    }
                    if (input1.equalsIgnoreCase("mode")) {
                        useDegrees = !useDegrees;
                        System.out.println("Режим тригонометрии изменен! Текущий: " + (useDegrees ? "ГРАДУСЫ" : "РАДИАНЫ"));
                        continue;
                    }

                    double num1;
                    try {
                        num1 = parseNumber(input1);
                    } catch (NumberFormatException e) {
                        System.out.println("Ошибка! Некорректное число или команда. Введите 'help' для справки.");
                        continue;
                    }

                    System.out.print("Выберите действие (+, -, *, /, ^, %, !, sin, cos, tan, log, log10, sqrt): ");
                    String op = scanner.nextLine().trim().toLowerCase();
                    if (op.equalsIgnoreCase("exit")) break;

                    // Унарные операции (с одним числом)
                    if (op.equals("sin") || op.equals("cos") || op.equals("tan") ||
                            op.equals("sqrt") || op.equals("log") || op.equals("log10") || op.equals("!")) {

                        double res = 0;
                        String expression = "";

                        switch (op) {
                            case "!":
                                if (num1 < 0 || num1 != Math.floor(num1)) {
                                    System.out.println("Ошибка! Факториал определен только для целых неотрицательных чисел.");
                                    continue;
                                }
                                res = factorial((int) num1);
                                expression = String.format("%s! = %s", formatResult(num1), formatResult(res));
                                break;
                            case "sin":
                                double angleSin = useDegrees ? Math.toRadians(num1) : num1;
                                res = Math.sin(angleSin);
                                expression = String.format("sin(%s%s) = %s", formatResult(num1), useDegrees ? "°" : " рад", formatResult(res));
                                break;
                            case "cos":
                                double angleCos = useDegrees ? Math.toRadians(num1) : num1;
                                res = Math.cos(angleCos);
                                expression = String.format("cos(%s%s) = %s", formatResult(num1), useDegrees ? "°" : " рад", formatResult(res));
                                break;
                            case "tan":
                                double angleTan = useDegrees ? Math.toRadians(num1) : num1;
                                if (Math.abs(Math.cos(angleTan)) < 1e-10) {
                                    System.out.println("Ошибка! Тангенс для этого угла не существует.");
                                    continue;
                                }
                                res = Math.tan(angleTan);
                                expression = String.format("tan(%s%s) = %s", formatResult(num1), useDegrees ? "°" : " рад", formatResult(res));
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
                                } // <-- Добавлено закрытие if
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
                            if (Math.abs(num2) < 1e-10) {
                                System.out.println("Ошибка! Делить на ноль нельзя.");
                                continue;
                            }
                            res = num1 / num2;
                            break;
                        case "%":
                            if (Math.abs(num2) < 1e-10) {
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

                    // Добавлено сохранение бинарной операции в историю
                    String expression = String.format("%s %s %s = %s", formatResult(num1), op, formatResult(num2), formattedRes);
                    history.add(expression);

                } catch (Exception e) {
                    System.out.println("Произошла непредвиденная ошибка: " + e.getMessage());
                }
            }
        }
    }

    // --- ДОБАВЛЕННЫЕ ВСПОМОГАТЕЛЬНЫЕ МЕТОДЫ ---

    private static void showWelcomeMessage() {
        System.out.println("=== Консольный Калькулятор ===");
        System.out.println("Доступные команды: help, history, clear, mode (смена Градусы/Радианы), exit");
        System.out.println("Константы: 'pi', 'e', а также 'ans' для использования предыдущего результата.");
    }

    private static double parseNumber(String input) throws NumberFormatException {
        if (input.equalsIgnoreCase("ans")) return ans;
        if (input.equalsIgnoreCase("pi")) return Math.PI;
        if (input.equalsIgnoreCase("e")) return Math.E;
        // Заменяем запятую на точку для универсальности ввода чисел с плавающей точкой
        return Double.parseDouble(input.replace(",", "."));
    }

    private static double factorial(int n) {
        double result = 1;
        for (int i = 1; i <= n; i++) {
            result *= i;
        }
        return result;
    }

    private static String formatResult(double value) {
        // Убираем лишние нули после запятой (например, 5.0 превращается в 5)
        DecimalFormatSymbols symbols = new DecimalFormatSymbols(Locale.US);
        DecimalFormat df = new DecimalFormat("#.##########", symbols);
        return df.format(value);
    }

    private static void printHistory(List<String> history) {


