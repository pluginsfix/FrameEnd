package pluginsfix.frameend.domain;

public final class MaskedCoordinate {
    private MaskedCoordinate() {}

    public static String mask(int value, int maskDigitsCount, char maskChar) {
        String str = String.valueOf(value);
        boolean isNegative = str.startsWith("-");
        String numberPart = isNegative ? str.substring(1) : str;

        if (numberPart.length() <= maskDigitsCount) {
            String replacement = String.valueOf(maskChar).repeat(numberPart.length());
            return isNegative ? "-" + replacement : replacement;
        }

        int visibleLength = numberPart.length() - maskDigitsCount;
        String visiblePart = numberPart.substring(0, visibleLength);
        String maskedPart = String.valueOf(maskChar).repeat(maskDigitsCount);

        return isNegative ? "-" + visiblePart + maskedPart : visiblePart + maskedPart;
    }
}
