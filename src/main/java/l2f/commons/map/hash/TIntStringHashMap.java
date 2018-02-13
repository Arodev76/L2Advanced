package l2f.commons.map.hash;

import gnu.trove.map.hash.TIntObjectHashMap;

/**
 * @author Arodev
 * @date 02:49/12.02.2018
 */
public final class TIntStringHashMap extends TIntObjectHashMap<String>
{
    public String getNotNull(final int key) {
        final String value = this.get(key);
        return (value == null) ? "" : value;
    }
}
