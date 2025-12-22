package com.exprivia.nest.cruud.utils;

import java.math.BigDecimal;
import java.util.regex.Pattern;

/**
 * Utilità per applicare semplici trasformazioni numeriche ai valori letti dal CSV.
 * Supporta funzioni configurate nel mapping (es. num, >0, +x, *x, /x, -x).
 */
public class Maths {

    /**
     * Applica una funzione al valore testuale: può validare numeri, forzare positività
     * o eseguire operazioni aritmetiche di base. In caso di input non numerico,
     * restituisce l'alternativa prevista.
     */
    public static String execute(String value, String function, String alternativeValue) {

        if (function.equalsIgnoreCase("num")) {

            return Utils.getNumberOrNullString(value, alternativeValue);

        } else if (function.charAt(0) == '>') {

            return Utils.getPositiveOrNullString(value, alternativeValue);

        } else {

            String normalized = Utils.normalizeDecimalSeparator(value);

            if (!Pattern.matches("-?\\d+(\\.\\d+)?", normalized))
                return value;

            BigDecimal a = new BigDecimal(normalized);
            BigDecimal b = new BigDecimal(Utils.normalizeDecimalSeparator(function.substring(1)));

            switch (function.charAt(0)) {
                case '+':
                    value = String.valueOf(a.add(b));
                    break;

                case '*':
                    value = String.valueOf(a.multiply(b));
                    break;

                case '/':
                    value = String.valueOf(a.divide(b));
                    break;

                case '-':
                    value = String.valueOf(a.subtract(b));
                    break;

                default:
                    break;
            }

            return ((Double.parseDouble(value) % 1) == 0) ? Integer.valueOf( (int) Double.parseDouble(value)).toString() : value;

        }

    }

}
