package utils;

/**
 * Created by scottreese on 10/26/19.
 *
 * Class containing useful formatting functions for dollars, percentages, and decimals.
 */
public class Utils {

    /**
     * Rounds a decimal to a specified number of digits.
     *
     * @param num The number to be rounded.
     * @param numDigits The number of digits to which the decimal is to be rounded.
     * @return The rounded decimal.
     */
    public static String roundDecimal(double num, int numDigits) {
        return String.format("%." + numDigits + "f",  num);
    }

    /**
     * Formats a decimal representing an amount of money to adhere to standard money formatting style.
     *
     * @param amount The amount of money to be formatted.
     * @return The decimal formatted to adhere to standard money formatting style.
     */
    public static String formatDollars(double amount) {
        String dollars = roundDecimal(amount, 2);
        String formattedDollarsRev = "";
        String formattedDollars = "";
        boolean isNegative = dollars.charAt(0) == '-';

        if (isNegative) {
            dollars = dollars.substring(1);
        }

        if (dollars.length() <= 6) {
            return isNegative ? "-$" + dollars : "$" + dollars;
        }

        int count = 0;
        boolean passedDecimal = false;

        for (int i = dollars.length() - 1; i >= 0; i--) {
            if (dollars.charAt(i) == '.') {
                formattedDollarsRev += '.';
                passedDecimal = true;
            } else if (!passedDecimal) {
                formattedDollarsRev += dollars.charAt(i);
            } else if (count == 3) {
                formattedDollarsRev += ',';
                formattedDollarsRev += dollars.charAt(i);
                count = 0;
            } else {
                formattedDollarsRev += dollars.charAt(i);
                count++;
            }
        }

        for (int i = formattedDollarsRev.length() - 1; i >= 0; i--) {
            formattedDollars += formattedDollarsRev.charAt(i);
        }

        return isNegative ? "-$" + formattedDollars : "$" + formattedDollars;
    }

    /**
     * Formats a decimal as a percentage, rounded to two decimal places.
     *
     * @param percentage The decimal to be formatted as a percentage.
     * @return The formatted percentage.
     */
    public static String formatPercentage(double percentage) {
        return roundDecimal(percentage, 2) + "%";
    }

}
