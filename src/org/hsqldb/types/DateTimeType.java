/* Copyright (c) 2001-2019, The HSQL Development Group
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 * Redistributions of source code must retain the above copyright notice, this
 * list of conditions and the following disclaimer.
 *
 * Redistributions in binary form must reproduce the above copyright notice,
 * this list of conditions and the following disclaimer in the documentation
 * and/or other materials provided with the distribution.
 *
 * Neither the name of the HSQL Development Group nor the names of its
 * contributors may be used to endorse or promote products derived from this
 * software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED. IN NO EVENT SHALL HSQL DEVELOPMENT GROUP, HSQLDB.ORG,
 * OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL,
 * EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO,
 * PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 * LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
 * ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */


package org.hsqldb.types;

import java.math.BigDecimal;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;

import org.hsqldb.HsqlDateTime;
import org.hsqldb.HsqlException;
import org.hsqldb.OpTypes;
import org.hsqldb.Session;
import org.hsqldb.SessionInterface;
import org.hsqldb.Tokens;
import org.hsqldb.error.Error;
import org.hsqldb.error.ErrorCode;
import org.hsqldb.lib.StringConverter;

/**
 * Type subclass for DATE, TIME and TIMESTAMP.<p>
 *
 * @author Fred Toussi (fredt@users dot sourceforge.net)
 * @version 2.5.1
 * @since 1.9.0
 */
public final class DateTimeType extends DTIType {

    public final boolean withTimeZone;
    private String       nameString;
    public static final long epochSeconds =
        HsqlDateTime.getDateSeconds("1-01-01");
    public static final TimestampData epochTimestamp =
        new TimestampData(epochSeconds);
    public static final long epochLimitSeconds =
        HsqlDateTime.getDateSeconds("10000-01-01");
    public static final TimestampData epochLimitTimestamp =
        new TimestampData(epochLimitSeconds);

    public DateTimeType(int typeGroup, int type, int scale) {

        super(typeGroup, type, 0, scale);

        withTimeZone = type == Types.SQL_TIME_WITH_TIME_ZONE
                       || type == Types.SQL_TIMESTAMP_WITH_TIME_ZONE;
        nameString = getNameStringPrivate();
    }

    public int displaySize() {

        switch (typeCode) {

            case Types.SQL_DATE :
                return 10;

            case Types.SQL_TIME :
                return 8 + (scale == 0 ? 0
                                       : scale + 1);

            case Types.SQL_TIME_WITH_TIME_ZONE :
                return 8 + (scale == 0 ? 0
                                       : scale + 1) + 6;

            case Types.SQL_TIMESTAMP :
                return 19 + (scale == 0 ? 0
                                        : scale + 1);

            case Types.SQL_TIMESTAMP_WITH_TIME_ZONE :
                return 19 + (scale == 0 ? 0
                                        : scale + 1) + 6;

            default :
                throw Error.runtimeError(ErrorCode.U_S0500, "DateTimeType");
        }
    }

    public int getJDBCTypeCode() {

        switch (typeCode) {

            case Types.SQL_TIME_WITH_TIME_ZONE :
                return Types.TIME_WITH_TIMEZONE;

            case Types.SQL_TIMESTAMP_WITH_TIME_ZONE :
                return Types.TIMESTAMP_WITH_TIMEZONE;

            default :

                // JDBC numbers happen to be the same as SQL
                return typeCode;
        }
    }

    public Class getJDBCClass() {

        switch (typeCode) {

            case Types.SQL_DATE :
                return java.sql.Date.class;

            case Types.SQL_TIME :
                return java.sql.Time.class;

            case Types.SQL_TIMESTAMP :
                return java.sql.Timestamp.class;

//#ifdef JAVA8
            case Types.SQL_TIME_WITH_TIME_ZONE :
                return java.time.OffsetTime.class;

            case Types.SQL_TIMESTAMP_WITH_TIME_ZONE :
                return java.time.OffsetDateTime.class;

//#else
/*
            case Types.SQL_TIME_WITH_TIME_ZONE :
                return java.sql.Time.class;

            case Types.SQL_TIMESTAMP_WITH_TIME_ZONE :
                return java.sql.Timestamp.class;
*/

//#endif JAVA8
            default :
                throw Error.runtimeError(ErrorCode.U_S0500, "DateTimeType");
        }
    }

    public String getJDBCClassName() {

        switch (typeCode) {

            case Types.SQL_DATE :
                return "java.sql.Date";

            case Types.SQL_TIME :
                return "java.sql.Time";

            case Types.SQL_TIMESTAMP :
                return "java.sql.Timestamp";

//#ifdef JAVA8
            case Types.SQL_TIME_WITH_TIME_ZONE :
                return "java.time.OffsetTime";

            case Types.SQL_TIMESTAMP_WITH_TIME_ZONE :
                return "java.time.OffsetDateTime";

//#else
/*
            case Types.SQL_TIME_WITH_TIME_ZONE :
                return "java.sql.Time";

            case Types.SQL_TIMESTAMP_WITH_TIME_ZONE :
                return "java.sql.Timestamp";
*/

//#endif JAVA8
            default :
                throw Error.runtimeError(ErrorCode.U_S0500, "DateTimeType");
        }
    }

    public int getJDBCPrecision() {
        return this.displaySize();
    }

    public int getSQLGenericTypeCode() {
        return Types.SQL_DATETIME;
    }

    public String getNameString() {
        return nameString;
    }

    public boolean canCompareDirect(Type otherType) {
        return typeCode == otherType.typeCode;
    }

    private String getNameStringPrivate() {

        switch (typeCode) {

            case Types.SQL_DATE :
                return Tokens.T_DATE;

            case Types.SQL_TIME :
                return Tokens.T_TIME;

            case Types.SQL_TIME_WITH_TIME_ZONE :
                return Tokens.T_TIME + ' ' + Tokens.T_WITH + ' '
                       + Tokens.T_TIME + ' ' + Tokens.T_ZONE;

            case Types.SQL_TIMESTAMP :
                return Tokens.T_TIMESTAMP;

            case Types.SQL_TIMESTAMP_WITH_TIME_ZONE :
                return Tokens.T_TIMESTAMP + ' ' + Tokens.T_WITH + ' '
                       + Tokens.T_TIME + ' ' + Tokens.T_ZONE;

            default :
                throw Error.runtimeError(ErrorCode.U_S0500, "DateTimeType");
        }
    }

    public String getDefinition() {

        String token;

        switch (typeCode) {

            case Types.SQL_DATE :
                return Tokens.T_DATE;

            case Types.SQL_TIME_WITH_TIME_ZONE :
            case Types.SQL_TIME :
                if (scale == DTIType.defaultTimeFractionPrecision) {
                    return getNameString();
                }

                token = Tokens.T_TIME;
                break;

            case Types.SQL_TIMESTAMP_WITH_TIME_ZONE :
            case Types.SQL_TIMESTAMP :
                if (scale == DTIType.defaultTimestampFractionPrecision) {
                    return getNameString();
                }

                token = Tokens.T_TIMESTAMP;
                break;

            default :
                throw Error.runtimeError(ErrorCode.U_S0500, "DateTimeType");
        }

        StringBuilder sb = new StringBuilder(16);

        sb.append(token);
        sb.append('(');
        sb.append(scale);
        sb.append(')');

        if (withTimeZone) {
            sb.append(' ' + Tokens.T_WITH + ' ' + Tokens.T_TIME + ' '
                      + Tokens.T_ZONE);
        }

        return sb.toString();
    }

    public boolean isDateTimeType() {
        return true;
    }

    public boolean isDateOrTimestampType() {

        switch (typeCode) {

            case Types.SQL_DATE :
            case Types.SQL_TIMESTAMP :
            case Types.SQL_TIMESTAMP_WITH_TIME_ZONE :
                return true;

            case Types.SQL_TIME :
            case Types.SQL_TIME_WITH_TIME_ZONE :
                return false;

            default :
                throw Error.runtimeError(ErrorCode.U_S0500, "DateTimeType");
        }
    }

    public boolean isTimestampType() {

        switch (typeCode) {

            case Types.SQL_TIMESTAMP :
            case Types.SQL_TIMESTAMP_WITH_TIME_ZONE :
                return true;

            case Types.SQL_DATE :
            case Types.SQL_TIME :
            case Types.SQL_TIME_WITH_TIME_ZONE :
                return false;

            default :
                throw Error.runtimeError(ErrorCode.U_S0500, "DateTimeType");
        }
    }

    public boolean isTimeType() {

        switch (typeCode) {

            case Types.SQL_TIMESTAMP :
            case Types.SQL_TIMESTAMP_WITH_TIME_ZONE :
            case Types.SQL_DATE :
                return false;

            case Types.SQL_TIME :
            case Types.SQL_TIME_WITH_TIME_ZONE :
                return true;

            default :
                throw Error.runtimeError(ErrorCode.U_S0500, "DateTimeType");
        }
    }

    public boolean isDateTimeTypeWithZone() {
        return withTimeZone;
    }

    public boolean acceptsFractionalPrecision() {
        return typeCode != Types.SQL_DATE;
    }

