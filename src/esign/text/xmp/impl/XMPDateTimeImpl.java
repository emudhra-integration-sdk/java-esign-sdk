package esign.text.xmp.impl;

import esign.text.xmp.XMPDateTime;
import esign.text.xmp.XMPException;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.Locale;
import java.util.TimeZone;

public class XMPDateTimeImpl
        implements XMPDateTime {

    private int year = 0;

    private int month = 0;

    private int day = 0;

    private int hour = 0;

    private int minute = 0;

    private int second = 0;

    private TimeZone timeZone = null;

    private int nanoSeconds;

    private boolean hasDate = false;

    private boolean hasTime = false;

    private boolean hasTimeZone = false;

    public XMPDateTimeImpl() {
    }

    public XMPDateTimeImpl(Calendar calendar) {
        Date date = calendar.getTime();
        TimeZone zone = calendar.getTimeZone();

        GregorianCalendar intCalendar = (GregorianCalendar) Calendar.getInstance(Locale.US);
        intCalendar.setGregorianChange(new Date(Long.MIN_VALUE));
        intCalendar.setTimeZone(zone);
        intCalendar.setTime(date);

        this.year = intCalendar.get(1);
        this.month = intCalendar.get(2) + 1;
        this.day = intCalendar.get(5);
        this.hour = intCalendar.get(11);
        this.minute = intCalendar.get(12);
        this.second = intCalendar.get(13);
        this.nanoSeconds = intCalendar.get(14) * 1000000;
        this.timeZone = intCalendar.getTimeZone();

        this.hasDate = this.hasTime = this.hasTimeZone = true;
    }

    public XMPDateTimeImpl(Date date, TimeZone timeZone) {
        GregorianCalendar calendar = new GregorianCalendar(timeZone);
        calendar.setTime(date);

        this.year = calendar.get(1);
        this.month = calendar.get(2) + 1;
        this.day = calendar.get(5);
        this.hour = calendar.get(11);
        this.minute = calendar.get(12);
        this.second = calendar.get(13);
        this.nanoSeconds = calendar.get(14) * 1000000;
        this.timeZone = timeZone;

        this.hasDate = this.hasTime = this.hasTimeZone = true;
    }

    public XMPDateTimeImpl(String strValue) throws XMPException {
        ISO8601Converter.parse(strValue, this);
    }

    public int getYear() {
        return this.year;
    }

    public void setYear(int year) {
        this.year = Math.min(Math.abs(year), 9999);
        this.hasDate = true;
    }

    public int getMonth() {
        return this.month;
    }

    public void setMonth(int month) {
        if (month < 1) {

            this.month = 1;
        } else if (month > 12) {

            this.month = 12;
        } else {

            this.month = month;
        }

        this.hasDate = true;
    }

    public int getDay() {
        return this.day;
    }

    public void setDay(int day) {
        if (day < 1) {

            this.day = 1;
        } else if (day > 31) {

            this.day = 31;
        } else {

            this.day = day;
        }

        this.hasDate = true;
    }

    public int getHour() {
        return this.hour;
    }

    public void setHour(int hour) {
        this.hour = Math.min(Math.abs(hour), 23);
        this.hasTime = true;
    }

    public int getMinute() {
        return this.minute;
    }

    public void setMinute(int minute) {
        this.minute = Math.min(Math.abs(minute), 59);
        this.hasTime = true;
    }

    public int getSecond() {
        return this.second;
    }

    public void setSecond(int second) {
        this.second = Math.min(Math.abs(second), 59);
        this.hasTime = true;
    }

    public int getNanoSecond() {
        return this.nanoSeconds;
    }

    public void setNanoSecond(int nanoSecond) {
        this.nanoSeconds = nanoSecond;
        this.hasTime = true;
    }

    public int compareTo(Object dt) {
        long d = getCalendar().getTimeInMillis() - ((XMPDateTime) dt).getCalendar().getTimeInMillis();
        if (d != 0L) {
            return (int) Math.signum((float) d);
        }

        d = (this.nanoSeconds - ((XMPDateTime) dt).getNanoSecond());
        return (int) Math.signum((float) d);
    }

    public TimeZone getTimeZone() {
        return this.timeZone;
    }

    public void setTimeZone(TimeZone timeZone) {
        this.timeZone = timeZone;
        this.hasTime = true;
        this.hasTimeZone = true;
    }

    public boolean hasDate() {
        return this.hasDate;
    }

    public boolean hasTime() {
        return this.hasTime;
    }

    public boolean hasTimeZone() {
        return this.hasTimeZone;
    }

    public Calendar getCalendar() {
        GregorianCalendar calendar = (GregorianCalendar) Calendar.getInstance(Locale.US);
        calendar.setGregorianChange(new Date(Long.MIN_VALUE));
        if (this.hasTimeZone) {
            calendar.setTimeZone(this.timeZone);
        }
        calendar.set(1, this.year);
        calendar.set(2, this.month - 1);
        calendar.set(5, this.day);
        calendar.set(11, this.hour);
        calendar.set(12, this.minute);
        calendar.set(13, this.second);
        calendar.set(14, this.nanoSeconds / 1000000);

        return calendar;
    }

    public String getISO8601String() {
        return ISO8601Converter.render(this);
    }

    public String toString() {
        return getISO8601String();
    }
}
