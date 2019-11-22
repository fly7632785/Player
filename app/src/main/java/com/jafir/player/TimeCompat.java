package com.jafir.player;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

public class TimeCompat {

    private static final String[] WEEK_DAYS = {"星期日", "星期一", "星期二", "星期三", "星期四", "星期五", "星期六"};

    public static Date getDateBefore(Date d, int day) {
        Calendar now = Calendar.getInstance();
        now.setTime(d);
        now.set(Calendar.DATE, now.get(Calendar.DATE) - day);
        return now.getTime();
    }

    public static String getWeek(Date date) {
        Calendar cal = Calendar.getInstance();
        cal.setTime(date);

        int w = cal.get(Calendar.DAY_OF_WEEK) - 1;
        if (w < 0)
            w = 0;
        return WEEK_DAYS[w];
    }

    public static String getDayName(long timeMillis) {
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyyMMdd", Locale.getDefault());
        String day = dateFormat.format(timeMillis);
        if (day.equals(dateFormat.format(System.currentTimeMillis()))) {
            return "";
        } else if (day.equals(dateFormat.format(TimeCompat.getDateBefore(new Date(), 1)))) {
            return "昨天";
        } else if (day.equals(dateFormat.format(TimeCompat.getDateBefore(new Date(), 2)))) {
            return "前天";
        } else {
            return TimeCompat.getWeek(new Date(timeMillis));
        }
    }

    public static boolean isTody(long l) {
        boolean b = false;
        Date time = new Date(l);
        Date today = new Date();
        if (time != null) {
            String nowDate = new SimpleDateFormat("yyyy-MM-dd").format(today);
            String timeDate = new SimpleDateFormat("yyyy-MM-dd").format(time);
            if (nowDate.equals(timeDate)) {
                b = true;
            }
        }
        return b;
    }


    public static String secToTime(int time) {
        String timeStr = null;
        int hour = 0;
        int minute = 0;
        int second = 0;
        if (time <= 0)
            return "00:00";
        else {
            minute = time / 60;
            if (minute < 60) {
                second = time % 60;
                timeStr = unitFormat(minute) + ":" + unitFormat(second);
            } else {
                hour = minute / 60;
                if (hour > 99)
                    return "99:59:59";
                minute = minute % 60;
                second = time - hour * 3600 - minute * 60;
                timeStr = unitFormat(hour) + ":" + unitFormat(minute) + ":" + unitFormat(second);
            }
        }
        return timeStr;
    }

    public static String unitFormat(int i) {
        String retStr = null;
        if (i >= 0 && i < 10)
            retStr = "0" + Integer.toString(i);
        else
            retStr = "" + i;
        return retStr;
    }

}