    public Type getAggregateType(Type other) {

        if (other == null) {
            return this;
        }

        if (other == SQL_ALL_TYPES) {
            return this;
        }

        // DATE with DATE returned here
        if (typeCode == other.typeCode) {
            return scale >= other.scale ? this
                                        : other;
        }

        if (other.typeCode == Types.SQL_ALL_TYPES) {
            return this;
        }

        if (other.isCharacterType()) {
            return other.getAggregateType(this);
        }

        if (!other.isDateTimeType()) {
            throw Error.error(ErrorCode.X_42562);
        }

        DateTimeType otherType = (DateTimeType) other;

        // DATE with TIME caught here
        if (otherType.startIntervalType > endIntervalType
                || startIntervalType > otherType.endIntervalType) {
            throw Error.error(ErrorCode.X_42562);
        }

        int     newType = typeCode;
        int     scale   = this.scale > otherType.scale ? this.scale
                                                       : otherType.scale;
        boolean zone    = withTimeZone || otherType.withTimeZone;
        int startType = otherType.startIntervalType > startIntervalType
                        ? startIntervalType
                        : otherType.startIntervalType;

        if (startType == Types.SQL_INTERVAL_HOUR) {
            newType = zone ? Types.SQL_TIME_WITH_TIME_ZONE
                           : Types.SQL_TIME;
        } else {
            newType = zone ? Types.SQL_TIMESTAMP_WITH_TIME_ZONE
                           : Types.SQL_TIMESTAMP;
        }

        return getDateTimeType(newType, scale);
    }

    public Type getCombinedType(Session session, Type other, int operation) {

        switch (operation) {

            case OpTypes.EQUAL :
            case OpTypes.GREATER :
            case OpTypes.GREATER_EQUAL :
            case OpTypes.SMALLER_EQUAL :
            case OpTypes.SMALLER :
            case OpTypes.NOT_EQUAL : {
                if (typeCode == other.typeCode) {
                    return this;
                }

                if (other.typeCode == Types.SQL_ALL_TYPES) {
                    return this;
                }

                if (!other.isDateTimeType()) {
                    throw Error.error(ErrorCode.X_42562);
                }

                DateTimeType otherType = (DateTimeType) other;

                // DATE with TIME caught here
                if (otherType.startIntervalType > endIntervalType
                        || startIntervalType > otherType.endIntervalType) {
                    throw Error.error(ErrorCode.X_42562);
                }

                int     newType = typeCode;
                int     scale   = this.scale > otherType.scale ? this.scale
                                                               : otherType
                                                                   .scale;
                boolean zone    = withTimeZone || otherType.withTimeZone;
                int startType = otherType.startIntervalType
                                > startIntervalType ? startIntervalType
                                                    : otherType
                                                        .startIntervalType;

                if (startType == Types.SQL_INTERVAL_HOUR) {
                    newType = zone ? Types.SQL_TIME_WITH_TIME_ZONE
                                   : Types.SQL_TIME;
                } else {
                    newType = zone ? Types.SQL_TIMESTAMP_WITH_TIME_ZONE
                                   : Types.SQL_TIMESTAMP;
                }

                return getDateTimeType(newType, scale);
            }
            case OpTypes.ADD :
            case OpTypes.SUBTRACT :
                if (other.isIntervalType()) {
                    if (typeCode != Types.SQL_DATE && other.scale > scale) {
                        return getDateTimeType(typeCode, other.scale);
                    }

                    return this;
                } else if (other.isDateTimeType()) {
                    if (operation == OpTypes.SUBTRACT) {
                        if (other.typeComparisonGroup == typeComparisonGroup) {
                            if (typeCode == Types.SQL_DATE) {
                                return Type.SQL_INTERVAL_DAY_MAX_PRECISION;
                            } else {
                                return Type
                                    .SQL_INTERVAL_DAY_TO_SECOND_MAX_PRECISION;
                            }
                        }
                    }
                } else if (other.isNumberType()) {
                    return this;
                }
                break;

            default :
        }

        throw Error.error(ErrorCode.X_42562);
    }

    public int compare(Session session, Object a, Object b) {

        long diff;

        if (a == b) {
            return 0;
        }

        if (a == null) {
            return -1;
        }

        if (b == null) {
            return 1;
        }

        switch (typeCode) {

            case Types.SQL_TIME :
            case Types.SQL_TIME_WITH_TIME_ZONE : {
                diff = ((TimeData) a).getSeconds()
                       - ((TimeData) b).getSeconds();

                if (diff == 0) {
                    diff = ((TimeData) a).getNanos()
                           - ((TimeData) b).getNanos();
                }

                return diff == 0 ? 0
                                 : diff > 0 ? 1
                                            : -1;
            }
            case Types.SQL_DATE :
            case Types.SQL_TIMESTAMP :
            case Types.SQL_TIMESTAMP_WITH_TIME_ZONE : {
                diff = ((TimestampData) a).getSeconds()
                       - ((TimestampData) b).getSeconds();

                if (diff == 0) {
                    diff = ((TimestampData) a).getNanos()
                           - ((TimestampData) b).getNanos();
                }

                return diff == 0 ? 0
                                 : diff > 0 ? 1
                                            : -1;
            }
            default :
                throw Error.runtimeError(ErrorCode.U_S0500, "DateTimeType");
        }
    }

    public Object convertToTypeLimits(SessionInterface session, Object a) {

        if (a == null) {
            return null;
        }

        switch (typeCode) {

            case Types.SQL_DATE :
                return a;

            case Types.SQL_TIME_WITH_TIME_ZONE :
            case Types.SQL_TIME : {
                TimeData ti       = (TimeData) a;
                int      nanos    = ti.getNanos();
                int      newNanos = scaleNanos(nanos);

                if (newNanos == nanos) {
                    return ti;
                }

                return new TimeData(ti.getSeconds(), newNanos, ti.getZone());
            }
            case Types.SQL_TIMESTAMP_WITH_TIME_ZONE :
            case Types.SQL_TIMESTAMP : {
                TimestampData ts       = (TimestampData) a;
                int           nanos    = ts.getNanos();
                int           newNanos = scaleNanos(nanos);

                if (ts.getSeconds() > epochLimitSeconds) {
                    throw Error.error(ErrorCode.X_22008);
                }

                if (newNanos == nanos) {
                    return ts;
                }

                return new TimestampData(ts.getSeconds(), newNanos,
                                         ts.getZone());
            }
            default :
                throw Error.runtimeError(ErrorCode.U_S0500, "DateTimeType");
        }
    }

    int scaleNanos(int nanos) {

        int divisor = nanoScaleFactors[scale];

        return (nanos / divisor) * divisor;
    }

