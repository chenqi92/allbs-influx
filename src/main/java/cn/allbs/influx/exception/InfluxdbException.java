package cn.allbs.influx.exception;

/**
 * 类 InfluxdbException
 *
 * @author ChenQi
 */
public class InfluxdbException extends RuntimeException {

    public InfluxdbException(Throwable e) {
        super(e.getLocalizedMessage());
    }

    public InfluxdbException(String message) {
        super(message);
    }

    public InfluxdbException(String message, Throwable throwable) {
        super(message, throwable);
    }
}
