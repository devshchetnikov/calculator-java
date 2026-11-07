// --- NEW CLASS: Fraction (добавить рядом с Complex) ---
private static class Fraction {
    private final long num;
    private final long denom;

    Fraction(long num, long denom) {
        if (denom == 0) throw new IllegalArgumentException("Division by zero");
        if (denom < 0) { num = -num; denom = -denom; }
        long g = gcd(Math.abs(num), denom);
        this.num = num / g;
        this.denom = denom / g;
    }

    Fraction add(Fraction o) { return new Fraction(num * o.denom + o.num * denom, denom * o.denom); }
    Fraction sub(Fraction o) { return new Fraction(num * o.denom - o.num * denom, denom * o.denom); }
    Fraction mul(Fraction o) { return new Fraction(num * o.num, denom * o.denom); }
    Fraction div(Fraction o) { return new Fraction(num * o.denom, denom * o.num); }

    double toDouble() { return (double) num / (double) denom; }

    private static long gcd(long a, long b) {
        while (b != 0) { long t = b; b = a % b; a = t; }
        return a;
    }

    @Override
    public String toString() {
        if (denom == 1) return Long.toString(num);
        return num + "/" + denom;
    }
}

// В методе fetch (добавить в switch):
case "ans" -> lastResult instanceof Fraction f ? f : lastResult;

// В методе resolve (добавить обработку токена дроби перед UNARY):
if (t.matches("-?\\d+/\\d+")) {
    String[] parts = t.split("/");
    stack.push(new Fraction(Long.parseLong(parts[0]), Long.parseLong(parts[1])));
    continue;
}

// В методе resolve (добавить ветку для Fraction + Fraction перед Double + Double):
if (aObj instanceof Fraction fa && bObj instanceof Fraction fb) {
    Fraction res = switch (t) {
        case "+" -> fa.add(fb);
        case "-" -> fa.sub(fb);
        case "*" -> fa.mul(fb);
        case "/" -> fa.div(fb);
        default -> throw new IllegalArgumentException("Unknown operator for fraction: " + t);
    };
    stack.push(res);
    continue;
}

// В методе resolve (расширить ветку Double + Object для поддержки Fraction):
if (aObj instanceof Double a) {
    if (bObj instanceof Fraction fb) {
        Double res = switch (t) {
            case "+" -> a + fb.toDouble();
            case "-" -> a - fb.toDouble();
            case "*" -> a * fb.toDouble();
            case "/" -> a / fb.toDouble();
            case "^" -> Math.pow(a, fb.toDouble());
            default -> throw new IllegalArgumentException("Mixed op (Double/Fraction): " + t);
        };
        stack.push(res);
        continue;
    }
    if (bObj instanceof Double b) {
        // ... существующий код для Double + Double ...
    }
}
if (aObj instanceof Fraction fa && bObj instanceof Double b) {
    Double res = switch (t) {
        case "+" -> fa.toDouble() + b;
        case "-" -> fa.toDouble() - b;
        case "*" -> fa.toDouble() * b;
        case "/" -> fa.toDouble() / b;
        case "^" -> Math.pow(fa.toDouble(), b);
        default -> throw new IllegalArgumentException("Mixed op (Fraction/Double): " + t);
    };
    stack.push(res);
    continue;
}

// В методе logResult (добавить перегрузку для Fraction):
private void logResult(String expr, Fraction f) {
    String out = f.toString();
    System.out.println("=> " + out);
    commandLog.add(expr + " = " + out);
    lastResult = f.toDouble();
}

// В методе repl() в блоке default (добавить новую команду):
} else if (line.startsWith(CMD_DEL + " ")) {
    // ... существующий код ...
} else if (line.startsWith("frac ")) {
    String[] parts = line.substring(5).trim().split("\\s+");
    if (parts.length == 2) {
        try {
            Fraction f = new Fraction(Long.parseLong(parts[0]), Long.parseLong(parts[1]));
            logResult("frac " + parts[0] + " " + parts[1], f);
        } catch (Exception ex) {
            System.err.println("! " + ex.getMessage());
        }
    } else {
        System.err.println("! Usage: frac <numerator> <denominator>");
    }
} else if (line.equalsIgnoreCase(CMD_RESET)) {
    // ... существующий код ...
}