    public Object convertToType(SessionInterface session, Object a,
                                Type otherType) {

        if (a == null) {
            return a;
        }

        switch (otherType.typeCode) {

            case Types.SQL_CLOB :
                a = a.toString();

            //fall through
            case Types.SQL_CHAR :
            case Types.SQL_VARCHAR :
                switch (this.typeCode) {

                    case Types.SQL_DATE :
                    case Types.SQL_TIME_WITH_TIME_ZONE :
                    case Types.SQL_TIME :
                    case Types.SQL_TIMESTAMP_WITH_TIME_ZONE :
                    case Types.SQL_TIMESTAMP : {
                        try {
                            return session.getScanner()
                                .convertToDatetimeInterval(session,
                                                           (String) a, this);
                        } catch (HsqlException e) {
                            return convertToDatetimeSpecial(session,
                                                            (String) a, this);
                        }
                    }
                }
                break;

            case Types.SQL_DATE :
            case Types.SQL_TIME :
            case Types.SQL_TIME_WITH_TIME_ZONE :
            case Types.SQL_TIMESTAMP :
            case Types.SQL_TIMESTAMP_WITH_TIME_ZONE :
                break;

            default :
                throw Error.error(ErrorCode.X_42561);
        }

        switch (this.typeCode) {

            case Types.SQL_DATE :
                switch (otherType.typeCode) {

                    case Types.SQL_DATE :
                        return a;

                    case Types.SQL_TIMESTAMP_WITH_TIME_ZONE : {
                        long seconds = ((TimestampData) a).getSeconds()
                                       + ((TimestampData) a).getZone();
                        long millis = HsqlDateTime.getNormalisedDate(
                            session.getCalendarGMT(), seconds * 1000);

                        return new TimestampData(millis / 1000);
                    }
                    case Types.SQL_TIMESTAMP : {
                        long l = HsqlDateTime.getNormalisedDate(
                            session.getCalendarGMT(),
                            ((TimestampData) a).getSeconds() * 1000);

                        return new TimestampData(l / 1000);
                    }
                    default :
                        throw Error.error(ErrorCode.X_42561);
                }
            case Types.SQL_TIME_WITH_TIME_ZONE :
                switch (otherType.typeCode) {

                    case Types.SQL_TIME_WITH_TIME_ZONE :
                        return convertToTypeLimits(session, a);

                    case Types.SQL_TIME : {
                        TimeData ti = (TimeData) a;

                        return new TimeData(
                            ti.getSeconds() - session.getZoneSeconds(),
                            scaleNanos(ti.getNanos()),
                            session.getZoneSeconds());
                    }
                    case Types.SQL_TIMESTAMP_WITH_TIME_ZONE : {
                        TimestampData ts = (TimestampData) a;
                        long seconds =
                            HsqlDateTime.convertToNormalisedTime(
                                ts.getSeconds() * 1000) / 1000;

                        return new TimeData((int) (seconds),
                                            scaleNanos(ts.getNanos()),
                                            ts.getZone());
                    }
                    case Types.SQL_TIMESTAMP : {
                        TimestampData ts = (TimestampData) a;
                        long seconds = ts.getSeconds()
                                       - session.getZoneSeconds();

                        seconds =
                            HsqlDateTime.convertToNormalisedTime(
                                session.getCalendarGMT(),
                                seconds * 1000) / 1000;

                        return new TimeData((int) (seconds),
                                            scaleNanos(ts.getNanos()),
                                            session.getZoneSeconds());
                    }
                    default :
                        throw Error.error(ErrorCode.X_42561);
                }
            case Types.SQL_TIME :
                switch (otherType.typeCode) {

                    case Types.SQL_TIME_WITH_TIME_ZONE : {
                        TimeData ti = (TimeData) a;

                        return new TimeData(ti.getSeconds() + ti.getZone(),
                                            scaleNanos(ti.getNanos()), 0);
                    }
                    case Types.SQL_TIME :
                        return convertToTypeLimits(session, a);

                    case Types.SQL_TIMESTAMP_WITH_TIME_ZONE : {
                        TimestampData ts      = (TimestampData) a;
                        long          seconds = ts.getSeconds() + ts.getZone();

                        seconds =
                            HsqlDateTime.convertToNormalisedTime(
                                session.getCalendarGMT(),
                                seconds * 1000) / 1000;

                        return new TimeData((int) (seconds),
                                            scaleNanos(ts.getNanos()), 0);
                    }
                    case Types.SQL_TIMESTAMP :
                        TimestampData ts = (TimestampData) a;
                        long seconds =
                            HsqlDateTime.convertToNormalisedTime(
                                session.getCalendarGMT(),
                                ts.getSeconds() * 1000) / 1000;

                        return new TimeData((int) (seconds),
                                            scaleNanos(ts.getNanos()));

                    default :
                        throw Error.error(ErrorCode.X_42561);
                }
            case Types.SQL_TIMESTAMP_WITH_TIME_ZONE :
                switch (otherType.typeCode) {

                    case Types.SQL_TIME_WITH_TIME_ZONE : {
                        TimeData ti = (TimeData) a;
                        long seconds = session.getCurrentDate().getSeconds()
                                       + ti.getSeconds();

                        return new TimestampData(seconds,
                                                 scaleNanos(ti.getNanos()),
                                                 ti.getZone());
                    }
                    case Types.SQL_TIME : {
                        TimeData ti = (TimeData) a;
                        long seconds = session.getCurrentDate().getSeconds()
                                       + ti.getSeconds()
                                       - session.getZoneSeconds();

                        return new TimestampData(seconds,
                                                 scaleNanos(ti.getNanos()),
                                                 session.getZoneSeconds());
                    }
                    case Types.SQL_TIMESTAMP_WITH_TIME_ZONE :
                        return convertToTypeLimits(session, a);

                    case Types.SQL_TIMESTAMP : {
                        TimestampData ts = (TimestampData) a;
                        long seconds = ts.getSeconds()
                                       - session.getZoneSeconds();

                        return new TimestampData(seconds,
                                                 scaleNanos(ts.getNanos()),
                                                 session.getZoneSeconds());
                    }
                    case Types.SQL_DATE : {
                        TimestampData ts = (TimestampData) a;

                        return new TimestampData(ts.getSeconds(), 0,
                                                 session.getZoneSeconds());
                    }
                    default :
                        throw Error.error(ErrorCode.X_42561);
                }
            case Types.SQL_TIMESTAMP :
                switch (otherType.typeCode) {

                    case Types.SQL_TIME_WITH_TIME_ZONE : {
                        TimeData ti = (TimeData) a;
                        long seconds = session.getCurrentDate().getSeconds()
                                       + ti.getSeconds()
                                       - session.getZoneSeconds();

                        return new TimestampData(seconds,
                                                 scaleNanos(ti.getNanos()),
                                                 session.getZoneSeconds());
                    }
                    case Types.SQL_TIME : {
                        TimeData ti = (TimeData) a;
                        long seconds = session.getCurrentDate().getSeconds()
                                       + ti.getSeconds();

                        return new TimestampData(seconds,
                                                 scaleNanos(ti.getNanos()));
                    }
                    case Types.SQL_TIMESTAMP_WITH_TIME_ZONE : {
                        TimestampData ts      = (TimestampData) a;
                        long          seconds = ts.getSeconds() + ts.getZone();

                        return new TimestampData(seconds,
                                                 scaleNanos(ts.getNanos()));
                    }
                    case Types.SQL_TIMESTAMP :
                        return convertToTypeLimits(session, a);

                    case Types.SQL_DATE :
                        return a;

                    default :
                        throw Error.error(ErrorCode.X_42561);
                }
            default :
                throw Error.runtimeError(ErrorCode.U_S0500, "DateTimeType");
        }
    }

    public Object convertToDefaultType(SessionInterface session, Object a) {

        Type otherType = a instanceof TimeData ? Type.SQL_TIME
                                               : Type.SQL_TIMESTAMP;

        return convertToType(session, a, otherType);
    }

    /** @todo - check the time zone conversion */
    public Object convertJavaToSQL(SessionInterface session, Object a) {

        if (a == null) {
            return null;
        }

        switch (typeCode) {

            case Types.SQL_TIME :
            case Types.SQL_TIME_WITH_TIME_ZONE :
                if (a instanceof java.sql.Date) {
                    break;
                }

                if (a instanceof java.util.Date) {
                    long millis;
                    int  nanos       = 0;
                    int  zoneSeconds = 0;

                    if (typeCode == Types.SQL_TIME) {
                        millis = HsqlDateTime.convertMillisFromCalendar(
                            session.getCalendarGMT(), session.getCalendar(),
                            ((java.util.Date) a).getTime());
                    } else {
                        millis      = ((java.util.Date) a).getTime();
                        zoneSeconds = session.getZoneSeconds();
                    }

                    millis = HsqlDateTime.getNormalisedTime(
                        session.getCalendarGMT(), millis);

                    if (a instanceof java.sql.Timestamp) {
                        nanos = ((java.sql.Timestamp) a).getNanos();
                        nanos = normaliseFraction(nanos, scale);
                    }

                    return new TimeData((int) millis / 1000, nanos,
                                        zoneSeconds);
                }

                TimeData time = convertJavaTimeObject(session, a);

                if (time != null) {
                    return time;
                }
                break;

            case Types.SQL_DATE : {
                if (a instanceof java.sql.Time) {
                    break;
                }

                if (a instanceof java.util.Date) {
                    long millis;
                    long seconds;

                    millis = HsqlDateTime.convertMillisFromCalendar(
                        session.getCalendarGMT(), session.getCalendar(),
                        ((java.util.Date) a).getTime());
                    millis = HsqlDateTime.getNormalisedDate(
                        session.getCalendarGMT(), millis);
                    seconds = millis / 1000;

                    if (seconds < epochSeconds
                            || seconds > epochLimitSeconds) {
                        throw Error.error(ErrorCode.X_22008);
                    }

                    return new TimestampData(seconds);
                }

                TimestampData timestamp = convertJavaTimeObject(session, a,
                    false);

                if (timestamp != null) {
                    return timestamp;
                }

                break;
            }
            case Types.SQL_TIMESTAMP :
            case Types.SQL_TIMESTAMP_WITH_TIME_ZONE : {
                if (a instanceof java.sql.Time) {
                    break;
                }

                if (a instanceof java.util.Date) {
                    long millis;
                    long seconds;
                    int  nanos       = 0;
                    int  zoneSeconds = 0;

                    if (typeCode == Types.SQL_TIMESTAMP) {
                        millis = HsqlDateTime.convertMillisFromCalendar(
                            session.getCalendarGMT(), session.getCalendar(),
                            ((java.util.Date) a).getTime());
                    } else {
                        millis = ((java.util.Date) a).getTime();
                        zoneSeconds =
                            HsqlDateTime.getZoneMillis(
                                session.getCalendar(), millis) / 1000;
                    }

                    if (a instanceof java.sql.Timestamp) {
                        nanos = ((java.sql.Timestamp) a).getNanos();
                        nanos = DateTimeType.normaliseFraction(nanos, scale);
                    }

                    seconds = millis / 1000;

                    if (seconds < epochSeconds
                            || seconds > epochLimitSeconds) {
                        throw Error.error(ErrorCode.X_22008);
                    }

                    return new TimestampData(seconds, nanos, zoneSeconds);
                }

                TimestampData timestamp = convertJavaTimeObject(session, a,
                    true);

                if (timestamp != null) {
                    return timestamp;
                }

                break;
            }
        }

        throw Error.error(ErrorCode.X_42561);
    }

//#ifdef JAVA8
    TimestampData convertJavaTimeObject(SessionInterface session, Object a, boolean timestamp) {
        if (a instanceof java.time.OffsetDateTime) {
            java.time.OffsetDateTime odt = (java.time.OffsetDateTime) a;
            long seconds = odt.toEpochSecond();
            int nanos = 0;
            int zoneSeconds = 0;

            if(timestamp) {
                nanos = odt.getNano();
                nanos = DateTimeType.normaliseFraction(nanos, scale);

                if(withTimeZone) {
                    zoneSeconds = odt.get(java.time.temporal.ChronoField.OFFSET_SECONDS);
                }
            } else {
                seconds = HsqlDateTime.getNormalisedDate(session.getCalendarGMT(),seconds * 1000) / 1000;
            }
            return new TimestampData(seconds, nanos, zoneSeconds);
        } else if (a instanceof java.time.LocalDateTime) {
            java.time.LocalDateTime odt = (java.time.LocalDateTime) a;
            int year = odt.getYear();
            int month = odt.getMonthValue() - 1;
            int day = odt.getDayOfMonth();
            int hour = 0;
            int minute = 0;
            int second = 0;
            int nanos = 0;
            int zoneSeconds = 0;

            if(timestamp) {
                hour = odt.getHour();
                minute = odt.getMinute();
                second = odt.getSecond();
                nanos = odt.getNano();
                nanos = DateTimeType.normaliseFraction(nanos, scale);

                if(withTimeZone) {
                    zoneSeconds = session.getZoneSeconds();
                }
            }

            Calendar cal = session.getCalendarGMT();
            cal.clear();
            cal.set(year, month, day, hour, minute, second);
            long seconds = cal.getTimeInMillis() / 1000;
            return new TimestampData(seconds, nanos, zoneSeconds);
        } else if (a instanceof java.time.LocalDate) {
            java.time.LocalDate odt = (java.time.LocalDate) a;
            int year = odt.getYear();
            int month = odt.getMonthValue() - 1;
            int day = odt.getDayOfMonth();
            int zoneSeconds = 0;

            if(timestamp) {
                if(withTimeZone) {
                    zoneSeconds = session.getZoneSeconds();
                }
            }

            Calendar cal = session.getCalendarGMT();
            cal.clear();

            cal.set(year, month, day);
            long seconds = cal.getTimeInMillis() / 1000;
            return new TimestampData(seconds, 0, zoneSeconds);
        }

        return null;
    }

