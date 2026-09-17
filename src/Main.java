import java.util.Scanner;

public class Main {

    public static void main(String[] args) {
        Scanner scanner = new Scanner(System.in);

        while (true) {
            System.out.print("\nВведите первое число или напишите (exit): ");
            String input1 = scanner.next();

            if (input1.equalsIgnoreCase("exit")) {
                System.out.println("Программа завершена. До свидания!");
                break;
            }

            float num1;
            try {
                num1 = Float.parseFloat(input1);
            } catch (NumberFormatException e) {
                System.out.println("Ошибка! Введите корректное число или 'exit'.");
                continue;
            }

            System.out.print("Введите второе число: ");
            float num2 = scanner.nextFloat();

            System.out.print("Выберите действие (+, -, *, /): ");
            String op = scanner.next();

            float res;

            switch (op) {
                case "+":
                    res = num1 + num2;
                    System.out.print("Результат: " + res);
                    break;
                case "-":
                    res = num1 - num2;
                    System.out.print("Результаat: " + res);
                    break;
                case "*":
                    res = num1 * num2;
                    System.out.print("Результат: " + res);
                    break;
                case "/":
                    if (num2 == 0) {
                        System.out.print("Ошибка! Делить на ноль нельзя.");
                    } else {
                        res = num1 / num2;
                        System.out.print("Результат: " + res);
                    }
                    break;
                default:
                    System.out.print("Неверная операция!");
                    break;
            }
            System.out.println();
        }

        scanner.close();
    }
}