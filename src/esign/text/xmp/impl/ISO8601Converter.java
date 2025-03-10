package esign.text.xmp.impl;

import esign.text.xmp.XMPDateTime;
import esign.text.xmp.XMPException;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.Locale;
import java.util.SimpleTimeZone;

public final class ISO8601Converter {

    public static XMPDateTime parse(String iso8601String) throws XMPException {
        return parse(iso8601String, new XMPDateTimeImpl());
    }

    public static XMPDateTime parse(String iso8601String, XMPDateTime binValue) throws XMPException {
        if (iso8601String == null) {
            throw new XMPException("Parameter must not be null", 4);
        }
        if (iso8601String.length() == 0) {
            return binValue;
        }

        ParseState input = new ParseState(iso8601String);

        if (input.ch(0) == '-') {
            input.skip();
        }

        int value = input.gatherInt("Invalid year in date string", 9999);
        if (input.hasNext() && input.ch() != '-') {
            throw new XMPException("Invalid date string, after year", 5);
        }

        if (input.ch(0) == '-') {
            value = -value;
        }
        binValue.setYear(value);
        if (!input.hasNext()) {
            return binValue;
        }
        input.skip();

        value = input.gatherInt("Invalid month in date string", 12);
        if (input.hasNext() && input.ch() != '-') {
            throw new XMPException("Invalid date string, after month", 5);
        }
        binValue.setMonth(value);
        if (!input.hasNext()) {
            return binValue;
        }
        input.skip();

        value = input.gatherInt("Invalid day in date string", 31);
        if (input.hasNext() && input.ch() != 'T') {
            throw new XMPException("Invalid date string, after day", 5);
        }
        binValue.setDay(value);
        if (!input.hasNext()) {
            return binValue;
        }
        input.skip();

        value = input.gatherInt("Invalid hour in date string", 23);
        binValue.setHour(value);
        if (!input.hasNext()) {
            return binValue;
        }

        if (input.ch() == ':') {

            input.skip();
            value = input.gatherInt("Invalid minute in date string", 59);
            if (input.hasNext() && input
                    .ch() != ':' && input.ch() != 'Z' && input.ch() != '+' && input.ch() != '-') {
                throw new XMPException("Invalid date string, after minute", 5);
            }
            binValue.setMinute(value);
        }

        if (!input.hasNext()) {
            return binValue;
        }
        if (input.hasNext() && input.ch() == ':') {

            input.skip();
            value = input.gatherInt("Invalid whole seconds in date string", 59);
            if (input.hasNext() && input.ch() != '.' && input.ch() != 'Z' && input
                    .ch() != '+' && input.ch() != '-') {
                throw new XMPException("Invalid date string, after whole seconds", 5);
            }

            binValue.setSecond(value);
            if (input.ch() == '.') {
                input.skip();
                int digits = input.pos();
                value = input.gatherInt("Invalid fractional seconds in date string", 999999999);
                if (input.hasNext() && input
                        .ch() != 'Z' && input.ch() != '+' && input.ch() != '-') {
                    throw new XMPException("Invalid date string, after fractional second", 5);
                }

                digits = input.pos() - digits;
                for (; digits > 9; digits--) {
                    value /= 10;
                }
                for (; digits < 9; digits++) {
                    value *= 10;
                }
                binValue.setNanoSecond(value);
            }

        } else if (input.ch() != 'Z' && input.ch() != '+' && input.ch() != '-') {

            throw new XMPException("Invalid date string, after time", 5);
        }

        int tzSign = 0;
        int tzHour = 0;
        int tzMinute = 0;

        if (!input.hasNext()) {

            return binValue;
        }
        if (input.ch() == 'Z') {

            input.skip();
        } else if (input.hasNext()) {

            if (input.ch() == '+') {

                tzSign = 1;
            } else if (input.ch() == '-') {

                tzSign = -1;
            } else {

                throw new XMPException("Time zone must begin with 'Z', '+', or '-'", 5);
            }

            input.skip();

            tzHour = input.gatherInt("Invalid time zone hour in date string", 23);
            if (input.hasNext()) {
                if (input.ch() == ':') {

                    input.skip();

                    tzMinute = input.gatherInt("Invalid time zone minute in date string", 59);
                } else {

                    throw new XMPException("Invalid date string, after time zone hour", 5);
                }
            }
        }

        int offset = (tzHour * 3600 * 1000 + tzMinute * 60 * 1000) * tzSign;
        binValue.setTimeZone(new SimpleTimeZone(offset, ""));

        if (input.hasNext()) {
            throw new XMPException("Invalid date string, extra chars at end", 5);
        }

        return binValue;
    }

    public static String render(XMPDateTime dateTime) {
        StringBuffer buffer = new StringBuffer();

        if (dateTime.hasDate()) {

            DecimalFormat df = new DecimalFormat("0000", new DecimalFormatSymbols(Locale.ENGLISH));
            buffer.append(df.format(dateTime.getYear()));
            if (dateTime.getMonth() == 0) {
                return buffer.toString();
            }

            df.applyPattern("'-'00");
            buffer.append(df.format(dateTime.getMonth()));
            if (dateTime.getDay() == 0) {
                return buffer.toString();
            }

            buffer.append(df.format(dateTime.getDay()));

            if (dateTime.hasTime()) {

                buffer.append('T');
                df.applyPattern("00");
                buffer.append(df.format(dateTime.getHour()));
                buffer.append(':');
                buffer.append(df.format(dateTime.getMinute()));

                if (dateTime.getSecond() != 0 || dateTime.getNanoSecond() != 0) {

                    double seconds = dateTime.getSecond() + dateTime.getNanoSecond() / 1.0E9D;

                    df.applyPattern(":00.#########");
                    buffer.append(df.format(seconds));
                }

                if (dateTime.hasTimeZone()) {

                    long timeInMillis = dateTime.getCalendar().getTimeInMillis();
                    int offset = dateTime.getTimeZone().getOffset(timeInMillis);
                    if (offset == 0) {

                        buffer.append('Z');
                    } else {

                        int thours = offset / 3600000;
                        int tminutes = Math.abs(offset % 3600000 / 60000);
                        df.applyPattern("+00;-00");
                        buffer.append(df.format(thours));
                        df.applyPattern(":00");
                        buffer.append(df.format(tminutes));
                    }
                }
            }
        }
        return buffer.toString();
    }
}