    TimeData convertJavaTimeObject(SessionInterface session, Object a) {
        int secondsInDay = 24 * 3600;
        if (a instanceof java.time.OffsetTime) {
            java.time.OffsetTime odt = (java.time.OffsetTime) a;

            int zoneSeconds = 0;

            int hour = odt.getHour();
            int minute = odt.getMinute();
            int second = odt.getSecond();
            int fraction = odt.getNano();
            fraction = DateTimeType.normaliseFraction(fraction, scale);

            if (withTimeZone) {
                zoneSeconds = odt.get(java.time.temporal.ChronoField.OFFSET_SECONDS);
            }

            int seconds = hour * 3600 + minute * 60 + second - zoneSeconds;

            if (seconds < 0) {
                seconds += secondsInDay;
            } else if (seconds >=secondsInDay) {
                seconds -= secondsInDay;
            }

            return new TimeData(seconds, fraction, zoneSeconds);

        } else if (a instanceof java.time.LocalTime) {
            java.time.LocalTime odt = (java.time.LocalTime) a;
            long nanos = odt.toNanoOfDay();
            int seconds = (int)(nanos / 1_000_000_000);
            int fraction = (int)(nanos % 1_000_000_000);
            fraction = DateTimeType.normaliseFraction(fraction, scale);

            int zoneSeconds = 0;

            if (withTimeZone) {
                zoneSeconds = session.getZoneSeconds();
            }

            return new TimeData(seconds, fraction, zoneSeconds);
        }

        return null;
    }

//#else
/*
    TimestampData convertJavaTimeObject(SessionInterface session, Object a,
                                        boolean timestamp) {
        return null;
    }

    TimeData convertJavaTimeObject(SessionInterface session, Object a) {
        return null;
    }
*/

//#endif JAVA8
    public Object convertSQLToJavaGMT(SessionInterface session, Object a) {

        long millis;

        switch (typeCode) {

            case Types.SQL_TIME :
            case Types.SQL_TIME_WITH_TIME_ZONE :
                millis = ((TimeData) a).getSeconds() * 1000L;
                millis += ((TimeData) a).getNanos() / 1000000;

                return new java.sql.Time(millis);

            case Types.SQL_DATE :
                millis = ((TimestampData) a).getSeconds() * 1000;

                return new java.sql.Date(millis);

            case Types.SQL_TIMESTAMP :
            case Types.SQL_TIMESTAMP_WITH_TIME_ZONE :
                millis = ((TimestampData) a).getSeconds() * 1000;

                java.sql.Timestamp value = new java.sql.Timestamp(millis);

                value.setNanos(((TimestampData) a).getNanos());

                return value;

            default :
                throw Error.runtimeError(ErrorCode.U_S0500, "DateTimeType");
        }
    }

    public Object convertSQLToJava(SessionInterface session, Object a) {

        if (a == null) {
            return null;
        }

        switch (typeCode) {

            case Types.SQL_TIME : {
                Calendar cal = session.getCalendar();
                long millis = HsqlDateTime.convertMillisToCalendar(cal,
                    ((TimeData) a).getMillis());

                millis = HsqlDateTime.getNormalisedTime(cal, millis);

                java.sql.Time value = new java.sql.Time(millis);

                return value;
            }
            case Types.SQL_TIME_WITH_TIME_ZONE : {
                long millis = ((TimeData) a).getMillis();

                return new java.sql.Time(millis);
            }
            case Types.SQL_DATE : {
                Calendar cal = session.getCalendar();
                long millis = HsqlDateTime.convertMillisToCalendar(cal,
                    ((TimestampData) a).getMillis());

                // millis = HsqlDateTime.getNormalisedDate(cal, millis);
                java.sql.Date value = new java.sql.Date(millis);

                return value;
            }
            case Types.SQL_TIMESTAMP : {
                Calendar cal = session.getCalendar();
                long millis = HsqlDateTime.convertMillisToCalendar(cal,
                    ((TimestampData) a).getMillis());
                java.sql.Timestamp value = new java.sql.Timestamp(millis);

                value.setNanos(((TimestampData) a).getNanos());

                return value;
            }
            case Types.SQL_TIMESTAMP_WITH_TIME_ZONE : {
                long               millis = ((TimestampData) a).getMillis();
                java.sql.Timestamp value  = new java.sql.Timestamp(millis);

                value.setNanos(((TimestampData) a).getNanos());

                return value;
            }
            default :
                throw Error.runtimeError(ErrorCode.U_S0500, "DateTimeType");
        }
    }

    public static int normaliseTime(int seconds) {

        while (seconds < 0) {
            seconds += 24 * 60 * 60;
        }

        if (seconds >= 24 * 60 * 60) {
            seconds %= 24 * 60 * 60;
        }

        return seconds;
    }

    public String convertToString(Object a) {

        boolean       zone = false;
        String        s;
        StringBuilder sb;

        if (a == null) {
            return null;
        }

        switch (typeCode) {

            case Types.SQL_DATE :
                return HsqlDateTime.getDateString(
                    ((TimestampData) a).getSeconds());

            case Types.SQL_TIME_WITH_TIME_ZONE :
            case Types.SQL_TIME : {
                TimeData t       = (TimeData) a;
                int      seconds = normaliseTime(t.getSeconds() + t.getZone());

                s = intervalSecondToString(seconds, t.getNanos(), false);

                if (!withTimeZone) {
                    return s;
                }

                sb = new StringBuilder(s);
                s = Type.SQL_INTERVAL_HOUR_TO_MINUTE.intervalSecondToString(
                    ((TimeData) a).getZone(), 0, true);

                sb.append(s);

                return sb.toString();
            }
            case Types.SQL_TIMESTAMP_WITH_TIME_ZONE :
            case Types.SQL_TIMESTAMP : {
                TimestampData ts = (TimestampData) a;

                sb = new StringBuilder();

                HsqlDateTime.getTimestampString(sb,
                                                ts.getSeconds()
                                                + ts.getZone(), ts.getNanos(),
                                                    scale);

                if (!withTimeZone) {
                    return sb.toString();
                }

                s = Type.SQL_INTERVAL_HOUR_TO_MINUTE.intervalSecondToString(
                    ((TimestampData) a).getZone(), 0, true);

                sb.append(s);

                return sb.toString();
            }
            default :
                throw Error.runtimeError(ErrorCode.U_S0500, "DateTimeType");
        }
    }

    public String convertToSQLString(Object a) {

        if (a == null) {
            return Tokens.T_NULL;
        }

        StringBuilder sb = new StringBuilder(32);

        switch (typeCode) {

            case Types.SQL_DATE :
                sb.append(Tokens.T_DATE);
                break;

            case Types.SQL_TIME_WITH_TIME_ZONE :
            case Types.SQL_TIME :
                sb.append(Tokens.T_TIME);
                break;

            case Types.SQL_TIMESTAMP_WITH_TIME_ZONE :
            case Types.SQL_TIMESTAMP :
                sb.append(Tokens.T_TIMESTAMP);
                break;
        }

        sb.append(StringConverter.toQuotedString(convertToString(a), '\'',
                false));

        return sb.toString();
    }

    public boolean canConvertFrom(Type otherType) {

        if (otherType.typeCode == Types.SQL_ALL_TYPES) {
            return true;
        }

        if (otherType.isCharacterType()) {
            return true;
        }

        if (!otherType.isDateTimeType()) {
            return false;
        }

        if (otherType.typeCode == Types.SQL_DATE) {
            return typeCode != Types.SQL_TIME;
        } else if (otherType.typeCode == Types.SQL_TIME) {
            return typeCode != Types.SQL_DATE;
        }

        return true;
    }

    public int canMoveFrom(Type otherType) {

        if (otherType == this) {
            return 0;
        }

        if (typeCode == otherType.typeCode) {
            return scale >= otherType.scale ? 0
                                            : -1;
        }

        return -1;
    }

