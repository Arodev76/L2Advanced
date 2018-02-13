package l2f.commons.net.nio;

public abstract class SendablePacket<T> extends AbstractPacket<T>
{
    protected void writeC(final int data) {
        this.getByteBuffer().put((byte)data);
    }
    
    protected void writeF(final double value) {
        this.getByteBuffer().putDouble(value);
    }
    
    protected void writeH(final int value) {
        this.getByteBuffer().putShort((short)value);
    }
    
    protected void writeD(final int value) {
        this.getByteBuffer().putInt(value);
    }
    
    protected void writeQ(final long value) {
        this.getByteBuffer().putLong(value);
    }
    
    protected void writeB(final byte[] data) {
        this.getByteBuffer().put(data);
    }
    
    protected void writeS(final CharSequence charSequence) {
        if (charSequence != null) {
            for (int length = charSequence.length(), i = 0; i < length; ++i) {
                this.getByteBuffer().putChar(charSequence.charAt(i));
            }
        }
        this.getByteBuffer().putChar('\0');
    }
    
    protected abstract boolean write();
}
