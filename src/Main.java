import java.util.Scanner;

public class Main {


    public static void main(String[] args) {

        Scanner scanner = new Scanner(System.in);
        System.out.print("Введите первое число: ");
        int num1 = scanner.nextInt();

        System.out.print("Введите второе число: ");
        int num2 = scanner.nextInt();

        int res;

        System.out.print("Выберите действие (+, -, *, /): ");

        scanner.nextLine();

        String op = scanner.nextLine();

        switch(op) {
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
                if(num2 == 0) {
                    System.out.print("Ошшибка делить на ноль нельзя!");
                } else {
                    res = num1 / num2;
                    System.out.print("Результат: " + res);
                }
                break;

        }

    }

}