    public Object add(Session session, Object a, Object b, Type otherType) {

        if (a == null || b == null) {
            return null;
        }

        if (otherType.isNumberType()) {
            if (typeCode == Types.SQL_DATE) {
                b = ((NumberType) otherType).floor(b);
            }

            b = Type.SQL_INTERVAL_SECOND_MAX_PRECISION.multiply(
                IntervalSecondData.oneDay, b);
        }

        switch (typeCode) {

            /** @todo -  range checks for units added */
            case Types.SQL_TIME_WITH_TIME_ZONE :
            case Types.SQL_TIME :
                if (b instanceof IntervalMonthData) {
                    throw Error.runtimeError(ErrorCode.U_S0500,
                                             "DateTimeType");
                } else if (b instanceof IntervalSecondData) {
                    return addSeconds((TimeData) a,
                                      ((IntervalSecondData) b).units,
                                      ((IntervalSecondData) b).nanos);
                }
                break;

            case Types.SQL_DATE :
            case Types.SQL_TIMESTAMP_WITH_TIME_ZONE :
            case Types.SQL_TIMESTAMP :
                if (b instanceof IntervalMonthData) {
                    return addMonths(session, (TimestampData) a,
                                     ((IntervalMonthData) b).units);
                } else if (b instanceof IntervalSecondData) {
                    return addSeconds((TimestampData) a,
                                      ((IntervalSecondData) b).units,
                                      ((IntervalSecondData) b).nanos);
                }
                break;

            default :
        }

        throw Error.runtimeError(ErrorCode.U_S0500, "DateTimeType");
    }

    public Object subtract(Session session, Object a, Object b,
                           Type otherType) {

        if (a == null || b == null) {
            return null;
        }

        if (otherType.isNumberType()) {
            if (typeCode == Types.SQL_DATE) {
                b = ((NumberType) otherType).floor(b);
            }

            b = Type.SQL_INTERVAL_SECOND_MAX_PRECISION.multiply(
                IntervalSecondData.oneDay, b);
        }

        switch (typeCode) {

            case Types.SQL_TIME_WITH_TIME_ZONE :
            case Types.SQL_TIME :
                if (b instanceof IntervalMonthData) {
                    throw Error.runtimeError(ErrorCode.U_S0500,
                                             "DateTimeType");
                } else if (b instanceof IntervalSecondData) {
                    return addSeconds((TimeData) a,
                                      -((IntervalSecondData) b).units,
                                      -((IntervalSecondData) b).nanos);
                }
                break;

            case Types.SQL_DATE :
            case Types.SQL_TIMESTAMP_WITH_TIME_ZONE :
            case Types.SQL_TIMESTAMP :
                if (b instanceof IntervalMonthData) {
                    return addMonths(session, (TimestampData) a,
                                     -((IntervalMonthData) b).units);
                } else if (b instanceof IntervalSecondData) {
                    return addSeconds((TimestampData) a,
                                      -((IntervalSecondData) b).units,
                                      -((IntervalSecondData) b).nanos);
                }
                break;

            default :
        }

        throw Error.runtimeError(ErrorCode.U_S0500, "DateTimeType");
    }

    public static double convertToDouble(Object a) {

        double seconds;
        double fraction;

        if (a instanceof TimeData) {
            seconds  = ((TimeData) a).getSeconds();
            fraction = ((TimeData) a).getNanos() / 1000000000d;
        } else {
            seconds  = ((TimestampData) a).getSeconds();
            fraction = ((TimestampData) a).getNanos() / 1000000000d;
        }

        return seconds + fraction;
    }

    public Object convertFromDouble(Session session, double value) {

        long units = (long) value;
        int  nanos = (int) ((value - units) * limitNanoseconds);

        return getValue(session, units, nanos, 0);
    }

    public Object truncate(Session session, Object a, int part) {

        if (a == null) {
            return null;
        }

        long     millis   = getMillis(a);
        Calendar calendar = session.getCalendarGMT();

        millis = HsqlDateTime.getTruncatedPart(calendar, millis, part);
        millis -= getZoneMillis(a);

        switch (typeCode) {

            case Types.SQL_TIME_WITH_TIME_ZONE :
                millis = HsqlDateTime.getNormalisedTime(calendar, millis);

            //fall through
            case Types.SQL_TIME : {
                return new TimeData((int) (millis / 1000), 0,
                                    ((TimeData) a).getZone());
            }
            case Types.SQL_DATE :
            case Types.SQL_TIMESTAMP_WITH_TIME_ZONE :
            case Types.SQL_TIMESTAMP : {
                return new TimestampData(millis / 1000, 0,
                                         ((TimestampData) a).getZone());
            }
            default :
        }

        throw Error.runtimeError(ErrorCode.U_S0500, "DateTimeType");
    }

    public Object round(Session session, Object a, int part) {

        if (a == null) {
            return null;
        }

        long     millis   = getMillis(a);
        Calendar calendar = session.getCalendarGMT();

        millis = HsqlDateTime.getRoundedPart(calendar, millis, part);
        millis -= getZoneMillis(a);

        switch (typeCode) {

            case Types.SQL_TIME_WITH_TIME_ZONE :
            case Types.SQL_TIME : {
                millis = HsqlDateTime.getNormalisedTime(millis);

                return new TimeData((int) (millis / 1000), 0,
                                    ((TimeData) a).getZone());
            }
            case Types.SQL_DATE :
            case Types.SQL_TIMESTAMP_WITH_TIME_ZONE :
            case Types.SQL_TIMESTAMP : {
                return new TimestampData(millis / 1000, 0,
                                         ((TimestampData) a).getZone());
            }
            default :
        }

        throw Error.runtimeError(ErrorCode.U_S0500, "DateTimeType");
    }

    public boolean equals(Object other) {

        if (other == this) {
            return true;
        }

        if (other instanceof DateTimeType) {
            return super.equals(other)
                   && ((DateTimeType) other).withTimeZone == withTimeZone;
        }

        return false;
    }

    public int getPart(Session session, Object dateTime, int part) {

        int calendarPart;
        int increment = 0;
        int divisor   = 1;

        switch (part) {

            case Types.SQL_INTERVAL_YEAR :
                calendarPart = Calendar.YEAR;
                break;

            case Types.SQL_INTERVAL_MONTH :
                increment    = 1;
                calendarPart = Calendar.MONTH;
                break;

            case Types.SQL_INTERVAL_DAY :
            case Types.DTI_DAY_OF_MONTH :
                calendarPart = Calendar.DAY_OF_MONTH;
                break;

            case Types.SQL_INTERVAL_HOUR :
                calendarPart = Calendar.HOUR_OF_DAY;
                break;

            case Types.SQL_INTERVAL_MINUTE :
                calendarPart = Calendar.MINUTE;
                break;

            case Types.SQL_INTERVAL_SECOND :
                calendarPart = Calendar.SECOND;
                break;

            case Types.DTI_DAY_OF_WEEK :
                calendarPart = Calendar.DAY_OF_WEEK;
                break;

            case Types.DTI_WEEK_OF_YEAR :
                calendarPart = Calendar.WEEK_OF_YEAR;
                break;

            case Types.DTI_SECONDS_MIDNIGHT : {
                if (typeCode == Types.SQL_TIME
                        || typeCode == Types.SQL_TIME_WITH_TIME_ZONE) {}
                else {
                    try {
                        Type target = withTimeZone
                                      ? Type.SQL_TIME_WITH_TIME_ZONE
                                      : Type.SQL_TIME;

                        dateTime = target.castToType(session, dateTime, this);
                    } catch (HsqlException e) {}
                }

                return ((TimeData) dateTime).getSeconds();
            }
            case Types.DTI_TIMEZONE_HOUR :
                if (typeCode == Types.SQL_TIMESTAMP_WITH_TIME_ZONE) {
                    return ((TimestampData) dateTime).getZone() / 3600;
                } else {
                    return ((TimeData) dateTime).getZone() / 3600;
                }
            case Types.DTI_TIMEZONE_MINUTE :
                if (typeCode == Types.SQL_TIMESTAMP_WITH_TIME_ZONE) {
                    return ((TimestampData) dateTime).getZone() / 60 % 60;
                } else {
                    return ((TimeData) dateTime).getZone() / 60 % 60;
                }
            case Types.DTI_TIMEZONE :
                if (typeCode == Types.SQL_TIMESTAMP_WITH_TIME_ZONE) {
                    return ((TimestampData) dateTime).getZone() / 60;
                } else {
                    return ((TimeData) dateTime).getZone() / 60;
                }
            case Types.DTI_QUARTER :
                increment    = 1;
                divisor      = 3;
                calendarPart = Calendar.MONTH;
                break;

            case Types.DTI_DAY_OF_YEAR :
                calendarPart = Calendar.DAY_OF_YEAR;
                break;

            case Types.DTI_MILLISECOND :
                if (this.isDateOrTimestampType()) {
                    return ((TimestampData) dateTime).getNanos() / 1000000;
                } else {
                    return ((TimeData) dateTime).getNanos() / 1000000;
                }
            case Types.DTI_NANOSECOND :
                if (this.isDateOrTimestampType()) {
                    return ((TimestampData) dateTime).getNanos();
                } else {
                    return ((TimeData) dateTime).getNanos();
                }
            default :
                throw Error.runtimeError(ErrorCode.U_S0500,
                                         "DateTimeType - " + part);
        }

        long millis = getMillis(dateTime);

        return HsqlDateTime.getDateTimePart(session.getCalendarGMT(), millis, calendarPart)
               / divisor + increment;
    }

    public Object addMonthsSpecial(Session session, Object dateTime,
                                   int months) {

        TimestampData ts     = (TimestampData) dateTime;
        Calendar      cal    = session.getCalendarGMT();
        long          millis = (ts.getSeconds() + ts.getZone()) * 1000;
        boolean       lastDay;

        HsqlDateTime.setTimeInMillis(cal, millis);
        cal.set(Calendar.DAY_OF_MONTH, 1);
        cal.add(Calendar.MONTH, 1);
        cal.add(Calendar.DAY_OF_MONTH, -1);

        lastDay = millis == cal.getTimeInMillis();

        HsqlDateTime.setTimeInMillis(cal, millis);
        cal.add(Calendar.MONTH, months);

        if (lastDay) {
            cal.set(Calendar.DAY_OF_MONTH, 1);
            cal.add(Calendar.MONTH, 1);
            cal.add(Calendar.DAY_OF_MONTH, -1);
        }

        millis = cal.getTimeInMillis();

        return new TimestampData(millis / 1000, 0, 0);
    }

