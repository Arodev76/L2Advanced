package l2f.commons.logging;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author Arodev
 * @date  01:42/12.02.2018
 */
public abstract class LoggerObject
{
    protected final Logger _log;
    
    public LoggerObject() {
        this._log = LoggerFactory.getLogger(this.getClass());
    }
    
    public void error(final String st, final Exception e) {
        this._log.error(this.getClass().getSimpleName() + ": " + st, e);
    }
    
    public void error(final String st) {
        this._log.error(this.getClass().getSimpleName() + ": " + st);
    }
    
    public void warn(final String st, final Exception e) {
        this._log.warn(this.getClass().getSimpleName() + ": " + st, e);
    }
    
    public void warn(final String st) {
        this._log.warn(this.getClass().getSimpleName() + ": " + st);
    }
    
    public void info(final String st, final Exception e) {
        this._log.info(this.getClass().getSimpleName() + ": " + st, e);
    }
    
    public void info(final String st) {
        this._log.info(this.getClass().getSimpleName() + ": " + st);
    }
}
