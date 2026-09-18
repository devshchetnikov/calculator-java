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

            int num1;
            try {
                num1 = Integer.parseInt(input1);
            } catch (NumberFormatException e) {
                System.out.println("Ошибка! Введите корректное число или 'exit'.");
                continue;
            }

            System.out.print("Введите второе число: ");
            String input2 = scanner.next();
            int num2;
            try {
                num2 = Integer.parseInt(input2);
            } catch (NumberFormatException e) {
                System.out.println("Ошибка! Второе число введено неверно.");
                continue;
            }

            System.out.print("Выберите действие (+, -, *, /): ");
            String op = scanner.next();

            int res;

            switch (op) {
                case "+":
                    res = num1 + num2;
                    System.out.print("Результат: " + res);
                    break;
                case "-":
                    res = num1 - num2;
                    System.out.print("Результат: " + res);
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