    public Object getLastDayOfMonth(Session session, Object dateTime) {

        TimestampData ts     = (TimestampData) dateTime;
        Calendar      cal    = session.getCalendarGMT();
        long          millis = (ts.getSeconds() + ts.getZone()) * 1000;

        HsqlDateTime.setTimeInMillis(cal, millis);
        cal.set(Calendar.DAY_OF_MONTH, 1);
        cal.add(Calendar.MONTH, 1);
        cal.add(Calendar.DAY_OF_MONTH, -1);

        millis = cal.getTimeInMillis();

        return new TimestampData(millis / 1000, 0, 0);
    }

    long getMillis(Object dateTime) {

        long millis;

        if (typeCode == Types.SQL_TIME
                || typeCode == Types.SQL_TIME_WITH_TIME_ZONE) {
            millis =
                (((TimeData) dateTime).getSeconds() + ((TimeData) dateTime)
                    .getZone()) * 1000L;
        } else {
            millis =
                (((TimestampData) dateTime)
                    .getSeconds() + ((TimestampData) dateTime).getZone()) * 1000;
        }

        return millis;
    }

    long getZoneMillis(Object dateTime) {

        long millis;

        if (typeCode == Types.SQL_TIME
                || typeCode == Types.SQL_TIME_WITH_TIME_ZONE) {
            millis = ((TimeData) dateTime).getZone() * 1000L;
        } else {
            millis = ((TimestampData) dateTime).getZone() * 1000L;
        }

        return millis;
    }

    public BigDecimal getSecondPart(Session session, Object dateTime) {

        long seconds = getPart(session, dateTime, Types.SQL_INTERVAL_SECOND);
        int  nanos   = 0;

        if (typeCode == Types.SQL_TIMESTAMP
                || typeCode == Types.SQL_TIMESTAMP_WITH_TIME_ZONE) {
            nanos = ((TimestampData) dateTime).getNanos();
        } else if (typeCode == Types.SQL_TIME
                   || typeCode == Types.SQL_TIME_WITH_TIME_ZONE) {
            nanos = ((TimeData) dateTime).getNanos();
        }

        return getSecondPart(seconds, nanos);
    }

    public String getPartString(Session session, Object dateTime, int part) {

        String javaPattern = "";

        switch (part) {

            case Types.DTI_DAY_NAME :
                javaPattern = "EEEE";
                break;

            case Types.DTI_MONTH_NAME :
                javaPattern = "MMMM";
                break;
        }

        SimpleDateFormat format = session.getSimpleDateFormatGMT();

        try {
            format.applyPattern(javaPattern);
        } catch (Exception e) {}

        Date date = (Date) convertSQLToJavaGMT(session, dateTime);

        return format.format(date);
    }

    public Object getValue(Session session, long seconds, int nanos,
                           int zoneSeconds) {

        Calendar calendar = session.getCalendarGMT();

        switch (typeCode) {

            case Types.SQL_DATE :
                seconds =
                    HsqlDateTime.getNormalisedDate(
                        calendar, (seconds + zoneSeconds) * 1000) / 1000;

                return new TimestampData(seconds);

            case Types.SQL_TIME_WITH_TIME_ZONE :
                seconds =
                    HsqlDateTime.getNormalisedDate(calendar, seconds * 1000)
                    / 1000;

                return new TimeData((int) seconds, nanos, zoneSeconds);

            case Types.SQL_TIME :
                seconds =
                    HsqlDateTime.getNormalisedTime(
                        calendar, (seconds + zoneSeconds) * 1000) / 1000;

                return new TimeData((int) seconds, nanos);

            case Types.SQL_TIMESTAMP_WITH_TIME_ZONE :
                return new TimestampData(seconds, nanos, zoneSeconds);

            case Types.SQL_TIMESTAMP :
                return new TimestampData(seconds + zoneSeconds, nanos);

            default :
                throw Error.runtimeError(ErrorCode.U_S0500, "DateTimeType");
        }
    }

    public DateTimeType getDateTimeTypeWithoutZone() {

        if (this.withTimeZone) {
            DateTimeType type;

            switch (typeCode) {

                case Types.SQL_TIME_WITH_TIME_ZONE :
                    type = new DateTimeType(Types.SQL_TIME, Types.SQL_TIME,
                                            scale);
                    break;

                case Types.SQL_TIMESTAMP_WITH_TIME_ZONE :
                    type = new DateTimeType(Types.SQL_TIMESTAMP,
                                            Types.SQL_TIMESTAMP, scale);
                    break;

                default :
                    throw Error.runtimeError(ErrorCode.U_S0500,
                                             "DateTimeType");
            }

            type.nameString = nameString;

            return type;
        }

        return this;
    }

    public static DateTimeType getDateTimeType(int type, int scale) {

        if (scale > DTIType.maxFractionPrecision) {
            throw Error.error(ErrorCode.X_42592);
        }

        switch (type) {

            case Types.SQL_DATE :
                return SQL_DATE;

            case Types.SQL_TIME :
                if (scale == DTIType.defaultTimeFractionPrecision) {
                    return SQL_TIME;
                }

                return new DateTimeType(Types.SQL_TIME, type, scale);

            case Types.SQL_TIME_WITH_TIME_ZONE :
                if (scale == DTIType.defaultTimeFractionPrecision) {
                    return SQL_TIME_WITH_TIME_ZONE;
                }

                return new DateTimeType(Types.SQL_TIME, type, scale);

            case Types.SQL_TIMESTAMP :
                if (scale == DTIType.defaultTimestampFractionPrecision) {
                    return SQL_TIMESTAMP;
                }

                if (scale == 0) {
                    return SQL_TIMESTAMP_NO_FRACTION;
                }

                return new DateTimeType(Types.SQL_TIMESTAMP, type, scale);

            case Types.SQL_TIMESTAMP_WITH_TIME_ZONE :
                if (scale == DTIType.defaultTimestampFractionPrecision) {
                    return SQL_TIMESTAMP_WITH_TIME_ZONE;
                }

                return new DateTimeType(Types.SQL_TIMESTAMP, type, scale);

            default :
                throw Error.runtimeError(ErrorCode.U_S0500, "DateTimeType");
        }
    }

    public static Object changeZoneToUTC(Object a) {

        if (a instanceof TimestampData) {
            TimestampData ts = (TimestampData) a;

            if (ts.getZone() != 0) {
                return new TimestampData(ts.seconds, ts.nanos);
            }
        }

        if (a instanceof TimeData) {
            TimeData ts = (TimeData) a;

            if (ts.getZone() != 0) {
                return new TimeData(ts.seconds, ts.nanos);
            }
        }

        return a;
    }

    public Object changeZone(Session session, Object a, Type otherType,
                             int targetZone, int localZone) {

        Calendar calendar = session.getCalendarGMT();

        if (a == null) {
            return null;
        }

        if (targetZone > DTIType.timezoneSecondsLimit
                || -targetZone > DTIType.timezoneSecondsLimit) {
            throw Error.error(ErrorCode.X_22009);
        }

        switch (typeCode) {

            case Types.SQL_TIME_WITH_TIME_ZONE : {
                TimeData value = (TimeData) a;

                if (otherType.isDateTimeTypeWithZone()) {
                    if (value.zone != targetZone) {
                        return new TimeData(value.getSeconds(),
                                            value.getNanos(), targetZone);
                    }
                } else {
                    int seconds = value.getSeconds() - localZone;

                    seconds =
                        (int) (HsqlDateTime.getNormalisedTime(calendar, seconds * 1000L)
                               / 1000);

                    return new TimeData(seconds, value.getNanos(), targetZone);
                }

                break;
            }
            case Types.SQL_TIMESTAMP_WITH_TIME_ZONE : {
                TimestampData value   = (TimestampData) a;
                long          seconds = value.getSeconds();

                if (!otherType.isDateTimeTypeWithZone()) {
                    seconds -= localZone;
                }

                if (value.getSeconds() != seconds
                        || value.zone != targetZone) {
                    return new TimestampData(seconds, value.getNanos(),
                                             targetZone);
                }

                break;
            }
        }

        return a;
    }

    public boolean canAdd(IntervalType other) {
        return other.startPartIndex >= startPartIndex
               && other.endPartIndex <= endPartIndex;
    }

    public int getSqlDateTimeSub() {

        switch (typeCode) {

            case Types.SQL_DATE :
                return 1;

            case Types.SQL_TIME :
                return 2;

            case Types.SQL_TIMESTAMP :
                return 3;

            default :
                return 0;
        }
    }

