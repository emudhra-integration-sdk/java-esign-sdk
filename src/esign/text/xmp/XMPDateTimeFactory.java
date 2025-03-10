package esign.text.xmp;

import esign.text.xmp.impl.XMPDateTimeImpl;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.TimeZone;

public final class XMPDateTimeFactory {

    private static final TimeZone UTC = TimeZone.getTimeZone("UTC");

    public static XMPDateTime createFromCalendar(Calendar calendar) {
        return (XMPDateTime) new XMPDateTimeImpl(calendar);
    }

    public static XMPDateTime create() {
        return (XMPDateTime) new XMPDateTimeImpl();
    }

    public static XMPDateTime create(int year, int month, int day) {
        XMPDateTimeImpl xMPDateTimeImpl = new XMPDateTimeImpl();
        xMPDateTimeImpl.setYear(year);
        xMPDateTimeImpl.setMonth(month);
        xMPDateTimeImpl.setDay(day);
        return (XMPDateTime) xMPDateTimeImpl;
    }

    public static XMPDateTime create(int year, int month, int day, int hour, int minute, int second, int nanoSecond) {
        XMPDateTimeImpl xMPDateTimeImpl = new XMPDateTimeImpl();
        xMPDateTimeImpl.setYear(year);
        xMPDateTimeImpl.setMonth(month);
        xMPDateTimeImpl.setDay(day);
        xMPDateTimeImpl.setHour(hour);
        xMPDateTimeImpl.setMinute(minute);
        xMPDateTimeImpl.setSecond(second);
        xMPDateTimeImpl.setNanoSecond(nanoSecond);
        return (XMPDateTime) xMPDateTimeImpl;
    }

    public static XMPDateTime createFromISO8601(String strValue) throws XMPException {
        return (XMPDateTime) new XMPDateTimeImpl(strValue);
    }

    public static XMPDateTime getCurrentDateTime() {
        return (XMPDateTime) new XMPDateTimeImpl(new GregorianCalendar());
    }

    public static XMPDateTime setLocalTimeZone(XMPDateTime dateTime) {
        Calendar cal = dateTime.getCalendar();
        cal.setTimeZone(TimeZone.getDefault());
        return (XMPDateTime) new XMPDateTimeImpl(cal);
    }

    public static XMPDateTime convertToUTCTime(XMPDateTime dateTime) {
        long timeInMillis = dateTime.getCalendar().getTimeInMillis();
        GregorianCalendar cal = new GregorianCalendar(UTC);
        cal.setGregorianChange(new Date(Long.MIN_VALUE));
        cal.setTimeInMillis(timeInMillis);
        return (XMPDateTime) new XMPDateTimeImpl(cal);
    }

    public static XMPDateTime convertToLocalTime(XMPDateTime dateTime) {
        long timeInMillis = dateTime.getCalendar().getTimeInMillis();

        GregorianCalendar cal = new GregorianCalendar();
        cal.setTimeInMillis(timeInMillis);
        return (XMPDateTime) new XMPDateTimeImpl(cal);
    }
}
