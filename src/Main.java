import java.util.Scanner;

public class Main {

    public static void main(String[] args) {
        Scanner scanner = new Scanner(System.in);

        System.out.println("--- Добро пожаловать в расширенный калькулятор! ---");
        System.out.println("Вы можете выйти в любой момент, написав 'exit'.");

        while (true) {
            System.out.print("\nВведите первое число: ");
            String input1 = scanner.next();
            if (input1.equalsIgnoreCase("exit")) break;

            double num1;
            try {
                num1 = Double.parseDouble(input1.replace(",", "."));
            } catch (NumberFormatException e) {
                System.out.println("Ошибка! Некорректное число.");
                continue;
            }

            System.out.print("Выберите действие (+, -, *, /, ^, %): ");
            String op = scanner.next();
            if (op.equalsIgnoreCase("exit")) break;

            System.out.print("Введите второе число: ");
            String input2 = scanner.next();
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
                case "+":
                    res = num1 + num2;
                    break;
                case "-":
                    res = num1 - num2;
                    break;
                case "*":
                    res = num1 * num2;
                    break;
                case "/":
                    if (num2 == 0) {
                        System.out.println("Ошибка! Делить на ноль нельзя.");
                        continue;
                    }
                    res = num1 / num2;
                    break;
                case "^":
                    res = Math.pow(num1, num2);
                    break;
                case "%":
                    res = num1 % num2;
                    break;
                default:
                    System.out.println("Ошибка! Неверная операция.");
                    continue;
            }

            if (res % 1 == 0) {
                System.out.printf("Результат: %.0f\n", res);
            } else {
                System.out.println("Результат: " + res);
            }
        }

        System.out.println("Программа завершена. До свидания!");
        scanner.close();
    }
}