    /**
     * For temporal predicate operations on periods, we need to make sure we
     * compare data of the same types.
     * <p>
     *
     * @param session
     * @param a First period to compare
     * @param ta Types of the first period
     * @param b Second period to compare
     * @param tb Type of the second period
     *
     * @return The common data type of the boundaries of the two limits.
     *         null if any of the two periods is null or if the first limit of
     *         any period is null.
     *
     * @since 2.3.4
     */
    public static Type normalizeInput(Session session, Object[] a, Type[] ta,
                                      Object[] b, Type[] tb,
                                      boolean pointOfTime) {

        if (a == null || b == null) {
            return null;
        }

        if (a[0] == null || b[0] == null) {
            return null;
        }

        if (a[1] == null) {
            return null;
        }

        if (!pointOfTime && b[1] == null) {
            return null;
        }

        Type commonType = SQL_TIMESTAMP_WITH_TIME_ZONE;

        a[0] = commonType.castToType(session, a[0], ta[0]);
        b[0] = commonType.castToType(session, b[0], tb[0]);

        if (ta[1].isIntervalType()) {
            a[1] = commonType.add(session, a[0], a[1], ta[1]);
        } else {
            a[1] = commonType.castToType(session, a[1], ta[1]);
        }

        if (tb[1].isIntervalType()) {
            b[1] = commonType.add(session, b[0], b[1], tb[1]);
        } else {
            if (pointOfTime) {
                b[1] = b[0];
            } else {
                b[1] = commonType.castToType(session, b[1], tb[1]);
            }
        }

        if (commonType.compare(session, a[0], a[1]) >= 0) {
            Object temp = a[0];

            a[0] = a[1];
            a[1] = temp;
        }

        if (!pointOfTime && commonType.compare(session, b[0], b[1]) >= 0) {
            Object temp = b[0];

            b[0] = b[1];
            b[1] = temp;
        }

        return commonType;
    }

    /**
     * For temporal predicate operations on periods, we need to make sure we
     * compare data of the same types.
     * We also switch the period boundaries if the first entry is after the
     * second one.
     * <p>
     * Important: when this method returns, the boundaries of the periods may
     * have been changed.
     *
     * @param session
     * @param a First period to compare
     * @param ta Types of the first period
     * @param b Second period to compare
     * @param tb Type of the second period
     *
     * @return The common data type of the boundaries of the two limits.
     *         null if any of the two periods is null or if the first limit of
     *         any period is null.
     *
     * @since 2.3.4
     */
    public static Type normalizeInputRelaxed(Session session, Object[] a,
            Type[] ta, Object[] b, Type[] tb) {

        if (a == null || b == null) {
            return null;
        }

        if (a[0] == null || b[0] == null) {
            return null;
        }

        if (a[1] == null) {
            a[1] = a[0];
        }

        if (b[1] == null) {
            b[1] = b[0];
        }

        Type commonType = ta[0].getCombinedType(session, tb[0], OpTypes.EQUAL);

        a[0] = commonType.castToType(session, a[0], ta[0]);
        b[0] = commonType.castToType(session, b[0], tb[0]);

        if (ta[1].isIntervalType()) {
            a[1] = commonType.add(session, a[0], a[1], ta[1]);
        } else {
            a[1] = commonType.castToType(session, a[1], ta[1]);
        }

        if (tb[1].isIntervalType()) {
            b[1] = commonType.add(session, b[0], b[1], tb[1]);
        } else {
            b[1] = commonType.castToType(session, b[1], tb[1]);
        }

        if (commonType.compare(session, a[0], a[1]) > 0) {
            Object temp = a[0];

            a[0] = a[1];
            a[1] = temp;
        }

        if (commonType.compare(session, b[0], b[1]) > 0) {
            Object temp = b[0];

            b[0] = b[1];
            b[1] = temp;
        }

        return commonType;
    }

    /**
     * The predicate "a OVERLAPS b" applies when both a and b are either period
     * names or period constructors.
     * This predicate returns True if the two periods have at least one time
     * point in common, i.e, if a[0] < b[1] and
     * a[1] > b[0]. This predicates is commutative: "a OVERLAPS B" must return
     * the same result of "b OVERLAPS a"
     * <p>
     *
     * @param session
     * @param a First period to compare
     * @param ta Types of the first period
     * @param b Second period to compare
     * @param tb Type of the second period
     *
     * @return {@link Boolean#TRUE} if the two periods overlaps,
     *          else {@link Boolean#FALSE}
     */
    public static Boolean overlaps(Session session, Object[] a, Type[] ta,
                                   Object[] b, Type[] tb) {

        Type commonType = normalizeInput(session, a, ta, b, tb, false);

        if (commonType == null) {
            return null;
        }

        if (commonType.compare(session, a[0], b[0]) > 0) {
            Object[] temp = a;

            a = b;
            b = temp;
        }

        if (commonType.compare(session, a[1], b[0]) > 0) {
            return Boolean.TRUE;
        }

        return Boolean.FALSE;
    }

    /**
     * The predicate "a OVERLAPS b" applies when both a and b are rows.
     * This predicate returns True if the two periods have at least one time
     * point in common, i.e, if a[0] < b[1] and
     * a[1] > b[0]. This predicates is commutative: "a OVERLAPS B" must return
     * the same result of "b OVERLAPS a"
     * <p>
     * Important: when this method returns, the boundaries of the periods may
     * have been changed.
     *
     * @param session
     * @param a First period to compare
     * @param ta Types of the first period
     * @param b Second period to compare
     * @param tb Type of the second period
     *
     * @return {@link Boolean#TRUE} if the two periods overlaps,
     *          else {@link Boolean#FALSE}
     */
    public static Boolean overlapsRelaxed(Session session, Object[] a,
                                          Type[] ta, Object[] b, Type[] tb) {

        Type commonType = normalizeInputRelaxed(session, a, ta, b, tb);

        if (commonType == null) {
            return null;
        }

        if (commonType.compare(session, a[0], b[0]) > 0) {
            Object[] temp = a;

            a = b;
            b = temp;
        }

        if (commonType.compare(session, a[1], b[0]) > 0) {
            return Boolean.TRUE;
        }

        return Boolean.FALSE;
    }

    /**
     * The predicate "a PRECEDES b" applies when both a and b are either period
     * names or period constructors.
     * In this case, the predicate returns True if the end value of a is less
     * than or equal to the start value of b, i.e., if ae <= as.
     * <p>
     *
     * @param session
     * @param a First period to compare
     * @param ta Types of the first period
     * @param b Second period to compare
     * @param tb Type of the second period
     *
     * @return {@link Boolean#TRUE} if period a precedes period b,
     *          else {@link Boolean#FALSE}
     */
    public static Boolean precedes(Session session, Object[] a, Type[] ta,
                                   Object[] b, Type[] tb) {

        Type commonType = normalizeInput(session, a, ta, b, tb, false);

        if (commonType == null) {
            return null;
        }

        if (commonType.compare(session, a[1], b[0]) <= 0) {
            return Boolean.TRUE;
        }

        return Boolean.FALSE;
    }

    /**
     * The predicate "x IMMEDIATELY PRECEDES y" applies when both x and y are either period names or
     * period constructors. In this case, the predicate returns True if the end value of x is equal to the start value
     * of y, i.e., if xe = ys.
     * <p>
     *
     * @param session
     * @param a First period to compare
     * @param ta Types of the first period
     * @param b Second period to compare
     * @param tb Type of the second period
     *
     * @return {@link Boolean#TRUE} if period a immediately precedes period b,
     *          else {@link Boolean#FALSE}
     */
    public static Boolean immediatelyPrecedes(Session session, Object[] a,
            Type[] ta, Object[] b, Type[] tb) {

        Type commonType = normalizeInput(session, a, ta, b, tb, false);

        if (commonType == null) {
            return null;
        }

        if (commonType.compare(session, a[1], b[0]) == 0) {
            return Boolean.TRUE;
        }

        return Boolean.FALSE;
    }

    /**
     * The predicate "x IMMEDIATELY SUCCEEDS y" applies when both x and y are either period names or
     * period constructors. In this case, the predicate returns True if the start value of x is equal to the end value
     * of y, i.e., if xs = ye.
     * <p>
     *
     * @param session
     * @param a First period to compare
     * @param ta Types of the first period
     * @param b Second period to compare
     * @param tb Type of the second period
     *
     * @return {@link Boolean#TRUE} if period a immediately succeeds period b,
     *          else {@link Boolean#FALSE}
     */
    public static Boolean immediatelySucceeds(Session session, Object[] a,
            Type[] ta, Object[] b, Type[] tb) {

        Type commonType = normalizeInput(session, a, ta, b, tb, false);

        if (commonType == null) {
            return null;
        }

        if (commonType.compare(session, a[0], b[1]) == 0) {
            return Boolean.TRUE;
        }

        return Boolean.FALSE;
    }

    /**
     * The predicate "x SUCCEEDS y" applies when both x and y are either period names or period constructors.
     * In this case, the predicate returns True if the start value of x is greater than or equal to the end value of y,
     * i.e., if xs >= ye.
     * <p>
     *
     * @param session
     * @param a First period to compare
     * @param ta Types of the first period
     * @param b Second period to compare
     * @param tb Type of the second period
     *
     * @return {@link Boolean#TRUE} if period a succeeds period b,
     *          else {@link Boolean#FALSE}
     */
    public static Boolean succeeds(Session session, Object[] a, Type[] ta,
                                   Object[] b, Type[] tb) {

        Type commonType = normalizeInput(session, a, ta, b, tb, false);

        if (commonType == null) {
            return null;
        }

        if (commonType.compare(session, a[0], b[1]) >= 0) {
            return Boolean.TRUE;
        }

        return Boolean.FALSE;
    }

    /**
     * The predicate "x EQUALS y" applies when both x and y are either period names or period constructors.
     * This predicate returns True if the two periods have every time point in common, i.e., if xs = ys and xe = ye.
     * <p>
     *
     * @param session
     * @param a First period to compare
     * @param ta Types of the first period
     * @param b Second period to compare
     * @param tb Type of the second period
     *
     * @return {@link Boolean#TRUE} if period a equals period b,
     *          else {@link Boolean#FALSE}
     */
    public static Boolean equals(Session session, Object[] a, Type[] ta,
                                 Object[] b, Type[] tb) {

        Type commonType = normalizeInput(session, a, ta, b, tb, false);

        if (commonType == null) {
            return null;
        }

        if (commonType.compare(session, a[0], b[0]) == 0
                && commonType.compare(session, a[1], b[1]) == 0) {
            return Boolean.TRUE;
        }

        return Boolean.FALSE;
    }

