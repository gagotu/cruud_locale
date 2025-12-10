package com.exprivia.nest.cruud.utils;

import java.math.BigDecimal;
import java.util.regex.Pattern;

/**
 * Util class used to Execute basic math operations
 */
public class Maths {

    /**
     * Method that retrieve the result of function for value
     * @param value used to elaborate result
     * @param function to apply
     * @return a function result
     */
    public static String execute(String value, String function, String alternativeValue) {

        if (function.equalsIgnoreCase("num")) {

            return Utils.getNumberOrNullString(value, alternativeValue);

        } else if (function.charAt(0) == '>') {

            return Utils.getPositiveOrNullString(value, alternativeValue);

        } else {

            if (!Pattern.matches("-?\\d+(\\.\\d+)?", value))
                return value;

            BigDecimal a = new BigDecimal(value);
            BigDecimal b = new BigDecimal(function.substring(1));

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