    /**
     * The predicate "x CONTAINS y" applies when<br>
     * a) both x and y are either period names or period constructors. In this case, the predicate returns True if
     * x contains every time point in y, i.e., if xs <= ys and xe >= ye.<br>
     * b) x is either a period name or a period constructor and y is a datetime value expression. In this case, the
     * predicate returns True if x contains y, i.e., if xs <= y and xe > y.
     * <p>
     * The <i>b</i> part of this definition is not supported yet. In order to get the same result, one have to specify
     * a period with the same date time value for the period start and end.
     * <p>
     *
     * @param session
     * @param a First period to compare
     * @param ta Types of the first period
     * @param b Second period to compare
     * @param tb Type of the second period
     *
     * @return {@link Boolean#TRUE} if period a contains period b,
     *          else {@link Boolean#FALSE}
     */
    public static Boolean contains(Session session, Object[] a, Type[] ta,
                                   Object[] b, Type[] tb,
                                   boolean pointOfTime) {

        Type commonType = normalizeInput(session, a, ta, b, tb, pointOfTime);

        if (commonType == null) {
            return null;
        }

        int compareStart = commonType.compare(session, a[0], b[0]);
        int compareEnd   = commonType.compare(session, a[1], b[1]);

        if (compareStart <= 0 && compareEnd >= 0) {

            // if the end of the two period are equals, period a does not
            // contains period b if it is defined by a single point in time
            if (pointOfTime) {
                if (compareEnd == 0) {
                    return Boolean.FALSE;
                }
            }

            return Boolean.TRUE;
        }

        return Boolean.FALSE;
    }

    public static BigDecimal subtractMonthsSpecial(Session session,
            TimestampData a, TimestampData b) {

        long    s1    = (a.getSeconds() + a.getZone()) * 1000;
        long    s2    = (b.getSeconds() + b.getZone()) * 1000;
        boolean minus = false;

        if (s1 < s2) {
            minus = true;

            long temp = s1;

            s1 = s2;
            s2 = temp;
        }

        s1 = HsqlDateTime.getNormalisedDate(session.getCalendarGMT(), s1);
        s2 = HsqlDateTime.getNormalisedDate(session.getCalendarGMT(), s2);

        Calendar cal = session.getCalendarGMT();

        cal.setTimeInMillis(s1);

        int lastDay1;
        int months1 = cal.get(Calendar.MONTH) + cal.get(Calendar.YEAR) * 12;
        int day1    = cal.get(Calendar.DAY_OF_MONTH);

        cal.set(Calendar.DAY_OF_MONTH, 1);

        long millis = cal.getTimeInMillis();

        cal.add(Calendar.MONTH, 1);

        millis = cal.getTimeInMillis();

        cal.add(Calendar.DAY_OF_MONTH, -1);

        millis   = cal.getTimeInMillis();
        lastDay1 = cal.get(Calendar.DAY_OF_MONTH);

        cal.setTimeInMillis(s2);

        int lastDay2;
        int months2 = cal.get(Calendar.MONTH) + cal.get(Calendar.YEAR) * 12;
        int day2    = cal.get(Calendar.DAY_OF_MONTH);

        cal.set(Calendar.DAY_OF_MONTH, 1);

        millis = cal.getTimeInMillis();

        cal.add(Calendar.MONTH, 1);

        millis = cal.getTimeInMillis();

        cal.add(Calendar.DAY_OF_MONTH, -1);

        millis   = cal.getTimeInMillis();
        lastDay2 = cal.get(Calendar.DAY_OF_MONTH);

        double months;
        double days;

        if (day1 == day2 || (day1 == lastDay1 && day2 == lastDay2)) {
            months = months1 - months2;

            if (minus) {
                months = -months;
            }

            return BigDecimal.valueOf(months);
        } else if (day2 > day1) {
            months = months1 - months2 - 1;
            days   = lastDay2 - day2 + day1;
            months += days / 31;

            if (minus) {
                months = -months;
            }

            return BigDecimal.valueOf(months);
        } else {
            months = months1 - months2;
            days   = day1 - day2;
            months += days / 31;

            if (minus) {
                months = -months;
            }

            return BigDecimal.valueOf(months);
        }
    }

    public static int subtractMonths(Session session, TimestampData a,
                                     TimestampData b, boolean isYear) {

        Calendar calendar = session.getCalendarGMT();
        boolean  negate   = false;

        if (b.getSeconds() > a.getSeconds()) {
            negate = true;

            TimestampData temp = a;

            a = b;
            b = temp;
        }

        calendar.setTimeInMillis(a.getSeconds() * 1000);

        int months = calendar.get(Calendar.MONTH);
        int years  = calendar.get(Calendar.YEAR);

        calendar.setTimeInMillis(b.getSeconds() * 1000);

        months -= calendar.get(Calendar.MONTH);
        years  -= calendar.get(Calendar.YEAR);

        if (isYear) {
            months = years * 12;
        } else {
            if (months < 0) {
                months += 12;

                years--;
            }

            months += years * 12;
        }

        if (negate) {
            months = -months;
        }

        return months;
    }

    public static TimeData addSeconds(TimeData source, long seconds,
                                      int nanos) {

        nanos   += source.getNanos();
        seconds += nanos / limitNanoseconds;
        nanos   %= limitNanoseconds;

        if (nanos < 0) {
            nanos += DTIType.limitNanoseconds;

            seconds--;
        }

        seconds += source.getSeconds();
        seconds %= (24 * 60 * 60);

        TimeData ti = new TimeData((int) seconds, nanos, source.getZone());

        return ti;
    }

    /** @todo - overflow */
    public static TimestampData addMonths(Session session,
                                          TimestampData source, int months) {

        int      n   = source.getNanos();
        Calendar cal = session.getCalendarGMT();

        HsqlDateTime.setTimeInMillis(cal, source.getSeconds() * 1000);
        cal.add(Calendar.MONTH, months);

        TimestampData ts = new TimestampData(cal.getTimeInMillis() / 1000, n,
                                             source.getZone());

        return ts;
    }

    public static TimestampData addSeconds(TimestampData source, long seconds,
                                           int nanos) {

        nanos   += source.getNanos();
        seconds += nanos / limitNanoseconds;
        nanos   %= limitNanoseconds;

        if (nanos < 0) {
            nanos += limitNanoseconds;

            seconds--;
        }

        long newSeconds = source.getSeconds() + seconds;
        TimestampData ts = new TimestampData(newSeconds, nanos,
                                             source.getZone());

        return ts;
    }

    public static TimestampData convertToDatetimeSpecial(
            SessionInterface session, String s, DateTimeType type) {

        switch (type.typeCode) {

            case Types.SQL_TIMESTAMP :
                if (session instanceof Session
                        && ((Session) session).database.sqlSyntaxOra) {
                    String pattern;

                    switch (s.length()) {

                        case 8 :
                        case 9 : {
                            pattern = "DD-MON-YY";

                            break;
                        }
                        case 10 :
                        case 11 : {
                            pattern = "DD-MON-YYYY";

                            break;
                        }
                        case 19 :
                        case 20 : {
                            pattern = "DD-MON-YYYY HH24:MI:SS";

                            break;
                        }
                        default :

                        // if (s.length() > 20)
                        {
                            pattern = "DD-MON-YYYY HH24:MI:SS.FF";

                            break;
                        }
                    }

                    SimpleDateFormat format = session.getSimpleDateFormatGMT();

                    return HsqlDateTime.toDate(s, pattern, format, true);
                }

            // fall through
            case Types.SQL_TIMESTAMP_WITH_TIME_ZONE :
            case Types.SQL_DATE :
            case Types.SQL_TIME :
            case Types.SQL_TIME_WITH_TIME_ZONE :
            default :
        }

        throw Error.error(ErrorCode.X_22007);
    }

    public static TimestampData nextDayOfWeek(Session session,
            TimestampData d, int day) {

        Calendar cal = session.getCalendarGMT();

        cal.setTimeInMillis(d.getMillis());

        int start = cal.get(Calendar.DAY_OF_WEEK);

        if (start >= day) {
            day += 7;
        }

        int diff = day - start;

        cal.add(Calendar.DAY_OF_MONTH, diff);

        long millis = cal.getTimeInMillis();

        millis = HsqlDateTime.getNormalisedDate(cal, millis);

        return new TimestampData(millis / 1000);
    }

    public static int getDayOfWeek(String name) {

        if (name.length() > 0) {
            int c = Character.toUpperCase(name.charAt(0));

            switch (c) {

                case 'M' :
                    return 2;

                case 'T' :
                    if (name.length() < 2) {
                        break;
                    }

                    if (Character.toUpperCase(name.charAt(1)) == 'U') {
                        return 3;
                    } else if (Character.toUpperCase(name.charAt(1)) == 'H') {
                        return 5;
                    }
                    break;

                case 'W' :
                    return 4;

                case 'F' :
                    return 6;

                case 'S' :
                    if (name.length() < 2) {
                        break;
                    }

                    if (Character.toUpperCase(name.charAt(1)) == 'A') {
                        return 7;
                    } else if (Character.toUpperCase(name.charAt(1)) == 'U') {
                        return 1;
                    }
                    break;
            }
        }

        throw Error.error(ErrorCode.X_22007, name);
    }
}
